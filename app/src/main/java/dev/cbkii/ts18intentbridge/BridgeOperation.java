package dev.cbkii.ts18intentbridge;

enum BridgeOperation {
    ACTIVITY_START,
    ACTIVITY_RESULT,
    SERVICE_START,
    SERVICE_BIND,
    BROADCAST_SEND,
    PENDING_INTENT_ACTIVITY,
    PENDING_INTENT_SERVICE,
    PENDING_INTENT_BROADCAST,
    PACKAGE_QUERY,
    SAF_PICKER;

    boolean allowsPackageActivityRewrite() {
        return this == ACTIVITY_START || this == ACTIVITY_RESULT || this == PACKAGE_QUERY;
    }

    boolean allowsSafRewrite() {
        return this == ACTIVITY_START || this == ACTIVITY_RESULT || this == SAF_PICKER || this == PACKAGE_QUERY;
    }
}
