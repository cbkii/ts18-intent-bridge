package dev.cbkii.ts18intentbridge;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XSharedPreferences;

final class BridgeConfig {
    static final String PREFS_NAME = "bridge_config";

    static final String KEY_RADIO_ENABLED = "radio_enabled";
    static final String KEY_RADIO_SOURCE_PACKAGE = "radio_source_package";
    static final String KEY_RADIO_TARGET_PACKAGE = "radio_target_package";
    static final String KEY_RADIO_TARGET_CLASS = "radio_target_class";
    static final String KEY_RADIO_USE_LAUNCH_INTENT = "radio_use_launch_intent";
    static final String KEY_RADIO_SPOOF_PM = "radio_spoof_pm";

    static final String KEY_MUSIC_ENABLED = "music_enabled";
    static final String KEY_MUSIC_SOURCE_PACKAGE = "music_source_package";
    static final String KEY_MUSIC_SOURCE_CLASS = "music_source_class";
    static final String KEY_MUSIC_TARGET_PACKAGE = "music_target_package";
    static final String KEY_MUSIC_TARGET_CLASS = "music_target_class";
    static final String KEY_MUSIC_USE_LAUNCH_INTENT = "music_use_launch_intent";
    static final String KEY_MUSIC_SPOOF_PM = "music_spoof_pm";

    static final String KEY_SAF_ENABLED = "saf_enabled";
    static final String KEY_SAF_TARGET_PACKAGES = "saf_target_packages";
    static final String KEY_PM_COMPAT_ENABLED = "pm_compat_enabled";
    static final String KEY_VERBOSE_LOGGING = "verbose_logging";
    static final String KEY_CALLER_ALLOWLIST = "caller_allowlist";
    static final String KEY_CALLER_BLOCKLIST = "caller_blocklist";

    static final String DEFAULT_RADIO_SOURCE_PACKAGE = "com.tw.radio";
    static final String DEFAULT_RADIO_TARGET_PACKAGE = "com.navimods.radio";
    static final String DEFAULT_MUSIC_SOURCE_PACKAGE = "com.tw.music";
    static final String DEFAULT_MUSIC_SOURCE_CLASS = "com.tw.music.MusicActivity";
    static final String DEFAULT_MUSIC_TARGET_PACKAGE = "com.tw.media";
    static final String DEFAULT_MUSIC_TARGET_CLASS = "com.tw.music.MusicActivity";
    static final String DEFAULT_SAF_TARGET_PACKAGES = "com.mixplorer,com.mixplorer.silver";
    static final String DEFAULT_CALLER_ALLOWLIST = "com.dofun.variety";
    static final String DEFAULT_CALLER_BLOCKLIST = "";

    private BridgeConfig() {}

    static BridgeRule[] loadRules() {
        XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, PREFS_NAME);
        try {
            prefs.reload();
        } catch (Throwable t) {
            BridgeLog.w("Could not reload XSharedPreferences; using built-in defaults: " + t.getClass().getSimpleName());
        }

        List<BridgeRule> rules = new ArrayList<>();

        boolean radioEnabled = prefs.getBoolean(KEY_RADIO_ENABLED, true);
        String radioSource = normalizePackage(prefs.getString(KEY_RADIO_SOURCE_PACKAGE, DEFAULT_RADIO_SOURCE_PACKAGE));
        String radioTarget = normalizePackage(prefs.getString(KEY_RADIO_TARGET_PACKAGE, DEFAULT_RADIO_TARGET_PACKAGE));
        String radioTargetClass = normalizeClass(prefs.getString(KEY_RADIO_TARGET_CLASS, ""));
        boolean radioUseLaunchIntent = prefs.getBoolean(KEY_RADIO_USE_LAUNCH_INTENT, true);
        boolean pmCompatEnabled = prefs.getBoolean(KEY_PM_COMPAT_ENABLED, false);
        boolean radioSpoofPm = pmCompatEnabled && prefs.getBoolean(KEY_RADIO_SPOOF_PM, false);
        if (radioSource != null && radioTarget != null) {
            rules.add(BridgeRule.packageRule(
                    "radio-" + radioSource + "-to-" + radioTarget,
                    radioEnabled,
                    radioSource,
                    null,
                    radioTarget,
                    radioTargetClass,
                    radioUseLaunchIntent,
                    radioSpoofPm,
                    "Configurable radio replacement. Default is com.tw.radio to com.navimods.radio."
            ));
        }

        boolean musicEnabled = prefs.getBoolean(KEY_MUSIC_ENABLED, true);
        String musicSource = normalizePackage(prefs.getString(KEY_MUSIC_SOURCE_PACKAGE, DEFAULT_MUSIC_SOURCE_PACKAGE));
        String musicSourceClass = normalizeClass(prefs.getString(KEY_MUSIC_SOURCE_CLASS, DEFAULT_MUSIC_SOURCE_CLASS));
        String musicTarget = normalizePackage(prefs.getString(KEY_MUSIC_TARGET_PACKAGE, DEFAULT_MUSIC_TARGET_PACKAGE));
        String musicTargetClass = normalizeClass(prefs.getString(KEY_MUSIC_TARGET_CLASS, DEFAULT_MUSIC_TARGET_CLASS));
        boolean musicUseLaunchIntent = prefs.getBoolean(KEY_MUSIC_USE_LAUNCH_INTENT, false);
        boolean musicSpoofPm = pmCompatEnabled && prefs.getBoolean(KEY_MUSIC_SPOOF_PM, false);
        if (musicSource != null && musicTarget != null) {
            rules.add(BridgeRule.packageRule(
                    "music-" + musicSource + "-to-" + musicTarget,
                    musicEnabled,
                    musicSource,
                    musicSourceClass,
                    musicTarget,
                    musicTargetClass,
                    musicUseLaunchIntent,
                    musicSpoofPm,
                    "Configurable music replacement. Default is com.tw.music/com.tw.music.MusicActivity to com.tw.media/com.tw.music.MusicActivity."
            ));
        }

