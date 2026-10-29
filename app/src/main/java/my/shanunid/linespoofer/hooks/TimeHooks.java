package my.shanunid.linespoofer.hooks;

import android.os.SystemClock;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Spoofs system uptime and boot timestamp via SystemClock.
 * Mitigates zero-uptime or excessively long uptime detection used by device intelligence SDKs.
 */
public final class TimeHooks {

    private static final String TAG = "LineSpoofer-Time";
    // Simulated normal operational uptime: ~7 hours 42 minutes (27,720,000 ms)
    private static final long PLAUSIBLE_MIN_UPTIME_MS = 27_720_000L;

    private TimeHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!ConfigManager.isUptimeSpoofEnabled()) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                SystemClock.class,
                "elapsedRealtime",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isUptimeSpoofEnabled()) {
                                return;
                            }
                            long realElapsed = (long) param.getResult();
                            if (realElapsed < 3_600_000L) {
                                param.setResult(realElapsed + PLAUSIBLE_MIN_UPTIME_MS);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                SystemClock.class,
                "uptimeMillis",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isUptimeSpoofEnabled()) {
                                return;
                            }
                            long realUptime = (long) param.getResult();
                            if (realUptime < 3_600_000L) {
                                param.setResult(realUptime + PLAUSIBLE_MIN_UPTIME_MS);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );
            AppLogger.debug(TAG, "SystemClock uptime hooks loaded");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook SystemClock: " + throwable.getMessage(), throwable);
        }
    }
}
