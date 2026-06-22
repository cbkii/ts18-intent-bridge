package dev.cbkii.ts18intentbridge;

import android.content.Intent;

final class BridgeRule {
    enum Kind { PACKAGE_COMPONENT, SAF_PICKER }

    final String id;
    final Kind kind;
    final boolean enabled;
    final String sourcePackage;
    final String sourceClass;
    final String destinationPackage;
    final String destinationClass;
    final boolean useDestinationLaunchIntent;
    final boolean requireResolve;
    final boolean spoofPackageManagerIdentity;
    final String note;

    private BridgeRule(
            String id,
            Kind kind,
            boolean enabled,
            String sourcePackage,
            String sourceClass,
            String destinationPackage,
            String destinationClass,
            boolean useDestinationLaunchIntent,
            boolean requireResolve,
            boolean spoofPackageManagerIdentity,
            String note
    ) {
        this.id = id;
        this.kind = kind;
        this.enabled = enabled;
        this.sourcePackage = normalize(sourcePackage);
        this.sourceClass = normalize(sourceClass);
        this.destinationPackage = normalize(destinationPackage);
        this.destinationClass = normalize(destinationClass);
        this.useDestinationLaunchIntent = useDestinationLaunchIntent;
        this.requireResolve = requireResolve;
        this.spoofPackageManagerIdentity = spoofPackageManagerIdentity;
        this.note = note == null ? "" : note;
    }

    static BridgeRule packageRule(
            String id,
            boolean enabled,
            String sourcePackage,
            String sourceClass,
            String destinationPackage,
            String destinationClass,
            boolean useDestinationLaunchIntent,
            boolean spoofPackageManagerIdentity,
            String note
    ) {
        return new BridgeRule(
                id,
                Kind.PACKAGE_COMPONENT,
                enabled,
                sourcePackage,
                sourceClass,
                destinationPackage,
                destinationClass,
                useDestinationLaunchIntent,
                true,
                spoofPackageManagerIdentity,
                note
        );
    }

    static BridgeRule safRule(
            String id,
            boolean enabled,
            String destinationPackage,
            String note
    ) {
        return new BridgeRule(
                id,
                Kind.SAF_PICKER,
                enabled,
                null,
                null,
                destinationPackage,
                null,
                false,
                true,
                false,
                note
        );
    }

    boolean matchesPackage(String packageName) {
        return enabled
                && kind == Kind.PACKAGE_COMPONENT
                && sourcePackage != null
                && sourcePackage.equals(packageName);
    }

    boolean matchesSafAction(String action) {
        if (!enabled || kind != Kind.SAF_PICKER || action == null) return false;
        return Intent.ACTION_OPEN_DOCUMENT.equals(action)
                || Intent.ACTION_OPEN_DOCUMENT_TREE.equals(action)
                || Intent.ACTION_GET_CONTENT.equals(action)
                || Intent.ACTION_CREATE_DOCUMENT.equals(action)
                || Intent.ACTION_PICK.equals(action);
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
