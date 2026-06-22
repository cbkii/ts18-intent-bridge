package dev.cbkii.ts18intentbridge;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || lpparam.classLoader == null || lpparam.packageName == null) return;

        String caller = lpparam.packageName;
        if (!BridgeConfig.isCallerAllowed(caller)) {
            BridgeLog.v("Skipping unallowed process " + caller + " process=" + lpparam.processName);
            return;
        }
        try {
            int hookCount = hookCallerSideActivityIntents(lpparam, caller);
            if (BridgeConfig.isPmCompatEnabled()) PackageIdentitySpoofer.hook(lpparam.classLoader, caller);
            if ("android".equals(caller)) hookCount += hookSystemServerActivityIntents(lpparam, caller);
            BridgeLog.i("Loaded in " + caller + " process=" + lpparam.processName + " activityHooks=" + hookCount);
        } catch (Throwable t) {
            BridgeLog.e("Failed to initialise hooks in " + caller, t);
        }
    }

    private int hookCallerSideActivityIntents(XC_LoadPackage.LoadPackageParam lpparam, String caller) {
        ClassLoader cl = lpparam.classLoader;
        int count = 0;
        count += HookUtils.hookIntentMethods("android.app.Instrumentation", cl, caller,
                BridgeOperation.ACTIVITY_RESULT, "execStartActivity");
        count += HookUtils.hookIntentMethods("android.app.Activity", cl, caller,
                BridgeOperation.ACTIVITY_START, "startActivity", "startActivityIfNeeded", "startNextMatchingActivity", "startActivities");
        count += HookUtils.hookIntentMethods("android.app.Activity", cl, caller,
                BridgeOperation.ACTIVITY_RESULT, "startActivityForResult");
        count += HookUtils.hookIntentMethods("android.app.ContextImpl", cl, caller,
                BridgeOperation.ACTIVITY_START, "startActivity", "startActivityAsUser", "startActivities");
        count += HookUtils.hookIntentMethods("android.content.ContextWrapper", cl, caller,
                BridgeOperation.ACTIVITY_START, "startActivity", "startActivities");
        return count;
    }

    private int hookSystemServerActivityIntents(XC_LoadPackage.LoadPackageParam lpparam, String caller) {
        ClassLoader cl = lpparam.classLoader;
        // Optional high-catch hooks for Android 10 system_server. Enable android/framework scope only after app-side hooks work.
        int count = 0;
        count += HookUtils.hookIntentMethods("com.android.server.wm.ActivityTaskManagerService", cl, caller,
                BridgeOperation.ACTIVITY_START, "startActivity", "startActivityAsUser", "startActivityIntentSender");
        count += HookUtils.hookIntentMethods("com.android.server.am.ActivityManagerService", cl, caller,
                BridgeOperation.ACTIVITY_START, "startActivity", "startActivityAsUser");
        return count;
    }
}
