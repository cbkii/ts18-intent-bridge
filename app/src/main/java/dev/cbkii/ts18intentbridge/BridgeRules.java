package dev.cbkii.ts18intentbridge;

final class BridgeRules {
    private static final long RELOAD_INTERVAL_MS = 5000L;
    private static volatile BridgeRule[] cachedRules;
    private static volatile long lastLoadElapsed;

    private BridgeRules() {}

    static BridgeRule[] rules() {
        long now = android.os.SystemClock.elapsedRealtime();
        BridgeRule[] local = cachedRules;
        if (local == null || now - lastLoadElapsed > RELOAD_INTERVAL_MS) {
            synchronized (BridgeRules.class) {
                local = cachedRules;
                if (local == null || now - lastLoadElapsed > RELOAD_INTERVAL_MS) {
                    local = BridgeConfig.loadRules();
                    cachedRules = local;
                    lastLoadElapsed = now;
                }
            }
        }
        return local;
    }

    static void invalidateForTestOrUi() {
        synchronized (BridgeRules.class) {
            cachedRules = null;
            lastLoadElapsed = 0L;
        }
    }

    static BridgeRule firstIdentityRuleForSource(String packageName) {
        if (packageName == null) return null;
        for (BridgeRule rule : rules()) {
            if (rule.enabled && rule.spoofPackageManagerIdentity && rule.matchesPackage(packageName)) return rule;
        }
        return null;
    }

    static BridgeRule firstIdentityRuleForDestination(String packageName) {
        if (packageName == null) return null;
        for (BridgeRule rule : rules()) {
            if (rule.enabled && rule.spoofPackageManagerIdentity && packageName.equals(rule.destinationPackage)) return rule;
        }
        return null;
    }

    static boolean isKnownSourcePackage(String packageName) {
        return firstIdentityRuleForSource(packageName) != null;
    }

    static boolean isKnownDestinationPackage(String packageName) {
        return firstIdentityRuleForDestination(packageName) != null;
    }

    static BridgeRule identityRuleForPair(String a, String b) {
        if (a == null || b == null) return null;
        for (BridgeRule rule : rules()) {
            if (!rule.enabled || !rule.spoofPackageManagerIdentity) continue;
            boolean forward = rule.sourcePackage != null && rule.destinationPackage != null
                    && rule.sourcePackage.equals(a) && rule.destinationPackage.equals(b);
            boolean reverse = rule.sourcePackage != null && rule.destinationPackage != null
                    && rule.sourcePackage.equals(b) && rule.destinationPackage.equals(a);
            if (forward || reverse) return rule;
        }
        return null;
    }
}