        boolean safEnabled = prefs.getBoolean(KEY_SAF_ENABLED, true);
        String safTargets = prefs.getString(KEY_SAF_TARGET_PACKAGES, DEFAULT_SAF_TARGET_PACKAGES);
        String[] packageNames = splitPackageList(safTargets);
        for (int i = 0; i < packageNames.length; i++) {
            rules.add(BridgeRule.safRule(
                    "saf-to-" + packageNames[i],
                    safEnabled,
                    packageNames[i],
                    "Configurable SAF/DocumentsUI replacement candidate. Earlier candidates win if they resolve."
            ));
        }

        return rules.toArray(new BridgeRule[0]);
    }

    static void ensureDefaults(SharedPreferences prefs) {
        if (prefs == null || prefs.contains(KEY_RADIO_SOURCE_PACKAGE)) return;
        prefs.edit()
                .putBoolean(KEY_RADIO_ENABLED, true)
                .putString(KEY_RADIO_SOURCE_PACKAGE, DEFAULT_RADIO_SOURCE_PACKAGE)
                .putString(KEY_RADIO_TARGET_PACKAGE, DEFAULT_RADIO_TARGET_PACKAGE)
                .putString(KEY_RADIO_TARGET_CLASS, "")
                .putBoolean(KEY_RADIO_USE_LAUNCH_INTENT, true)
                .putBoolean(KEY_RADIO_SPOOF_PM, false)
                .putBoolean(KEY_MUSIC_ENABLED, true)
                .putString(KEY_MUSIC_SOURCE_PACKAGE, DEFAULT_MUSIC_SOURCE_PACKAGE)
                .putString(KEY_MUSIC_SOURCE_CLASS, DEFAULT_MUSIC_SOURCE_CLASS)
                .putString(KEY_MUSIC_TARGET_PACKAGE, DEFAULT_MUSIC_TARGET_PACKAGE)
                .putString(KEY_MUSIC_TARGET_CLASS, DEFAULT_MUSIC_TARGET_CLASS)
                .putBoolean(KEY_MUSIC_USE_LAUNCH_INTENT, false)
                .putBoolean(KEY_MUSIC_SPOOF_PM, false)
                .putBoolean(KEY_SAF_ENABLED, true)
                .putString(KEY_SAF_TARGET_PACKAGES, DEFAULT_SAF_TARGET_PACKAGES)
                .putBoolean(KEY_PM_COMPAT_ENABLED, false)
                .putBoolean(KEY_VERBOSE_LOGGING, false)
                .putString(KEY_CALLER_ALLOWLIST, DEFAULT_CALLER_ALLOWLIST)
                .putString(KEY_CALLER_BLOCKLIST, DEFAULT_CALLER_BLOCKLIST)
                .apply();
    }

    static String normalizePackage(String value) {
        String s = normalize(value);
        if (s == null) return null;
        if (!s.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")) return null;
        return s;
    }

    static String normalizeClass(String value) {
        String s = normalize(value);
        if (s == null) return null;
        if (s.startsWith(".")) return s;
        if (!s.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")) return null;
        return s;
    }

    static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    static boolean isVerboseLoggingEnabled() {
        XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, PREFS_NAME);
        try { prefs.reload(); } catch (Throwable ignored) {}
        return prefs.getBoolean(KEY_VERBOSE_LOGGING, false);
    }

    static boolean isPmCompatEnabled() {
        XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, PREFS_NAME);
        try { prefs.reload(); } catch (Throwable ignored) {}
        return prefs.getBoolean(KEY_PM_COMPAT_ENABLED, false);
    }

    static boolean isCallerAllowed(String callerPackage) {
        if (callerPackage == null) return false;
        XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, PREFS_NAME);
        try { prefs.reload(); } catch (Throwable ignored) {}
        String[] block = splitPackageList(prefs.getString(KEY_CALLER_BLOCKLIST, DEFAULT_CALLER_BLOCKLIST));
        for (String pkg : block) if (callerPackage.equals(pkg)) return false;
        String[] allow = splitPackageList(prefs.getString(KEY_CALLER_ALLOWLIST, DEFAULT_CALLER_ALLOWLIST));
        if (allow.length == 0) return false;
        for (String pkg : allow) if (callerPackage.equals(pkg)) return true;
        return false;
    }

    static String[] splitPackageList(String value) {
        String normalized = normalize(value);
        if (normalized == null) return new String[0];
        String[] raw = normalized.split("[,\\s]+", -1);
        List<String> out = new ArrayList<>();
        for (String item : raw) {
            String pkg = normalizePackage(item);
            if (pkg != null && !out.contains(pkg)) out.add(pkg);
        }
        return out.toArray(new String[0]);
    }
}
