package my.shanunid.linespoofer.hooks;

import android.os.SystemClock;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Spoofs system uptime and boot timestamp via SystemClock.
 * Supports custom and randomized operational uptime.
 */
public final class TimeHooks {

    private static final String TAG = "LineSpoofer-Time";

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
                            long fakeBaseMs = ConfigManager.getSpoofedUptimeMs();
                            param.setResult(fakeBaseMs + (realElapsed % 3600000L));
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
                            long fakeBaseMs = ConfigManager.getSpoofedUptimeMs();
                            param.setResult(fakeBaseMs + (realUptime % 3600000L));
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                SystemClock.class,
                "elapsedRealtimeNanos",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isUptimeSpoofEnabled()) {
                                return;
                            }
                            long realNanos = (long) param.getResult();
                            long fakeBaseNanos = ConfigManager.getSpoofedUptimeMs() * 1_000_000L;
                            param.setResult(fakeBaseNanos + (realNanos % 3_600_000_000_000L));
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            AppLogger.debug(TAG, "SystemClock uptime hooks loaded successfully");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook SystemClock: " + throwable.getMessage(), throwable);
        }
    }
}
