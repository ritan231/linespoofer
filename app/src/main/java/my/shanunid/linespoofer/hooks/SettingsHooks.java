package my.shanunid.linespoofer.hooks;

import android.content.ContentResolver;
import android.provider.Settings;

import my.shanunid.linespoofer.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks Settings.Secure and Settings.Global to spoof:
 * 1. Android ID and GSF ID
 * 2. Developer Options and ADB enabled status (anti-detection)
 */
public class SettingsHooks {

    private static final String TAG = "LineSpoofer-Settings";
    private static final String ANDROID_ID = "android_id";
    private static final String GSF_ID = "gsf_id";
    private static final String ADB_ENABLED = "adb_enabled";
    private static final String DEVELOPMENT_SETTINGS_ENABLED = "development_settings_enabled";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        hookSettingsSecure(lpparam);
        hookSettingsGlobal(lpparam);
    }

    private static void hookSettingsSecure(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> settingsSecure = XposedHelpers.findClassIfExists(
                "android.provider.Settings$Secure",
                lpparam.classLoader
        );

        if (settingsSecure == null) {
            return;
        }

        // Hook getString(ContentResolver, String)
        try {
            XposedHelpers.findAndHookMethod(settingsSecure, "getString",
                    ContentResolver.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[1];
                            if (name == null) {
                                return;
                            }

                            if (ANDROID_ID.equals(name)) {
                                String spoofedValue = ConfigManager.getAndroidId();
                                if (spoofedValue != null) {
                                    param.setResult(spoofedValue);
                                }
                                return;
                            }

                            if (name.contains("gsf") || GSF_ID.equals(name)) {
                                String spoofedValue = ConfigManager.getGSFId();
                                if (spoofedValue != null) {
                                    param.setResult(spoofedValue);
                                }
                                return;
                            }

                            if (ADB_ENABLED.equals(name) || DEVELOPMENT_SETTINGS_ENABLED.equals(name)) {
                                param.setResult("0");
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }

        // Hook getInt(ContentResolver, String)
        try {
            XposedHelpers.findAndHookMethod(settingsSecure, "getInt",
                    ContentResolver.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[1];
                            if (ADB_ENABLED.equals(name) || DEVELOPMENT_SETTINGS_ENABLED.equals(name)) {
                                param.setResult(0);
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }

        // Hook getInt(ContentResolver, String, int)
        try {
            XposedHelpers.findAndHookMethod(settingsSecure, "getInt",
                    ContentResolver.class, String.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[1];
                            if (ADB_ENABLED.equals(name) || DEVELOPMENT_SETTINGS_ENABLED.equals(name)) {
                                param.setResult(0);
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    private static void hookSettingsGlobal(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> settingsGlobal = XposedHelpers.findClassIfExists(
                "android.provider.Settings$Global",
                lpparam.classLoader
        );

        if (settingsGlobal == null) {
            return;
        }

        // Hook getString(ContentResolver, String)
        try {
            XposedHelpers.findAndHookMethod(settingsGlobal, "getString",
                    ContentResolver.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[1];
                            if (ADB_ENABLED.equals(name) || DEVELOPMENT_SETTINGS_ENABLED.equals(name)) {
                                param.setResult("0");
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }

        // Hook getInt(ContentResolver, String)
        try {
            XposedHelpers.findAndHookMethod(settingsGlobal, "getInt",
                    ContentResolver.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[1];
                            if (ADB_ENABLED.equals(name) || DEVELOPMENT_SETTINGS_ENABLED.equals(name)) {
                                param.setResult(0);
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }

        // Hook getInt(ContentResolver, String, int)
        try {
            XposedHelpers.findAndHookMethod(settingsGlobal, "getInt",
                    ContentResolver.class, String.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[1];
                            if (ADB_ENABLED.equals(name) || DEVELOPMENT_SETTINGS_ENABLED.equals(name)) {
                                param.setResult(0);
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }
    }
}
