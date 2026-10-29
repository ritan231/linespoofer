package my.shanunid.linespoofer.hooks;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Spoofs BatteryManager metrics and sticky ACTION_BATTERY_CHANGED broadcast.
 * Neutralizes emulator bot detection that relies on static battery patterns (100% or 50% AC).
 */
public final class BatteryHooks {

    private static final String TAG = "LineSpoofer-Battery";

    private BatteryHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!ConfigManager.isBatterySpoofEnabled()) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                BatteryManager.class,
                "getIntProperty",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isBatterySpoofEnabled()) {
                                return;
                            }
                            int propertyId = (int) param.args[0];
                            int level = ConfigManager.getBatteryLevel();
                            switch (propertyId) {
                                case BatteryManager.BATTERY_PROPERTY_CAPACITY:
                                    param.setResult(level);
                                    break;
                                case BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER:
                                    param.setResult((int) (5000L * 1000L * level / 100L));
                                    break;
                                case BatteryManager.BATTERY_PROPERTY_CURRENT_NOW:
                                    param.setResult(-220000); // Plausible discharge: -220 mA
                                    break;
                                case BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE:
                                    param.setResult(-200000);
                                    break;
                                case BatteryManager.BATTERY_PROPERTY_STATUS:
                                    param.setResult(BatteryManager.BATTERY_STATUS_DISCHARGING);
                                    break;
                                default:
                                    break;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );
            AppLogger.debug(TAG, "BatteryManager.getIntProperty hooked");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook BatteryManager.getIntProperty: " + throwable.getMessage(), throwable);
        }

        try {
            XposedHelpers.findAndHookMethod(
                "android.content.ContextWrapper",
                lpparam.classLoader,
                "registerReceiver",
                BroadcastReceiver.class,
                IntentFilter.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isBatterySpoofEnabled()) {
                                return;
                            }
                            IntentFilter filter = (IntentFilter) param.args[1];
                            if (filter != null && filter.hasAction(Intent.ACTION_BATTERY_CHANGED)) {
                                Intent intent = (Intent) param.getResult();
                                if (intent != null) {
                                    applyBatteryIntentExtras(intent);
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                "android.content.ContextWrapper",
                lpparam.classLoader,
                "registerReceiver",
                BroadcastReceiver.class,
                IntentFilter.class,
                String.class,
                android.os.Handler.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isBatterySpoofEnabled()) {
                                return;
                            }
                            IntentFilter filter = (IntentFilter) param.args[1];
                            if (filter != null && filter.hasAction(Intent.ACTION_BATTERY_CHANGED)) {
                                Intent intent = (Intent) param.getResult();
                                if (intent != null) {
                                    applyBatteryIntentExtras(intent);
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );
            AppLogger.debug(TAG, "ContextWrapper.registerReceiver (battery) hooked");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook ContextWrapper.registerReceiver: " + throwable.getMessage(), throwable);
        }
    }

    private static void applyBatteryIntentExtras(Intent intent) {
        int level = ConfigManager.getBatteryLevel();
        intent.putExtra(BatteryManager.EXTRA_LEVEL, level);
        intent.putExtra(BatteryManager.EXTRA_SCALE, 100);
        intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_DISCHARGING);
        intent.putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD);
        intent.putExtra(BatteryManager.EXTRA_PLUGGED, 0);
        intent.putExtra(BatteryManager.EXTRA_VOLTAGE, 4120); // 4.12V
        intent.putExtra(BatteryManager.EXTRA_TEMPERATURE, 315); // 31.5 C
        intent.putExtra(BatteryManager.EXTRA_TECHNOLOGY, "Li-poly");
        intent.putExtra(BatteryManager.EXTRA_PRESENT, true);
    }
}
