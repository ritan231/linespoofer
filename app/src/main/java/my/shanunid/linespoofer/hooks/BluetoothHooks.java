package my.shanunid.linespoofer.hooks;

import android.bluetooth.BluetoothAdapter;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Spoofs Bluetooth adapter name and hardware MAC address.
 * Standardizes privacy response to prevent tracking via persistent Bluetooth device identities.
 */
public final class BluetoothHooks {

    private static final String TAG = "LineSpoofer-Bluetooth";

    private BluetoothHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!ConfigManager.isBluetoothSpoofEnabled()) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                BluetoothAdapter.class,
                "getName",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isBluetoothSpoofEnabled()) {
                                return;
                            }
                            String model = ConfigManager.getBuildModel();
                            if (model != null && !model.isEmpty()) {
                                param.setResult(model);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                BluetoothAdapter.class,
                "getAddress",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isBluetoothSpoofEnabled()) {
                                return;
                            }
                            // Default privacy placeholder address
                            param.setResult("02:00:00:00:00:00");
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            AppLogger.debug(TAG, "BluetoothHooks loaded");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook BluetoothAdapter: " + throwable.getMessage(), throwable);
        }
    }
}
