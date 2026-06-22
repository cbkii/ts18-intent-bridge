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
            hookCallerSideIntentSends(lpparam, caller);
            if (BridgeConfig.isPmCompatEnabled()) PackageIdentitySpoofer.hook(lpparam.classLoader, caller);
            if ("android".equals(caller)) hookSystemServerIntentSends(lpparam, caller);
            BridgeLog.i("Loaded in " + caller + " process=" + lpparam.processName);
        } catch (Throwable t) {
            BridgeLog.e("Failed to initialise hooks in " + caller, t);
        }
    }

    private void hookCallerSideIntentSends(XC_LoadPackage.LoadPackageParam lpparam, String caller) {
        ClassLoader cl = lpparam.classLoader;
        HookUtils.hookIntentMethods("android.app.Instrumentation", cl, caller, "execStartActivity");
        HookUtils.hookIntentMethods("android.app.Activity", cl, caller,
                "startActivity", "startActivityForResult", "startActivityIfNeeded", "startNextMatchingActivity", "startActivities");
        HookUtils.hookIntentMethods("android.app.ContextImpl", cl, caller,
                "startActivity", "startActivityAsUser", "startActivities", "startService", "startForegroundService",
                "bindService", "bindIsolatedService", "sendBroadcast", "sendBroadcastAsUser",
                "sendOrderedBroadcast", "sendStickyBroadcast");
        HookUtils.hookIntentMethods("android.content.ContextWrapper", cl, caller,
                "startActivity", "startActivities", "startService", "startForegroundService", "bindService",
                "sendBroadcast", "sendOrderedBroadcast", "sendStickyBroadcast");
    }

    private void hookSystemServerIntentSends(XC_LoadPackage.LoadPackageParam lpparam, String caller) {
        ClassLoader cl = lpparam.classLoader;
        // Optional high-catch hooks for Android 10 system_server. Enable android/framework scope only after app-side hooks work.
        HookUtils.hookIntentMethods("com.android.server.wm.ActivityTaskManagerService", cl, caller,
                "startActivity", "startActivityAsUser", "startActivityIntentSender");
        HookUtils.hookIntentMethods("com.android.server.am.ActivityManagerService", cl, caller,
                "startActivity", "startActivityAsUser", "broadcastIntent", "startService", "bindService");
    }
}
