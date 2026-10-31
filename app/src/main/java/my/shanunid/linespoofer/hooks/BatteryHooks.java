package my.shanunid.linespoofer.hooks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;

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
                                    param.setResult(ConfigManager.getBatteryStatus());
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

        XC_MethodHook receiverHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (!ConfigManager.isBatterySpoofEnabled()) {
                        return;
                    }
                    boolean isBatteryQuery = false;
                    if (param.args != null) {
                        for (Object arg : param.args) {
                            if (arg instanceof IntentFilter) {
                                IntentFilter filter = (IntentFilter) arg;
                                if (filter.hasAction(Intent.ACTION_BATTERY_CHANGED)) {
                                    isBatteryQuery = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (isBatteryQuery || (param.getResult() instanceof Intent && Intent.ACTION_BATTERY_CHANGED.equals(((Intent) param.getResult()).getAction()))) {
                        Intent intent = (Intent) param.getResult();
                        if (intent == null) {
                            intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
                        }
                        applyBatteryIntentExtras(intent);
                        param.setResult(intent);
                    }
                } catch (Throwable ignored) {
                }
            }
        };

        try {
            Class<?> contextImplClass = XposedHelpers.findClassIfExists("android.app.ContextImpl", lpparam.classLoader);
            if (contextImplClass != null) {
                for (java.lang.reflect.Method m : contextImplClass.getDeclaredMethods()) {
                    if ("registerReceiverInternal".equals(m.getName())) {
                        XposedHelpers.findAndHookMethod(contextImplClass, "registerReceiverInternal", m.getParameterTypes(), receiverHook);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(
                "android.content.ContextWrapper",
                lpparam.classLoader,
                "registerReceiver",
                BroadcastReceiver.class,
                IntentFilter.class,
                receiverHook
            );

            XposedHelpers.findAndHookMethod(
                "android.content.ContextWrapper",
                lpparam.classLoader,
                "registerReceiver",
                BroadcastReceiver.class,
                IntentFilter.class,
                int.class,
                receiverHook
            );

            XposedHelpers.findAndHookMethod(
                "android.content.ContextWrapper",
                lpparam.classLoader,
                "registerReceiver",
                BroadcastReceiver.class,
                IntentFilter.class,
                String.class,
                Handler.class,
                receiverHook
            );

            XposedHelpers.findAndHookMethod(
                "android.content.ContextWrapper",
                lpparam.classLoader,
                "registerReceiver",
                BroadcastReceiver.class,
                IntentFilter.class,
                String.class,
                Handler.class,
                int.class,
                receiverHook
            );
            AppLogger.debug(TAG, "ContextWrapper.registerReceiver (battery) hooked across all overloads");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook ContextWrapper.registerReceiver: " + throwable.getMessage(), throwable);
        }
    }

    private static void applyBatteryIntentExtras(Intent intent) {
        int level = ConfigManager.getBatteryLevel();
        intent.putExtra(BatteryManager.EXTRA_LEVEL, level);
        intent.putExtra(BatteryManager.EXTRA_SCALE, 100);
        intent.putExtra(BatteryManager.EXTRA_STATUS, ConfigManager.getBatteryStatus());
        intent.putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD);
        intent.putExtra(BatteryManager.EXTRA_PLUGGED, 0);
        intent.putExtra(BatteryManager.EXTRA_VOLTAGE, 4120); // 4.12V
        intent.putExtra(BatteryManager.EXTRA_TEMPERATURE, ConfigManager.getBatteryTemperature());
        intent.putExtra(BatteryManager.EXTRA_TECHNOLOGY, "Li-poly");
        intent.putExtra(BatteryManager.EXTRA_PRESENT, true);
    }
}
