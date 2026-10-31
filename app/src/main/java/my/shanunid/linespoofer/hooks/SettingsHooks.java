package my.shanunid.linespoofer.hooks;

import android.content.ContentResolver;
import android.provider.Settings;

import my.shanunid.linespoofer.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks Settings.Secure, Settings.Global, and Settings.System to spoof:
 * 1. Android ID and GSF ID
 * 2. Developer Options and ADB enabled status (anti-detection)
 * 3. Bluetooth MAC & name, Wi-Fi MAC, Device name
 * 4. Time zone & clock format (auto_time_zone, time_12_24)
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
        hookSettingsSystem(lpparam);
    }

    private static String resolveSettingString(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        if (ANDROID_ID.equals(name)) {
            String spoofedValue = ConfigManager.getAndroidId();
            if (spoofedValue != null) return spoofedValue;
        }

        if (name.contains("gsf") || GSF_ID.equals(name)) {
            String spoofedValue = ConfigManager.getGSFId();
            if (spoofedValue != null) return spoofedValue;
        }

        if (ADB_ENABLED.equals(name) || DEVELOPMENT_SETTINGS_ENABLED.equals(name)) {
            return "0";
        }

        if ("bluetooth_address".equalsIgnoreCase(name) || "bluetooth_mac".equalsIgnoreCase(name)) {
            if (ConfigManager.isBluetoothSpoofEnabled()) {
                String mac = ConfigManager.getSpoofedBluetoothMac();
                if (mac != null) return mac;
            }
        }

        if ("bluetooth_name".equalsIgnoreCase(name)) {
            if (ConfigManager.isBluetoothSpoofEnabled()) {
                String btName = ConfigManager.getSpoofedBluetoothName();
                if (btName != null) return btName;
            }
        }

        if ("wifi_mac_address".equalsIgnoreCase(name) || "wifi_mac".equalsIgnoreCase(name)) {
            if (ConfigManager.isMacSpoofEnabled() || ConfigManager.isWifiSpoofEnabled()) {
                String mac = ConfigManager.getSpoofedMacAddress();
                if (mac != null) return mac;
            }
        }

        if ("device_name".equalsIgnoreCase(name)) {
            String model = ConfigManager.getBuildModel();
            if (model != null && !model.isEmpty()) return model;
        }

        if ("auto_time_zone".equalsIgnoreCase(name)) {
            return "0";
        }

        if ("time_zone".equalsIgnoreCase(name) || "timezone".equalsIgnoreCase(name)) {
            String tz = ConfigManager.getTimezone();
            if (tz != null && !tz.trim().isEmpty()) return tz.trim();
        }

        if ("locale".equalsIgnoreCase(name) || "system_locales".equalsIgnoreCase(name)) {
            String loc = ConfigManager.getLocaleTag();
            if (loc != null && !loc.trim().isEmpty()) return loc.trim();
        }

        if ("time_12_24".equalsIgnoreCase(name)) {
            return "24";
        }

        return null;
    }

    private static void hookSettingsSecure(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> settingsSecure = XposedHelpers.findClassIfExists("android.provider.Settings$Secure", lpparam.classLoader);
        if (settingsSecure == null) {
            return;
        }

        XC_MethodHook stringHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String name = (String) param.args[1];
                String spoofed = resolveSettingString(name);
                if (spoofed != null) {
                    param.setResult(spoofed);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(settingsSecure, "getString", ContentResolver.class, String.class, stringHook);
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(settingsSecure, "getStringForUser", ContentResolver.class, String.class, int.class, stringHook);
        } catch (Throwable ignored) {
        }

        XC_MethodHook intHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String name = (String) param.args[1];
                if (ADB_ENABLED.equals(name) || DEVELOPMENT_SETTINGS_ENABLED.equals(name) || "auto_time_zone".equals(name)) {
                    param.setResult(0);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(settingsSecure, "getInt", ContentResolver.class, String.class, intHook);
            XposedHelpers.findAndHookMethod(settingsSecure, "getInt", ContentResolver.class, String.class, int.class, intHook);
            XposedHelpers.findAndHookMethod(settingsSecure, "getIntForUser", ContentResolver.class, String.class, int.class, intHook);
            XposedHelpers.findAndHookMethod(settingsSecure, "getIntForUser", ContentResolver.class, String.class, int.class, int.class, intHook);
        } catch (Throwable ignored) {
        }
    }

    private static void hookSettingsGlobal(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> settingsGlobal = XposedHelpers.findClassIfExists("android.provider.Settings$Global", lpparam.classLoader);
        if (settingsGlobal == null) {
            return;
        }

        XC_MethodHook stringHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String name = (String) param.args[1];
                String spoofed = resolveSettingString(name);
                if (spoofed != null) {
                    param.setResult(spoofed);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(settingsGlobal, "getString", ContentResolver.class, String.class, stringHook);
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(settingsGlobal, "getStringForUser", ContentResolver.class, String.class, int.class, stringHook);
        } catch (Throwable ignored) {
        }

        XC_MethodHook intHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String name = (String) param.args[1];
                if (ADB_ENABLED.equals(name) || DEVELOPMENT_SETTINGS_ENABLED.equals(name) || "auto_time_zone".equals(name)) {
                    param.setResult(0);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(settingsGlobal, "getInt", ContentResolver.class, String.class, intHook);
            XposedHelpers.findAndHookMethod(settingsGlobal, "getInt", ContentResolver.class, String.class, int.class, intHook);
            XposedHelpers.findAndHookMethod(settingsGlobal, "getIntForUser", ContentResolver.class, String.class, int.class, intHook);
            XposedHelpers.findAndHookMethod(settingsGlobal, "getIntForUser", ContentResolver.class, String.class, int.class, int.class, intHook);
        } catch (Throwable ignored) {
        }
    }

    private static void hookSettingsSystem(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> settingsSystem = XposedHelpers.findClassIfExists("android.provider.Settings$System", lpparam.classLoader);
        if (settingsSystem == null) {
            return;
        }

        XC_MethodHook stringHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String name = (String) param.args[1];
                String spoofed = resolveSettingString(name);
                if (spoofed != null) {
                    param.setResult(spoofed);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(settingsSystem, "getString", ContentResolver.class, String.class, stringHook);
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(settingsSystem, "getStringForUser", ContentResolver.class, String.class, int.class, stringHook);
        } catch (Throwable ignored) {
        }

        XC_MethodHook intHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String name = (String) param.args[1];
                if (ADB_ENABLED.equals(name) || DEVELOPMENT_SETTINGS_ENABLED.equals(name) || "auto_time_zone".equals(name)) {
                    param.setResult(0);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(settingsSystem, "getInt", ContentResolver.class, String.class, intHook);
            XposedHelpers.findAndHookMethod(settingsSystem, "getInt", ContentResolver.class, String.class, int.class, intHook);
            XposedHelpers.findAndHookMethod(settingsSystem, "getIntForUser", ContentResolver.class, String.class, int.class, intHook);
            XposedHelpers.findAndHookMethod(settingsSystem, "getIntForUser", ContentResolver.class, String.class, int.class, int.class, intHook);
        } catch (Throwable ignored) {
        }
    }
}
