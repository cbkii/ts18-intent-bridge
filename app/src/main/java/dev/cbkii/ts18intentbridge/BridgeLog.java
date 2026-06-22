package dev.cbkii.ts18intentbridge;

import de.robv.android.xposed.XposedBridge;

final class BridgeLog {
    private static final String TAG = "TS18IntentBridge";
    private static final boolean VERBOSE = false;

    private BridgeLog() {}

    static void i(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    static void v(String message) {
        if (VERBOSE) XposedBridge.log(TAG + ": " + message);
    }

    static void w(String message) {
        XposedBridge.log(TAG + " WARN: " + message);
    }

    static void e(String message, Throwable throwable) {
        XposedBridge.log(TAG + " ERROR: " + message);
        if (throwable != null) XposedBridge.log(throwable);
    }
}
