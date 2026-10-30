package my.shanunid.linespoofer.hooks;

import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.SystemClock;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Spoofs GPS / FusedLocationProvider coordinates and strips mock provider flags.
 * Masks Location.isFromMockProvider() to prevent anti-cheat and fraud engine detection.
 */
public final class LocationHooks {

    private static final String TAG = "LineSpoofer-Location";

    private LocationHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!ConfigManager.isLocationSpoofEnabled()) {
            return;
        }

        try {
            // 1. Direct coordinate getters on Location class
            XposedHelpers.findAndHookMethod(
                Location.class,
                "getLatitude",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (ConfigManager.isLocationSpoofEnabled()) {
                                param.setResult(ConfigManager.getSpoofedLatitude());
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                Location.class,
                "getLongitude",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (ConfigManager.isLocationSpoofEnabled()) {
                                param.setResult(ConfigManager.getSpoofedLongitude());
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            // 2. Anti-mock detection
            XposedHelpers.findAndHookMethod(
                Location.class,
                "isFromMockProvider",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (ConfigManager.isLocationSpoofEnabled()) {
                                param.setResult(false);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    XposedHelpers.findAndHookMethod(
                        Location.class,
                        "isMock",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                try {
                                    if (ConfigManager.isLocationSpoofEnabled()) {
                                        param.setResult(false);
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    );
                } catch (Throwable ignored) {
                }
            }

            // 3. LocationManager.getLastKnownLocation
            XposedHelpers.findAndHookMethod(
                LocationManager.class,
                "getLastKnownLocation",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isLocationSpoofEnabled()) {
                                return;
                            }
                            String provider = (String) param.args[0];
                            if (provider == null) {
                                provider = LocationManager.GPS_PROVIDER;
                            }
                            param.setResult(createSpoofedLocation(provider));
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            // 4. LocationManager.getLastLocation
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    XposedHelpers.findAndHookMethod(
                        LocationManager.class,
                        "getLastLocation",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                try {
                                    if (ConfigManager.isLocationSpoofEnabled()) {
                                        param.setResult(createSpoofedLocation(LocationManager.FUSED_PROVIDER));
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    );
                } catch (Throwable ignored) {
                }
            }

            // 5. LocationManager.getCurrentLocation (API 30+)
            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    XposedHelpers.findAndHookMethod(
                        LocationManager.class,
                        "getCurrentLocation",
                        String.class,
                        CancellationSignal.class,
                        Executor.class,
                        Consumer.class,
                        new XC_MethodHook() {
                            @Override
                            @SuppressWarnings("unchecked")
                            protected void beforeHookedMethod(MethodHookParam param) {
                                try {
                                    if (!ConfigManager.isLocationSpoofEnabled()) {
                                        return;
                                    }
                                    String provider = (String) param.args[0];
                                    Executor executor = (Executor) param.args[2];
                                    Consumer<Location> consumer = (Consumer<Location>) param.args[3];
                                    if (consumer != null) {
                                        Location loc = createSpoofedLocation(provider);
                                        if (executor != null) {
                                            executor.execute(() -> consumer.accept(loc));
                                        } else {
                                            consumer.accept(loc);
                                        }
                                        param.setResult(null);
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    );
                } catch (Throwable ignored) {
                }
            }

            AppLogger.debug(TAG, "LocationHooks loaded");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook LocationManager: " + throwable.getMessage(), throwable);
        }
    }

    private static Location createSpoofedLocation(String provider) {
        Location location = new Location(provider != null ? provider : LocationManager.GPS_PROVIDER);
        location.setLatitude(ConfigManager.getSpoofedLatitude());
        location.setLongitude(ConfigManager.getSpoofedLongitude());
        location.setAltitude(18.5);
        location.setAccuracy(3.5f);
        location.setSpeed(0.0f);
        location.setBearing(0.0f);
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        return location;
    }
}
