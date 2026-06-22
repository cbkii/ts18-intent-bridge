package dev.cbkii.ts18intentbridge;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

import java.util.Arrays;

final class IntentRewriter {
    private IntentRewriter() {}

    static Intent rewriteIntent(Intent original, Context context, String callerPackage, String hookPoint) {
        if (original == null) return null;

        Intent candidate = null;
        for (BridgeRule rule : BridgeRules.rules()) {
            if (!rule.enabled) continue;
            if (rule.kind == BridgeRule.Kind.PACKAGE_COMPONENT) {
                candidate = rewritePackageIntent(original, context, callerPackage, hookPoint, rule);
            } else if (rule.kind == BridgeRule.Kind.SAF_PICKER) {
                candidate = rewriteSafIntent(original, context, callerPackage, hookPoint, rule);
            }
            if (candidate != null) return candidate;
        }
        return null;
    }

    static Intent[] rewriteIntentArray(Intent[] originals, Context context, String callerPackage, String hookPoint) {
        if (originals == null || originals.length == 0) return null;
        Intent[] copy = null;
        for (int i = 0; i < originals.length; i++) {
            Intent rewritten = rewriteIntent(originals[i], context, callerPackage, hookPoint + "[" + i + "]");
            if (rewritten != null) {
                if (copy == null) copy = Arrays.copyOf(originals, originals.length);
                copy[i] = rewritten;
            }
        }
        return copy;
    }

    private static Intent rewritePackageIntent(
            Intent original,
            Context context,
            String callerPackage,
            String hookPoint,
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
                // Preserve caller extras/data/type/categories/flags where possible, but use the real launch component.
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
                rewritten = merged;
                destinationApplied = true;
            }
        }

        if (!destinationApplied) {
            rewritten.setComponent(null);
            rewritten.setPackage(rule.destinationPackage);
        }

        if (rule.requireResolve && context != null && !canResolve(context, rewritten)) {
            BridgeLog.w("Not rewriting " + describe(original) + " via " + rule.id + ": destination does not resolve from " + hookPoint);
            return null;
        }

        BridgeLog.i("Rewrite " + rule.id + " for caller=" + callerPackage + " hook=" + hookPoint
                + " from=" + describe(original) + " to=" + describe(rewritten));
        return rewritten;
    }

    private static Intent rewriteSafIntent(
            Intent original,
            Context context,
            String callerPackage,
            String hookPoint,
            BridgeRule rule
    ) {
        String action = original.getAction();
        if (!rule.matchesSafAction(action)) return null;

        ComponentName component = original.getComponent();
        String componentPackage = component != null ? component.getPackageName() : null;
        boolean explicitDocumentsUi = isDocumentsUiPackage(componentPackage);
        boolean implicitPicker = component == null;
        boolean restrictedToDocumentsUi = isDocumentsUiPackage(original.getPackage());

        if (!explicitDocumentsUi && !implicitPicker && !restrictedToDocumentsUi) return null;

        Intent rewritten = new Intent(original);
        rewritten.setComponent(null);
        rewritten.setPackage(rule.destinationPackage);

        if (context != null && !canResolve(context, rewritten)) {
            BridgeLog.v("SAF rule " + rule.id + " did not resolve for " + describe(original));
            return null;
        }

        BridgeLog.i("Rewrite SAF picker for caller=" + callerPackage + " hook=" + hookPoint
                + " from=" + describe(original) + " to=" + describe(rewritten));
        return rewritten;
    }

    private static boolean canResolve(Context context, Intent intent) {
        try {
            PackageManager pm = context.getPackageManager();
            ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return resolveInfo != null && resolveInfo.activityInfo != null;
        } catch (Throwable t) {
            BridgeLog.e("resolveActivity failed for " + describe(intent), t);
            return false;
        }
    }

    private static boolean isDocumentsUiPackage(String packageName) {
        if (packageName == null) return false;
        return "com.android.documentsui".equals(packageName)
                || "com.google.android.documentsui".equals(packageName)
                || "com.android.providers.media.module".equals(packageName);
    }

    static String describe(Intent intent) {
        if (intent == null) return "<null>";
        ComponentName c = intent.getComponent();
        return "Intent{act=" + intent.getAction()
                + " pkg=" + intent.getPackage()
                + " cmp=" + (c == null ? "null" : c.flattenToShortString())
                + " typ=" + intent.getType()
                + " dat=" + intent.getDataString()
                + " flags=0x" + Integer.toHexString(intent.getFlags())
                + "}";
    }
}
