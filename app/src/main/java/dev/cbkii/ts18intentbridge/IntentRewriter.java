package dev.cbkii.ts18intentbridge;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.Arrays;

final class IntentRewriter {
    private IntentRewriter() {}

    static Intent rewriteIntent(Intent original, Context context, String callerPackage, String hookPoint,
            BridgeOperation operation) {
        return rewriteIntent(original, context, callerPackage, hookPoint, operation, BridgeRules.rules());
    }

    static Intent rewriteIntent(Intent original, Context context, String callerPackage, String hookPoint,
            BridgeOperation operation, BridgeRule[] rules) {
        if (original == null || operation == null || rules == null) return null;

        Intent chooser = rewriteChooserIfNeeded(original, context, callerPackage, hookPoint, operation, rules);
        if (chooser != null) return chooser;

        for (BridgeRule rule : rules) {
            if (rule == null || !rule.enabled) continue;
            Intent candidate = null;
            if (rule.kind == BridgeRule.Kind.PACKAGE_COMPONENT && operation.allowsPackageActivityRewrite()) {
                candidate = rewritePackageIntent(original, context, callerPackage, hookPoint, operation, rule);
            } else if (rule.kind == BridgeRule.Kind.SAF_PICKER && operation.allowsSafRewrite()) {
                candidate = rewriteSafIntent(original, context, callerPackage, hookPoint, operation, rule);
            }
            if (candidate != null) return candidate;
        }
        return null;
    }

    static Intent[] rewriteIntentArray(Intent[] originals, Context context, String callerPackage, String hookPoint,
            BridgeOperation operation) {
        if (originals == null || originals.length == 0) return null;
        Intent[] copy = null;
        for (int i = 0; i < originals.length; i++) {
            Intent rewritten = rewriteIntent(originals[i], context, callerPackage, hookPoint + "[" + i + "]", operation);
            if (rewritten != null) {
                if (copy == null) copy = Arrays.copyOf(originals, originals.length);
                copy[i] = rewritten;
            }
        }
        return copy;
    }

    private static Intent rewriteChooserIfNeeded(Intent original, Context context, String callerPackage, String hookPoint,
            BridgeOperation operation, BridgeRule[] rules) {
        if (!Intent.ACTION_CHOOSER.equals(original.getAction())) return null;
        boolean changed = false;
        Intent copy = new Intent(original);
        Parcelable extra = original.getParcelableExtra(Intent.EXTRA_INTENT);
        if (extra instanceof Intent) {
            Intent rewritten = rewriteIntent((Intent) extra, context, callerPackage, hookPoint + ":chooser", operation, rules);
            if (rewritten != null) {
                copy.putExtra(Intent.EXTRA_INTENT, rewritten);
                changed = true;
            }
        }
        Parcelable[] initial = original.getParcelableArrayExtra(Intent.EXTRA_INITIAL_INTENTS);
        if (initial != null && initial.length > 0) {
            Parcelable[] next = Arrays.copyOf(initial, initial.length);
            for (int i = 0; i < initial.length; i++) {
                if (initial[i] instanceof Intent) {
                    Intent rewritten = rewriteIntent((Intent) initial[i], context, callerPackage,
                            hookPoint + ":initial[" + i + "]", operation, rules);
                    if (rewritten != null) {
                        next[i] = rewritten;
                        changed = true;
                    }
                }
            }
            if (changed) copy.putExtra(Intent.EXTRA_INITIAL_INTENTS, next);
        }
        return changed ? copy : null;
    }

