package dev.cbkii.ts18intentbridge;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * Caller-side illusion layer. This does not change Android's real PackageManager database.
 * It only helps hardcoded TS18 launcher/app checks inside scoped processes.
 */
final class PackageIdentitySpoofer {
    private PackageIdentitySpoofer() {}

    static void hook(ClassLoader loader, final String callerPackage) {
        Class<?> apm = HookUtils.findClassOrNull("android.app.ApplicationPackageManager", loader);
        if (apm == null) {
            BridgeLog.v("ApplicationPackageManager unavailable in " + callerPackage);
            return;
        }
        hookPackageStringFirstArg(apm, callerPackage, "getPackageInfo");
        hookPackageStringFirstArg(apm, callerPackage, "getApplicationInfo");
        hookPackageStringFirstArg(apm, callerPackage, "getPackageUid");
        hookPackageStringFirstArg(apm, callerPackage, "getInstallerPackageName");
        hookPackageStringFirstArg(apm, callerPackage, "getLaunchIntentForPackage");
        hookComponentFirstArg(apm, callerPackage, "getActivityInfo");
        hookComponentFirstArg(apm, callerPackage, "getServiceInfo");
        hookComponentFirstArg(apm, callerPackage, "getProviderInfo");
        hookIntentQuery(apm, callerPackage, "resolveActivity");
        hookIntentQuery(apm, callerPackage, "queryIntentActivities");
        hookIntentQuery(apm, callerPackage, "queryIntentServices");
    }

    private static void hookPackageStringFirstArg(Class<?> clazz, final String callerPackage, final String methodName) {
        for (final Method method : clazz.getDeclaredMethods()) {
            if (!methodName.equals(method.getName())) continue;
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0 || parameterTypes[0] != String.class) continue;
            try {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    String sourcePackage;
                    BridgeRule rule;
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        sourcePackage = (String) param.args[0];
                        rule = BridgeRules.firstIdentityRuleForSource(sourcePackage);
                        if (rule == null) return;
                        param.args[0] = rule.destinationPackage;
                        BridgeLog.v("PM delegate " + methodName + "(" + sourcePackage + ") -> " + rule.destinationPackage + " for " + callerPackage);
                    }
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (rule == null) return;
                        Object result = param.getResult();
                        if (result instanceof PackageInfo) spoofPackageInfo((PackageInfo) result, rule);
                        else if (result instanceof ApplicationInfo) spoofApplicationInfo((ApplicationInfo) result, rule);
                        else if (result instanceof Intent) spoofLaunchIntent((Intent) result, rule);
                    }
                });
            } catch (Throwable t) {
                BridgeLog.e("Failed PM hook " + methodName + " in " + callerPackage, t);
            }
        }
    }

    private static void hookComponentFirstArg(Class<?> clazz, final String callerPackage, final String methodName) {
        for (final Method method : clazz.getDeclaredMethods()) {
            if (!methodName.equals(method.getName())) continue;
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0 || parameterTypes[0] != ComponentName.class) continue;
            try {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    BridgeRule rule;
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        ComponentName component = (ComponentName) param.args[0];
                        if (component == null) return;
                        rule = BridgeRules.firstIdentityRuleForSource(component.getPackageName());
                        if (rule == null) return;
                        String dstClass = rule.destinationClass != null ? rule.destinationClass : component.getClassName();
                        param.args[0] = new ComponentName(rule.destinationPackage, dstClass);
                    }
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (rule == null) return;
                        spoofInfoObject(param.getResult(), rule);
                    }
                });
            } catch (Throwable t) {
                BridgeLog.e("Failed component PM hook " + methodName + " in " + callerPackage, t);
            }
        }
    }

    private static void hookIntentQuery(Class<?> clazz, final String callerPackage, final String methodName) {
        for (final Method method : clazz.getDeclaredMethods()) {
            if (!methodName.equals(method.getName())) continue;
            Class<?>[] p = method.getParameterTypes();
            if (p.length == 0 || p[0] != Intent.class) continue;
            try {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        HookUtils.rewriteIntentArgs(param, callerPackage, "PackageManager." + methodName);
                    }
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        Object result = param.getResult();
                        if (result instanceof ResolveInfo) spoofResolveInfo((ResolveInfo) result);
                        else if (result instanceof List<?>) {
                            for (Object item : (List<?>) result) if (item instanceof ResolveInfo) spoofResolveInfo((ResolveInfo) item);
                        }
                    }
                });
            } catch (Throwable t) {
                BridgeLog.e("Failed intent PM query hook " + methodName + " in " + callerPackage, t);
            }
        }
    }

    private static void spoofPackageInfo(PackageInfo info, BridgeRule rule) {
        if (info == null) return;
        info.packageName = rule.sourcePackage;
        if (info.applicationInfo != null) spoofApplicationInfo(info.applicationInfo, rule);
        if (info.activities != null) for (ActivityInfo ai : info.activities) spoofInfoObject(ai, rule);
        if (info.services != null) for (ServiceInfo si : info.services) spoofInfoObject(si, rule);
        if (info.providers != null) for (ProviderInfo pi : info.providers) spoofInfoObject(pi, rule);
    }

    private static void spoofApplicationInfo(ApplicationInfo info, BridgeRule rule) {
        if (info == null) return;
        info.packageName = rule.sourcePackage;
        if (info.processName != null && info.processName.equals(rule.destinationPackage)) info.processName = rule.sourcePackage;
    }

    private static void spoofLaunchIntent(Intent intent, BridgeRule rule) {
        if (intent == null) return;
        ComponentName component = intent.getComponent();
        if (component != null && rule.destinationPackage.equals(component.getPackageName())) {
            String cls = rule.sourceClass != null ? rule.sourceClass : component.getClassName();
            intent.setComponent(new ComponentName(rule.sourcePackage, cls));
        } else if (rule.destinationPackage.equals(intent.getPackage())) {
            intent.setPackage(rule.sourcePackage);
        }
    }

    private static void spoofResolveInfo(ResolveInfo info) {
        if (info == null) return;
        spoofInfoObject(info.activityInfo, null);
        spoofInfoObject(info.serviceInfo, null);
        spoofInfoObject(info.providerInfo, null);
    }

    private static void spoofInfoObject(Object info, BridgeRule knownRule) {
        if (info == null) return;
        try {
            Field packageNameField = info.getClass().getField("packageName");
            Object value = packageNameField.get(info);
            if (!(value instanceof String)) return;
            BridgeRule rule = knownRule != null ? knownRule : BridgeRules.firstIdentityRuleForDestination((String) value);
            if (rule != null) packageNameField.set(info, rule.sourcePackage);
        } catch (Throwable ignored) {
            // Not every info object exposes packageName as public on every build.
        }
        try {
            Field appField = info.getClass().getField("applicationInfo");
            Object app = appField.get(info);
            if (app instanceof ApplicationInfo) {
                BridgeRule rule = knownRule != null ? knownRule : BridgeRules.firstIdentityRuleForDestination(((ApplicationInfo) app).packageName);
                if (rule != null) spoofApplicationInfo((ApplicationInfo) app, rule);
            }
        } catch (Throwable ignored) {
            // Best effort only.
        }
    }
}
