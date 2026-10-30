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
 *
 * Implements strict re-entrancy protection and cached offsets to prevent
 * recursion or stack overflow on high-frequency SystemClock calls.
 */
public final class TimeHooks {

    private static final String TAG = "LineSpoofer-Time";
    private static final ThreadLocal<Boolean> IN_HOOK = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static volatile boolean sUptimeSpoofEnabled = false;
    private static volatile long sFakeBaseMs = 0L;
    private static volatile boolean sHooksInstalled = false;

    private TimeHooks() {
    }

    public static synchronized void updateConfig() {
        try {
            sUptimeSpoofEnabled = ConfigManager.isUptimeSpoofEnabled();
            sFakeBaseMs = sUptimeSpoofEnabled ? ConfigManager.getSpoofedUptimeMs() : 0L;
        } catch (Throwable t) {
            sUptimeSpoofEnabled = false;
            sFakeBaseMs = 0L;
        }
    }

    public static long getFakeBaseMs() {
        return sFakeBaseMs;
    }

    public static boolean isUptimeSpoofEnabled() {
        return sUptimeSpoofEnabled && sFakeBaseMs > 0L;
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        updateConfig();
        if (!sUptimeSpoofEnabled || sFakeBaseMs <= 0L) {
            return;
        }

        if (sHooksInstalled) {
            return;
        }
        sHooksInstalled = true;

        try {
            XposedHelpers.findAndHookMethod(
                SystemClock.class,
                "elapsedRealtime",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (IN_HOOK.get()) {
                            return;
                        }
                        if (!sUptimeSpoofEnabled || sFakeBaseMs <= 0L) {
                            return;
                        }
                        IN_HOOK.set(Boolean.TRUE);
                        try {
                            long realElapsed = (long) param.getResult();
                            param.setResult(sFakeBaseMs + realElapsed);
                        } catch (Throwable ignored) {
                        } finally {
                            IN_HOOK.set(Boolean.FALSE);
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
                        if (IN_HOOK.get()) {
                            return;
                        }
                        if (!sUptimeSpoofEnabled || sFakeBaseMs <= 0L) {
                            return;
                        }
                        IN_HOOK.set(Boolean.TRUE);
                        try {
                            long realUptime = (long) param.getResult();
                            param.setResult(sFakeBaseMs + realUptime);
                        } catch (Throwable ignored) {
                        } finally {
                            IN_HOOK.set(Boolean.FALSE);
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
                        if (IN_HOOK.get()) {
                            return;
                        }
                        if (!sUptimeSpoofEnabled || sFakeBaseMs <= 0L) {
                            return;
                        }
                        IN_HOOK.set(Boolean.TRUE);
                        try {
                            long realNanos = (long) param.getResult();
                            long fakeBaseNanos = sFakeBaseMs * 1_000_000L;
                            param.setResult(fakeBaseNanos + realNanos);
                        } catch (Throwable ignored) {
                        } finally {
                            IN_HOOK.set(Boolean.FALSE);
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
