package dev.cbkii.ts18intentbridge;

import android.content.Context;
import android.content.Intent;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class HookUtils {
    private static final Set<String> HOOKED_METHODS = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private HookUtils() {}

    static Class<?> findClassOrNull(String className, ClassLoader loader) {
        try {
            return XposedHelpers.findClass(className, loader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static int hookIntentMethods(String className, ClassLoader loader, final String callerPackage,
            final BridgeOperation operation, String... methodNames) {
        Class<?> clazz = findClassOrNull(className, loader);
        if (clazz == null) {
            BridgeLog.v("Class not found for intent hook: " + className);
            return 0;
        }
        int hooked = 0;
        for (final Method method : clazz.getDeclaredMethods()) {
            if (!contains(methodNames, method.getName())) continue;
            if (!hasIntentParam(method) && !hasIntentArrayParam(method)) continue;
            String signature = className + "#" + method.toGenericString() + "#" + operation;
            if (!HOOKED_METHODS.add(signature)) continue;
            try {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        rewriteIntentArgs(param, callerPackage, className + "." + method.getName(), operation);
                    }
                });
                hooked++;
                BridgeLog.v("Hooked " + className + "." + method.getName() + " operation=" + operation);
            } catch (Throwable t) {
                HOOKED_METHODS.remove(signature);
                BridgeLog.e("Failed to hook " + className + "." + method.getName(), t);
            }
        }
        return hooked;
    }

    static void rewriteIntentArgs(XC_MethodHook.MethodHookParam param, String callerPackage, String hookPoint,
            BridgeOperation operation) {
        if (param == null || param.args == null) return;
Context context = param.thisObject instanceof Context ? (Context) param.thisObject : findContext(param.args);
        for (int i = 0; i < param.args.length; i++) {
            Object arg = param.args[i];
            if (arg instanceof Intent) {
                Intent rewritten = IntentRewriter.rewriteIntent((Intent) arg, context, callerPackage, hookPoint, operation);
                if (rewritten != null) param.args[i] = rewritten;
            } else if (arg instanceof Intent[]) {
                Intent[] rewritten = IntentRewriter.rewriteIntentArray((Intent[]) arg, context, callerPackage, hookPoint, operation);
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
