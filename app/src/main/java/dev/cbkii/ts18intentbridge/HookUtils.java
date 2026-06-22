package dev.cbkii.ts18intentbridge;

import android.content.Context;
import android.content.Intent;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class HookUtils {
    private HookUtils() {}

    static Class<?> findClassOrNull(String className, ClassLoader loader) {
        try {
            return XposedHelpers.findClass(className, loader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static void hookIntentMethods(String className, ClassLoader loader, final String callerPackage, String... methodNames) {
        Class<?> clazz = findClassOrNull(className, loader);
        if (clazz == null) {
            BridgeLog.v("Class not found for intent hook: " + className);
            return;
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (!contains(methodNames, method.getName())) continue;
            if (!hasIntentParam(method) && !hasIntentArrayParam(method)) continue;
            try {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        rewriteIntentArgs(param, callerPackage, className + "." + method.getName());
                    }
                });
                BridgeLog.v("Hooked " + className + "." + method.getName());
            } catch (Throwable t) {
                BridgeLog.e("Failed to hook " + className + "." + method.getName(), t);
            }
        }
    }

    static void rewriteIntentArgs(XC_MethodHook.MethodHookParam param, String callerPackage, String hookPoint) {
        Context context = findContext(param.args);
        for (int i = 0; i < param.args.length; i++) {
            Object arg = param.args[i];
            if (arg instanceof Intent) {
                Intent rewritten = IntentRewriter.rewriteIntent((Intent) arg, context, callerPackage, hookPoint);
                if (rewritten != null) param.args[i] = rewritten;
            } else if (arg instanceof Intent[]) {
                Intent[] rewritten = IntentRewriter.rewriteIntentArray((Intent[]) arg, context, callerPackage, hookPoint);
                if (rewritten != null) param.args[i] = rewritten;
            }
        }
    }

    static Context findContext(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof Context) return (Context) arg;
        }
        return null;
    }

    private static boolean hasIntentParam(Method method) {
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (Intent.class.isAssignableFrom(parameterType)) return true;
        }
        return false;
    }

    private static boolean hasIntentArrayParam(Method method) {
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (parameterType.isArray() && Intent.class.isAssignableFrom(parameterType.getComponentType())) return true;
        }
        return false;
    }

    private static boolean contains(String[] values, String target) {
        if (values == null) return false;
        for (String value : values) if (value.equals(target)) return true;
        return false;
    }
}
