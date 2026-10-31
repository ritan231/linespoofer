package my.shanunid.linespoofer.hooks;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.lang.reflect.Constructor;
import java.net.NetworkInterface;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Spoofs Wi-Fi connection metadata (SSID, BSSID, RSSI, Frequency, MAC)
 * and hooks NetworkInterface.getHardwareAddress() across all network interfaces.
 */
public final class WifiHooks {

    private static final String TAG = "LineSpoofer-Wifi";

    private WifiHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        // 1. Hook NetworkInterface.getHardwareAddress() (Primary MAC API on modern Android)
        try {
            XposedHelpers.findAndHookMethod(
                NetworkInterface.class.getName(),
                lpparam.classLoader,
                "getHardwareAddress",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isMacSpoofEnabled()) {
                                return;
                            }
                            NetworkInterface ni = (NetworkInterface) param.thisObject;
                            if (ni != null) {
                                String name = ni.getName();
                                if (name != null && (name.startsWith("wlan") || name.startsWith("eth") || name.startsWith("p2p"))) {
                                    byte[] macBytes = ConfigManager.getSpoofedMacBytes();
                                    if (macBytes != null && macBytes.length == 6) {
                                        param.setResult(macBytes);
                                    }
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );
            AppLogger.debug(TAG, "NetworkInterface.getHardwareAddress hooked");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook NetworkInterface.getHardwareAddress: " + throwable.getMessage(), throwable);
        }

        // 2. Hook WifiInfo APIs
        try {
            XposedHelpers.findAndHookMethod(
                WifiInfo.class,
                "getSSID",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (ConfigManager.isWifiSpoofEnabled()) {
                                param.setResult(ConfigManager.getSpoofedWifiSsid());
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
                                param.setResult(ConfigManager.getSpoofedWifiBssid());
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
                                param.setResult(ConfigManager.getSpoofedWifiRssi());
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
                            if (ConfigManager.isMacSpoofEnabled() || ConfigManager.isWifiSpoofEnabled()) {
                                param.setResult(ConfigManager.getSpoofedMacAddress());
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                WifiInfo.class,
                "getLinkSpeed",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (ConfigManager.isWifiSpoofEnabled()) {
                                param.setResult(866);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            AppLogger.debug(TAG, "WifiHooks loaded successfully");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook WifiInfo: " + throwable.getMessage(), throwable);
        }

        // 3. Hook WifiManager.getConnectionInfo()
        try {
            Class<?> wifiManagerClass = XposedHelpers.findClassIfExists("android.net.wifi.WifiManager", lpparam.classLoader);
            if (wifiManagerClass != null) {
                XposedHelpers.findAndHookMethod(wifiManagerClass, "getConnectionInfo", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (ConfigManager.isWifiSpoofEnabled() || ConfigManager.isMacSpoofEnabled()) {
                            try {
                                Object res = param.getResult();
                                if (res == null) {
                                    res = createSyntheticWifiInfo();
                                    param.setResult(res);
                                }
                                applyWifiInfoFields(res);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(wifiManagerClass, "getScanResults", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isWifiSpoofEnabled()) return;
                            Object res = param.getResult();
                            if (res instanceof java.util.List) {
                                for (Object sr : (java.util.List<?>) res) {
                                    if (sr != null) {
                                        try {
                                            java.lang.reflect.Field bField = XposedHelpers.findFieldIfExists(sr.getClass(), "BSSID");
                                            if (bField != null) {
                                                bField.setAccessible(true);
                                                bField.set(sr, ConfigManager.getSpoofedWifiBssid());
                                            }
                                            java.lang.reflect.Field sField = XposedHelpers.findFieldIfExists(sr.getClass(), "SSID");
                                            if (sField != null) {
                                                sField.setAccessible(true);
                                                sField.set(sr, ConfigManager.getSpoofedWifiSsid().replace("\"", ""));
                                            }
                                        } catch (Throwable ignored) {}
                                    }
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                });
            }
        } catch (Throwable ignored) {
        }

        // 4. Hook NetworkCapabilities.getTransportInfo() (Android 10+)
        try {
            Class<?> ncClass = XposedHelpers.findClassIfExists("android.net.NetworkCapabilities", lpparam.classLoader);
            if (ncClass != null) {
                XposedHelpers.findAndHookMethod(ncClass, "getTransportInfo", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (ConfigManager.isWifiSpoofEnabled() || ConfigManager.isMacSpoofEnabled()) {
                            try {
                                Object res = param.getResult();
                                if (res == null && ConfigManager.isWifiSpoofEnabled()) {
                                    res = createSyntheticWifiInfo();
                                    param.setResult(res);
                                }
                                applyWifiInfoFields(res);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                });
            }
        } catch (Throwable ignored) {
        }
    }

    private static void applyWifiInfoFields(Object wifiInfo) {
        if (wifiInfo == null) return;
        try {
            if (ConfigManager.isWifiSpoofEnabled()) {
                setFieldQuietly(wifiInfo, "mBSSID", ConfigManager.getSpoofedWifiBssid());
                setFieldQuietly(wifiInfo, "mRssi", ConfigManager.getSpoofedWifiRssi());
            }
            if (ConfigManager.isMacSpoofEnabled() || ConfigManager.isWifiSpoofEnabled()) {
                setFieldQuietly(wifiInfo, "mMacAddress", ConfigManager.getSpoofedMacAddress());
            }
        } catch (Throwable ignored) {
        }
    }

    private static void setFieldQuietly(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = XposedHelpers.findFieldIfExists(target.getClass(), fieldName);
            if (f != null) {
                f.setAccessible(true);
                f.set(target, value);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object createSyntheticWifiInfo() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            java.lang.reflect.Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            java.lang.reflect.Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
            return allocateInstance.invoke(unsafe, WifiInfo.class);
        } catch (Throwable ignored) {
        }
        try {
            Constructor<?> constructor = WifiInfo.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
