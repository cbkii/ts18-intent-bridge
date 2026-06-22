package dev.cbkii.ts18intentbridge;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public final class MainActivity extends Activity {
    private SharedPreferences prefs;
    private CheckBox pmCompatEnabled;
    private CheckBox verboseLogging;
    private EditText callerAllowlist;
    private EditText callerBlocklist;
    private CheckBox radioEnabled;
    private EditText radioSourcePackage;
    private EditText radioTargetPackage;
    private EditText radioTargetClass;
    private CheckBox radioUseLaunchIntent;
    private CheckBox radioSpoofPm;
    private CheckBox musicEnabled;
    private EditText musicSourcePackage;
    private EditText musicSourceClass;
    private EditText musicTargetPackage;
    private EditText musicTargetClass;
    private CheckBox musicUseLaunchIntent;
    private CheckBox musicSpoofPm;
    private CheckBox safEnabled;
    private EditText safTargetPackages;
    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(BridgeConfig.PREFS_NAME, MODE_PRIVATE);
        BridgeConfig.ensureDefaults(prefs);
        setContentView(buildView());
        loadFromPrefs();
        updateStatus("Loaded. Save changes, then force-stop/restart scoped apps or reboot.");
    }

    private View buildView() {
        int pad = 16;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = text("TS18 Intent Bridge", 22);
        root.addView(title);
        root.addView(text("Configurable LSPosed/Vector intent redirection. Defaults are TS18/DoFun-specific. Keep scope narrow and validate each rule separately.", 14));

        pmCompatEnabled = checkBox("Enable bounded PackageManager compatibility shims (off by default)");
        verboseLogging = checkBox("Enable verbose Xposed logging");
        callerAllowlist = edit("Caller allowlist, comma-separated", BridgeConfig.DEFAULT_CALLER_ALLOWLIST);
        callerBlocklist = edit("Caller blocklist, comma-separated", BridgeConfig.DEFAULT_CALLER_BLOCKLIST);
        addAll(root, pmCompatEnabled, verboseLogging, callerAllowlist, callerBlocklist);

        radioEnabled = checkBox("Enable radio: source package -> replacement package");
        root.addView(radioEnabled);
        radioSourcePackage = edit("Radio source package", BridgeConfig.DEFAULT_RADIO_SOURCE_PACKAGE);
        radioTargetPackage = edit("Radio replacement package", BridgeConfig.DEFAULT_RADIO_TARGET_PACKAGE);
        radioTargetClass = edit("Radio replacement class (blank = launch intent)", "");
        radioUseLaunchIntent = checkBox("Radio: use replacement package launch intent when class is blank");
        radioSpoofPm = checkBox("Radio: spoof selected PackageManager answers to source identity");
        addAll(root, radioSourcePackage, radioTargetPackage, radioTargetClass, radioUseLaunchIntent, radioSpoofPm);

        musicEnabled = checkBox("Enable music: source package/component -> replacement package/component");
        root.addView(musicEnabled);
        musicSourcePackage = edit("Music source package", BridgeConfig.DEFAULT_MUSIC_SOURCE_PACKAGE);
        musicSourceClass = edit("Music source class", BridgeConfig.DEFAULT_MUSIC_SOURCE_CLASS);
        musicTargetPackage = edit("Music replacement package", BridgeConfig.DEFAULT_MUSIC_TARGET_PACKAGE);
        musicTargetClass = edit("Music replacement class", BridgeConfig.DEFAULT_MUSIC_TARGET_CLASS);
        musicUseLaunchIntent = checkBox("Music: use replacement package launch intent when class is blank");
        musicSpoofPm = checkBox("Music: spoof selected PackageManager answers to source identity");
        addAll(root, musicSourcePackage, musicSourceClass, musicTargetPackage, musicTargetClass, musicUseLaunchIntent, musicSpoofPm);

        safEnabled = checkBox("Enable SAF/DocumentsUI picker redirect");
        root.addView(safEnabled);
        safTargetPackages = edit("SAF replacement package candidates, comma-separated", BridgeConfig.DEFAULT_SAF_TARGET_PACKAGES);
        root.addView(safTargetPackages);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { savePrefs(); } });
        Button reset = new Button(this);
        reset.setText("Reset defaults");
        reset.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { resetDefaults(); } });
        Button summary = new Button(this);
        summary.setText("Debug summary");
        summary.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { updateStatus(buildDiagnosticSummary()); } });
        buttons.addView(save);
        buttons.addView(reset);
        buttons.addView(summary);
        root.addView(buttons);

        status = text("", 13);
        root.addView(status);
        root.addView(text("STOP if you see boot loops, repeated launcher crashes, PackageManager signature errors, or target apps opening with broken state. Disable the module in Vector/LSPosed and reboot.", 13));

        wireEnablers();
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    private void wireEnablers() {
        radioEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setEnabledAll(isChecked, radioSourcePackage, radioTargetPackage, radioTargetClass, radioUseLaunchIntent, radioSpoofPm);
            }
        });
        musicEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setEnabledAll(isChecked, musicSourcePackage, musicSourceClass, musicTargetPackage, musicTargetClass, musicUseLaunchIntent, musicSpoofPm);
            }
        });
        safEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                safTargetPackages.setEnabled(isChecked);
            }
        });
    }

    private void loadFromPrefs() {
        pmCompatEnabled.setChecked(prefs.getBoolean(BridgeConfig.KEY_PM_COMPAT_ENABLED, false));
        verboseLogging.setChecked(prefs.getBoolean(BridgeConfig.KEY_VERBOSE_LOGGING, false));
        callerAllowlist.setText(prefs.getString(BridgeConfig.KEY_CALLER_ALLOWLIST, BridgeConfig.DEFAULT_CALLER_ALLOWLIST));
        callerBlocklist.setText(prefs.getString(BridgeConfig.KEY_CALLER_BLOCKLIST, BridgeConfig.DEFAULT_CALLER_BLOCKLIST));

        radioEnabled.setChecked(prefs.getBoolean(BridgeConfig.KEY_RADIO_ENABLED, true));
        radioSourcePackage.setText(prefs.getString(BridgeConfig.KEY_RADIO_SOURCE_PACKAGE, BridgeConfig.DEFAULT_RADIO_SOURCE_PACKAGE));
        radioTargetPackage.setText(prefs.getString(BridgeConfig.KEY_RADIO_TARGET_PACKAGE, BridgeConfig.DEFAULT_RADIO_TARGET_PACKAGE));
        radioTargetClass.setText(prefs.getString(BridgeConfig.KEY_RADIO_TARGET_CLASS, ""));
        radioUseLaunchIntent.setChecked(prefs.getBoolean(BridgeConfig.KEY_RADIO_USE_LAUNCH_INTENT, true));
        radioSpoofPm.setChecked(prefs.getBoolean(BridgeConfig.KEY_RADIO_SPOOF_PM, false));

        musicEnabled.setChecked(prefs.getBoolean(BridgeConfig.KEY_MUSIC_ENABLED, true));
        musicSourcePackage.setText(prefs.getString(BridgeConfig.KEY_MUSIC_SOURCE_PACKAGE, BridgeConfig.DEFAULT_MUSIC_SOURCE_PACKAGE));
        musicSourceClass.setText(prefs.getString(BridgeConfig.KEY_MUSIC_SOURCE_CLASS, BridgeConfig.DEFAULT_MUSIC_SOURCE_CLASS));
        musicTargetPackage.setText(prefs.getString(BridgeConfig.KEY_MUSIC_TARGET_PACKAGE, BridgeConfig.DEFAULT_MUSIC_TARGET_PACKAGE));
        musicTargetClass.setText(prefs.getString(BridgeConfig.KEY_MUSIC_TARGET_CLASS, BridgeConfig.DEFAULT_MUSIC_TARGET_CLASS));
        musicUseLaunchIntent.setChecked(prefs.getBoolean(BridgeConfig.KEY_MUSIC_USE_LAUNCH_INTENT, false));
        musicSpoofPm.setChecked(prefs.getBoolean(BridgeConfig.KEY_MUSIC_SPOOF_PM, false));

        safEnabled.setChecked(prefs.getBoolean(BridgeConfig.KEY_SAF_ENABLED, true));
        safTargetPackages.setText(prefs.getString(BridgeConfig.KEY_SAF_TARGET_PACKAGES, BridgeConfig.DEFAULT_SAF_TARGET_PACKAGES));
    }

    private void savePrefs() {
        String allow = packageListOrEmpty(callerAllowlist);
        String block = packageListOrEmpty(callerBlocklist);
        String radioSource = requiredPackage(radioSourcePackage, radioEnabled.isChecked());
        String radioTarget = requiredPackage(radioTargetPackage, radioEnabled.isChecked());
        String radioClass = optionalClass(radioTargetClass);
        String musicSource = requiredPackage(musicSourcePackage, musicEnabled.isChecked());
        String musicSourceCls = optionalClass(musicSourceClass);
        String musicTarget = requiredPackage(musicTargetPackage, musicEnabled.isChecked());
        String musicTargetCls = optionalClass(musicTargetClass);
        String safTargets = requiredPackageList(safTargetPackages, safEnabled.isChecked());

        if (allow == null || block == null || radioSource == null || radioTarget == null || radioClass == INVALID
                || musicSource == null || musicSourceCls == INVALID || musicTarget == null || musicTargetCls == INVALID
                || safTargets == null) {
            updateStatus("Not saved: fix invalid package/class names.");
            return;
        }

        prefs.edit()
                .putBoolean(BridgeConfig.KEY_PM_COMPAT_ENABLED, pmCompatEnabled.isChecked())
                .putBoolean(BridgeConfig.KEY_VERBOSE_LOGGING, verboseLogging.isChecked())
                .putString(BridgeConfig.KEY_CALLER_ALLOWLIST, allow)
                .putString(BridgeConfig.KEY_CALLER_BLOCKLIST, block)
                .putBoolean(BridgeConfig.KEY_RADIO_ENABLED, radioEnabled.isChecked())
                .putString(BridgeConfig.KEY_RADIO_SOURCE_PACKAGE, radioSource)
                .putString(BridgeConfig.KEY_RADIO_TARGET_PACKAGE, radioTarget)
                .putString(BridgeConfig.KEY_RADIO_TARGET_CLASS, radioClass == null ? "" : radioClass)
                .putBoolean(BridgeConfig.KEY_RADIO_USE_LAUNCH_INTENT, radioUseLaunchIntent.isChecked())
                .putBoolean(BridgeConfig.KEY_RADIO_SPOOF_PM, radioSpoofPm.isChecked())
                .putBoolean(BridgeConfig.KEY_MUSIC_ENABLED, musicEnabled.isChecked())
                .putString(BridgeConfig.KEY_MUSIC_SOURCE_PACKAGE, musicSource)
                .putString(BridgeConfig.KEY_MUSIC_SOURCE_CLASS, musicSourceCls == null ? "" : musicSourceCls)
                .putString(BridgeConfig.KEY_MUSIC_TARGET_PACKAGE, musicTarget)
                .putString(BridgeConfig.KEY_MUSIC_TARGET_CLASS, musicTargetCls == null ? "" : musicTargetCls)
                .putBoolean(BridgeConfig.KEY_MUSIC_USE_LAUNCH_INTENT, musicUseLaunchIntent.isChecked())
                .putBoolean(BridgeConfig.KEY_MUSIC_SPOOF_PM, musicSpoofPm.isChecked())
                .putBoolean(BridgeConfig.KEY_SAF_ENABLED, safEnabled.isChecked())
                .putString(BridgeConfig.KEY_SAF_TARGET_PACKAGES, safTargets)
                .apply();
        makePreferencesReadableBestEffort();
        BridgeRules.invalidateForTestOrUi();
        String message = "Saved. Restart scoped apps or reboot for hooks to reload.";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        updateStatus(message);
    }

    private void resetDefaults() {
        prefs.edit().clear().apply();
        BridgeConfig.ensureDefaults(prefs);
        loadFromPrefs();
        makePreferencesReadableBestEffort();
        updateStatus("Defaults restored. Save/restart scoped apps or reboot.");
    }

    private static final String INVALID = "\u0000INVALID";

    private String requiredPackage(EditText field, boolean required) {
        String value = BridgeConfig.normalize(field.getText().toString());
        if (!required && value == null) return "";
        String pkg = BridgeConfig.normalizePackage(value);
        if (pkg == null) markInvalid(field); else field.setError(null);
        return pkg;
    }

    private String optionalClass(EditText field) {
        String value = BridgeConfig.normalize(field.getText().toString());
        if (value == null) {
            field.setError(null);
            return null;
        }
        String cls = BridgeConfig.normalizeClass(value);
        if (cls == null) {
            markInvalid(field);
            return INVALID;
        }
        field.setError(null);
        return cls;
    }

    private String requiredPackageList(EditText field, boolean required) {
        String value = BridgeConfig.normalize(field.getText().toString());
        if (!required && value == null) return "";
        String[] packages = BridgeConfig.splitPackageList(value);
        if (packages.length == 0) {
            markInvalid(field);
            return null;
        }
        field.setError(null);
        StringBuilder b = new StringBuilder();
        for (String p : packages) {
            if (b.length() > 0) b.append(',');
            b.append(p);
        }
        return b.toString();
    }

    private String packageListOrEmpty(EditText field) {
        String value = BridgeConfig.normalize(field.getText().toString());
        if (value == null) {
            field.setError(null);
            return "";
        }
        String[] packages = BridgeConfig.splitPackageList(value);
        if (packages.length == 0) {
            markInvalid(field);
            return null;
        }
        field.setError(null);
        StringBuilder b = new StringBuilder();
        for (String p : packages) {
            if (b.length() > 0) b.append(',');
            b.append(p);
        }
        return b.toString();
    }

    private String buildDiagnosticSummary() {
        return "Diagnostic summary\n"
                + "Android SDK: " + android.os.Build.VERSION.SDK_INT + "\n"
                + "Module: " + BuildConfig.APPLICATION_ID + " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n"
                + "Allowlist: " + callerAllowlist.getText() + "\n"
                + "Blocklist: " + callerBlocklist.getText() + "\n"
                + "Radio: " + radioSourcePackage.getText() + " -> " + radioTargetPackage.getText() + "/" + radioTargetClass.getText() + " enabled=" + radioEnabled.isChecked() + "\n"
                + "Music: " + musicSourcePackage.getText() + "/" + musicSourceClass.getText() + " -> " + musicTargetPackage.getText() + "/" + musicTargetClass.getText() + " enabled=" + musicEnabled.isChecked() + "\n"
                + "SAF: " + safTargetPackages.getText() + " enabled=" + safEnabled.isChecked() + "\n"
                + "PM compat: " + pmCompatEnabled.isChecked() + "; verbose: " + verboseLogging.isChecked();
    }

    private void makePreferencesReadableBestEffort() {
        try {
            File dataDir = new File(getApplicationInfo().dataDir);
            File prefsDir = new File(dataDir, "shared_prefs");
            File prefsFile = new File(prefsDir, BridgeConfig.PREFS_NAME + ".xml");
            dataDir.setExecutable(true, false);
            prefsDir.setExecutable(true, false);
            prefsDir.setReadable(true, false);
            prefsFile.setReadable(true, false);
        } catch (Throwable ignored) {
            // LSPosed/Vector-provided XSharedPreferences may not need this; best effort only.
        }
    }

    private void updateStatus(String message) {
        status.setText("Status: " + message + "\nPrefs: " + BuildConfig.APPLICATION_ID + "/" + BridgeConfig.PREFS_NAME);
    }

    private EditText edit(String hint, String value) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setText(value);
        return e;
    }

    private CheckBox checkBox(String label) {
        CheckBox c = new CheckBox(this);
        c.setText(label);
        c.setTextSize(14f);
        return c;
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        int p = 6;
        t.setPadding(0, p, 0, p);
        return t;
    }

    private static void addAll(LinearLayout layout, View... views) {
        for (View view : views) layout.addView(view);
    }

    private static void setEnabledAll(boolean enabled, View... views) {
        for (View view : views) view.setEnabled(enabled);
    }

    private static void markInvalid(EditText field) {
        field.setError("Invalid package/class name");
    }
}
