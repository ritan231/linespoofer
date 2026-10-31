package my.shanunid.linespoofer.hooks;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.SystemClock;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Spoofs GPS / FusedLocationProvider coordinates, location updates, and strips mock provider flags.
 * Masks Location.isFromMockProvider() to prevent anti-cheat and fraud engine detection.
 */
public final class LocationHooks {

    private static final String TAG = "LineSpoofer-Location";

    private LocationHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
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

            XposedHelpers.findAndHookMethod(
                Location.class,
                "getAltitude",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (ConfigManager.isLocationSpoofEnabled()) {
                                param.setResult(18.5);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                Location.class,
                "getAccuracy",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (ConfigManager.isLocationSpoofEnabled()) {
                                param.setResult(3.5f);
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

            // 4. LocationManager.getLastLocation (API 31+)
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

            // 6. Hook LocationListener.onLocationChanged across LocationManager.requestLocationUpdates
            XC_MethodHook requestLocationUpdatesHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        if (!ConfigManager.isLocationSpoofEnabled() || param.args == null) {
                            return;
                        }
                        for (Object arg : param.args) {
                            if (arg instanceof LocationListener) {
                                hookLocationListener((LocationListener) arg);
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            };

            for (Method m : LocationManager.class.getDeclaredMethods()) {
                if ("requestLocationUpdates".equals(m.getName())) {
                    try {
                        XposedHelpers.findAndHookMethod(LocationManager.class, "requestLocationUpdates", m.getParameterTypes(), requestLocationUpdatesHook);
                    } catch (Throwable ignored) {
                    }
                }
            }

            // 7. Google Play Services LocationResult hook
            try {
                Class<?> locationResultClass = XposedHelpers.findClassIfExists("com.google.android.gms.location.LocationResult", lpparam.classLoader);
                if (locationResultClass != null) {
                    XposedHelpers.findAndHookMethod(locationResultClass, "getLastLocation", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (ConfigManager.isLocationSpoofEnabled()) {
                                param.setResult(createSpoofedLocation("fused"));
                            }
                        }
                    });

                    XposedHelpers.findAndHookMethod(locationResultClass, "getLocations", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (ConfigManager.isLocationSpoofEnabled()) {
                                param.setResult(Collections.singletonList(createSpoofedLocation("fused")));
                            }
                        }
                    });
                }
            } catch (Throwable ignored) {
            }

            // 8. Geocoder hook to prevent reverse-lookup revealing original coordinates
            try {
                Class<?> geocoderClass = XposedHelpers.findClassIfExists("android.location.Geocoder", lpparam.classLoader);
                if (geocoderClass != null) {
                    XposedHelpers.findAndHookMethod(geocoderClass, "getFromLocation", double.class, double.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (ConfigManager.isLocationSpoofEnabled()) {
                                param.args[0] = ConfigManager.getSpoofedLatitude();
                                param.args[1] = ConfigManager.getSpoofedLongitude();
                            }
                        }
                    });
                }
            } catch (Throwable ignored) {
            }

            AppLogger.debug(TAG, "LocationHooks loaded");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook LocationManager: " + throwable.getMessage(), throwable);
        }
    }

    private static void hookLocationListener(LocationListener listener) {
        if (listener == null) return;
        try {
            XposedHelpers.findAndHookMethod(listener.getClass(), "onLocationChanged", Location.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (ConfigManager.isLocationSpoofEnabled() && param.args[0] instanceof Location) {
                        applySpoofedLocation((Location) param.args[0]);
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static void applySpoofedLocation(Location location) {
        if (location == null) return;
        location.setLatitude(ConfigManager.getSpoofedLatitude());
        location.setLongitude(ConfigManager.getSpoofedLongitude());
        location.setAltitude(18.5);
        location.setAccuracy(3.5f);
        location.setSpeed(0.0f);
        location.setBearing(0.0f);
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
    }

    private static Location createSpoofedLocation(String provider) {
        Location location = new Location(provider != null ? provider : LocationManager.GPS_PROVIDER);
        applySpoofedLocation(location);
        return location;
    }
}