    private static Intent rewritePackageIntent(
            Intent original,
            Context context,
            String callerPackage,
            String hookPoint,
            BridgeOperation operation,
            BridgeRule rule
    ) {
        String packageName = original.getPackage();
        ComponentName component = original.getComponent();
        String componentPackage = component != null ? component.getPackageName() : null;
        String componentClass = component != null ? component.getClassName() : null;

        boolean packageMatch = rule.sourcePackage.equals(packageName);
        boolean componentPackageMatch = rule.sourcePackage.equals(componentPackage);
        boolean componentClassMatch = rule.sourceClass == null || rule.sourceClass.equals(componentClass);

        if (!packageMatch && !(componentPackageMatch && componentClassMatch)) return null;

        Intent rewritten = new Intent(original);
        boolean destinationApplied = false;

        if (rule.destinationClass != null) {
            rewritten.setComponent(new ComponentName(rule.destinationPackage, rule.destinationClass));
            rewritten.setPackage(null);
            destinationApplied = true;
        } else if (rule.useDestinationLaunchIntent && context != null) {
            Intent launch = context.getPackageManager().getLaunchIntentForPackage(rule.destinationPackage);
            if (launch != null) {
                rewritten = mergeLaunchIntent(original, launch);
                destinationApplied = true;
            }
        }

        if (!destinationApplied) {
            rewritten.setComponent(null);
            rewritten.setPackage(rule.destinationPackage);
        }

        if (rule.requireResolve && context != null && !canResolve(context, rewritten)) {
            BridgeLog.w("Not rewriting rule=" + rule.id + " op=" + operation + " hook=" + hookPoint
                    + ": destination does not resolve");
            return null;
        }

        BridgeLog.i("Rewrite rule=" + rule.id + " op=" + operation + " caller=" + callerPackage + " hook=" + hookPoint
                + " from=" + describe(original, BridgeConfig.isVerboseLoggingEnabled())
                + " to=" + describe(rewritten, BridgeConfig.isVerboseLoggingEnabled()));
        return rewritten;
    }

    private static Intent mergeLaunchIntent(Intent original, Intent launch) {
        Intent merged = new Intent(launch);
        Bundle extras = original.getExtras();
        if (extras != null) merged.putExtras(extras);
        Uri data = original.getData();
        String type = original.getType();
        if (data != null || type != null) merged.setDataAndType(data, type);
        if (original.getCategories() != null) {
            for (String category : original.getCategories()) merged.addCategory(category);
        }
        merged.addFlags(original.getFlags());
        ClipData clipData = original.getClipData();
        if (clipData != null) merged.setClipData(clipData);
        if (original.getSourceBounds() != null) merged.setSourceBounds(original.getSourceBounds());
        if (original.getSelector() != null) merged.setSelector(new Intent(original.getSelector()));
        return merged;
    }

    private static Intent rewriteSafIntent(
            Intent original,
            Context context,
            String callerPackage,
            String hookPoint,
            BridgeOperation operation,
            BridgeRule rule
    ) {
        if (!rule.matchesSafAction(original)) return null;

        ComponentName component = original.getComponent();
        String componentPackage = component != null ? component.getPackageName() : null;
        boolean explicitDocumentsUi = isDocumentsUiPackage(componentPackage);
        boolean implicitPicker = component == null && original.getPackage() == null;
        boolean restrictedToDocumentsUi = isDocumentsUiPackage(original.getPackage());

        if (!explicitDocumentsUi && !implicitPicker && !restrictedToDocumentsUi) return null;

        Intent rewritten = new Intent(original);
        rewritten.setComponent(null);
        rewritten.setPackage(rule.destinationPackage);

        if (context != null && !canResolve(context, rewritten)) {
            BridgeLog.v("SAF rule " + rule.id + " did not resolve for " + describe(original, false));
            return null;
        }

        BridgeLog.i("Rewrite SAF rule=" + rule.id + " op=" + operation + " caller=" + callerPackage + " hook=" + hookPoint
                + " from=" + describe(original, BridgeConfig.isVerboseLoggingEnabled())
                + " to=" + describe(rewritten, BridgeConfig.isVerboseLoggingEnabled()));
        return rewritten;
    }

    private static boolean canResolve(Context context, Intent intent) {
        try {
            PackageManager pm = context.getPackageManager();
            ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return resolveInfo != null && resolveInfo.activityInfo != null;
        } catch (Throwable t) {
            BridgeLog.e("resolveActivity failed for " + describe(intent, false), t);
            return false;
        }
    }

    private static boolean isDocumentsUiPackage(String packageName) {
        if (packageName == null) return false;
        return "com.android.documentsui".equals(packageName)
                || "com.google.android.documentsui".equals(packageName)
                || "com.android.providers.media.module".equals(packageName);
    }

    static String describe(Intent intent, boolean verbose) {
        if (intent == null) return "<null>";
        ComponentName c = intent.getComponent();
        return "Intent{act=" + intent.getAction()
                + " pkg=" + intent.getPackage()
                + " cmp=" + (c == null ? "null" : c.flattenToShortString())
                + " typ=" + intent.getType()
                + " dat=" + (verbose ? intent.getDataString() : redact(intent.getDataString()))
                + " flags=0x" + Integer.toHexString(intent.getFlags())
                + "}";
    }

    private static String redact(String value) {
        return value == null ? null : "<redacted>";
    }
}
