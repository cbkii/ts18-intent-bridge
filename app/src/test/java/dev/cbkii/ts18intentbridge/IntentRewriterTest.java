package dev.cbkii.ts18intentbridge;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IntentRewriterTest {
    @Test public void operationPolicyIsActivityOnlyByDefaultForPackageRules() {
        assertTrue(BridgeOperation.ACTIVITY_START.allowsPackageActivityRewrite());
        assertTrue(BridgeOperation.ACTIVITY_RESULT.allowsPackageActivityRewrite());
        assertTrue(BridgeOperation.PACKAGE_QUERY.allowsPackageActivityRewrite());
        assertFalse(BridgeOperation.SERVICE_START.allowsPackageActivityRewrite());
        assertFalse(BridgeOperation.SERVICE_BIND.allowsPackageActivityRewrite());
        assertFalse(BridgeOperation.BROADCAST_SEND.allowsPackageActivityRewrite());
        assertFalse(BridgeOperation.PENDING_INTENT_ACTIVITY.allowsPackageActivityRewrite());
        assertFalse(BridgeOperation.PENDING_INTENT_SERVICE.allowsPackageActivityRewrite());
        assertFalse(BridgeOperation.PENDING_INTENT_BROADCAST.allowsPackageActivityRewrite());
    }

    @Test public void operationPolicyBoundsSafToActivityPickerAndPackageQueries() {
        assertTrue(BridgeOperation.ACTIVITY_START.allowsSafRewrite());
        assertTrue(BridgeOperation.ACTIVITY_RESULT.allowsSafRewrite());
        assertTrue(BridgeOperation.SAF_PICKER.allowsSafRewrite());
        assertTrue(BridgeOperation.PACKAGE_QUERY.allowsSafRewrite());
        assertFalse(BridgeOperation.SERVICE_START.allowsSafRewrite());
        assertFalse(BridgeOperation.SERVICE_BIND.allowsSafRewrite());
        assertFalse(BridgeOperation.BROADCAST_SEND.allowsSafRewrite());
    }

    @Test public void packageRuleMatchingRequiresEnabledSourcePackage() {
        BridgeRule rule = BridgeRule.packageRule(
                "radio", true, "com.tw.radio", null, "com.navimods.radio", null, false, false, "test");
        assertTrue(rule.matchesPackage("com.tw.radio"));
        assertFalse(rule.matchesPackage("com.tw.media"));

        BridgeRule disabled = BridgeRule.packageRule(
                "disabled", false, "com.tw.radio", null, "com.navimods.radio", null, false, false, "test");
        assertFalse(disabled.matchesPackage("com.tw.radio"));
    }

    @Test public void packageManagerCompatibilityDefaultsAreOffAndScoped() {
        assertFalse(BridgeConfig.DEFAULT_CALLER_ALLOWLIST.isEmpty());
        assertTrue("com.dofun.variety".equals(BridgeConfig.DEFAULT_CALLER_ALLOWLIST));
    }
}
