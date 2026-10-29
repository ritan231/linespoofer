package my.shanunid.linespoofer.hooks;

import android.net.wifi.WifiInfo;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Spoofs Wi-Fi connection metadata (SSID, BSSID, RSSI, Frequency, MAC).
 * Neutralizes Wi-Fi geolocation tracking and network environment fingerprinting.
 */
public final class WifiHooks {

    private static final String TAG = "LineSpoofer-Wifi";

    private WifiHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!ConfigManager.isWifiSpoofEnabled()) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                WifiInfo.class,
                "getSSID",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (ConfigManager.isWifiSpoofEnabled()) {
                                param.setResult("\"Home-WiFi-5G\"");
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                WifiInfo.class,
                "getBSSID",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (ConfigManager.isWifiSpoofEnabled()) {
                                param.setResult("02:00:00:00:00:00");
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                WifiInfo.class,
                "getRssi",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (ConfigManager.isWifiSpoofEnabled()) {
                                param.setResult(-55);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                WifiInfo.class,
                "getFrequency",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (ConfigManager.isWifiSpoofEnabled()) {
                                param.setResult(5180);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                WifiInfo.class,
                "getMacAddress",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (ConfigManager.isWifiSpoofEnabled()) {
                                param.setResult("02:00:00:00:00:00");
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            AppLogger.debug(TAG, "WifiHooks loaded");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook WifiInfo: " + throwable.getMessage(), throwable);
        }
    }
}
