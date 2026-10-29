package my.shanunid.linespoofer.hooks;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks hardware motion and environmental sensors.
 *
 * 1. Masks emulator/generic sensor vendor and model strings to standard tier-1 hardware (Bosch, STMicroelectronics).
 * 2. Injects natural micro-jitter to Accelerometer and Gyroscope streams to defeat flat-surface / bot detectors.
 * 3. Populates plausible gravitational vectors (~9.80665 m/s^2) if sensor readings are dead flat (0.0).
 */
public class SensorHooks {

    private static final String TAG = "LineSpoofer-Sensors";
    private static final Set<Class<?>> HOOKED_LISTENER_CLASSES = ConcurrentHashMap.newKeySet();

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookSensorProperties(lpparam);
            hookSensorManagerRegistration(lpparam);
            AppLogger.info(TAG, "SensorHooks registered successfully for " + lpparam.packageName);
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to register SensorHooks: " + throwable.getMessage(), throwable);
        }
    }

    private static void hookSensorProperties(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                Sensor.class,
                "getVendor",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!ConfigManager.isSensorsSpoofEnabled()) {
                            return;
                        }
                        String vendor = (String) param.getResult();
                        if (vendor == null || isSuspiciousVendor(vendor)) {
                            param.setResult("Bosch Sensortec");
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                Sensor.class,
                "getName",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!ConfigManager.isSensorsSpoofEnabled()) {
                            return;
                        }
                        String name = (String) param.getResult();
                        if (name == null || isSuspiciousSensorName(name)) {
                            Sensor sensor = (Sensor) param.thisObject;
                            param.setResult(resolveRealisticName(sensor.getType()));
                        }
                    }
                }
            );
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook Sensor properties: " + throwable.getMessage(), throwable);
        }
    }

    private static void hookSensorManagerRegistration(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> sensorManagerClass = XposedHelpers.findClassIfExists("android.hardware.SensorManager", lpparam.classLoader);
        if (sensorManagerClass == null) {
            sensorManagerClass = SensorManager.class;
        }

        try {
            for (Method method : sensorManagerClass.getDeclaredMethods()) {
                if ("registerListener".equals(method.getName())) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length >= 2 && SensorEventListener.class.isAssignableFrom(paramTypes[0])) {
                        try {
                            XposedBridge.hookMethod(method, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) {
                                    if (!ConfigManager.isSensorsSpoofEnabled()) {
                                        return;
                                    }
                                    Object listener = param.args[0];
                                    if (listener != null) {
                                        hookListenerClass(listener.getClass());
                                    }
                                }
                            });
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook SensorManager.registerListener: " + throwable.getMessage(), throwable);
        }
    }

    private static void hookListenerClass(Class<?> listenerClass) {
        if (listenerClass == null || !HOOKED_LISTENER_CLASSES.add(listenerClass)) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                listenerClass,
                "onSensorChanged",
                SensorEvent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!ConfigManager.isSensorsSpoofEnabled()) {
                            return;
                        }
                        SensorEvent event = (SensorEvent) param.args[0];
                        if (event == null || event.values == null || event.sensor == null) {
                            return;
                        }
                        applySensorJitter(event);
                    }
                }
            );
        } catch (Throwable ignored) {
            // Listener might implement onSensorChanged in superclass
        }
    }

    private static void applySensorJitter(SensorEvent event) {
        int sensorType = event.sensor.getType();
        float[] values = event.values;

        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            if (values.length >= 3) {
                long time = System.currentTimeMillis();
                // Natural micro-tremor (~0.015 - 0.035 m/s^2)
                float jitterX = (float) (Math.sin(time * 0.003) * 0.03 + (Math.random() - 0.5) * 0.02);
                float jitterY = (float) (Math.cos(time * 0.003) * 0.03 + (Math.random() - 0.5) * 0.02);
                float jitterZ = (float) ((Math.random() - 0.5) * 0.03);

                // If sensor is completely uninitialized / zeroed (flat emulator artifact)
                if (Math.abs(values[0]) < 0.0001f && Math.abs(values[1]) < 0.0001f && Math.abs(values[2]) < 0.0001f) {
                    values[0] = jitterX;
                    values[1] = jitterY;
                    values[2] = 9.80665f + jitterZ;
                } else {
                    values[0] += jitterX;
                    values[1] += jitterY;
                    values[2] += jitterZ;
                }
            }
        } else if (sensorType == Sensor.TYPE_GYROSCOPE) {
            if (values.length >= 3) {
                long time = System.currentTimeMillis();
                // Micro angular velocity variations (~0.003 - 0.007 rad/s)
                float jitterX = (float) (Math.sin(time * 0.004) * 0.006 + (Math.random() - 0.5) * 0.004);
                float jitterY = (float) (Math.cos(time * 0.004) * 0.006 + (Math.random() - 0.5) * 0.004);
                float jitterZ = (float) ((Math.random() - 0.5) * 0.004);

                values[0] += jitterX;
                values[1] += jitterY;
                values[2] += jitterZ;
            }
        } else if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
            if (values.length >= 3) {
                // Earth's geomagnetic field (~40-50 microteslas) if dead
                if (Math.abs(values[0]) < 0.0001f && Math.abs(values[1]) < 0.0001f && Math.abs(values[2]) < 0.0001f) {
                    values[0] = 18.5f + (float) ((Math.random() - 0.5) * 0.2);
                    values[1] = -5.2f + (float) ((Math.random() - 0.5) * 0.2);
                    values[2] = 42.1f + (float) ((Math.random() - 0.5) * 0.2);
                }
            }
        }
    }

    private static boolean isSuspiciousVendor(String vendor) {
        String lower = vendor.toLowerCase(Locale.US);
        return lower.contains("goldfish")
            || lower.contains("ranchu")
            || lower.contains("qemu")
            || lower.contains("google inc.")
            || lower.contains("android open source project")
            || lower.contains("aosp");
    }

    private static boolean isSuspiciousSensorName(String name) {
        String lower = name.toLowerCase(Locale.US);
        return lower.contains("goldfish")
            || lower.contains("ranchu")
            || lower.contains("qemu")
            || lower.contains("emulator")
            || lower.contains("dummy")
            || lower.contains("mock");
    }

    private static String resolveRealisticName(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                return "BMI260 Accelerometer";
            case Sensor.TYPE_GYROSCOPE:
                return "BMI260 Gyroscope";
            case Sensor.TYPE_MAGNETIC_FIELD:
                return "BMM150 Magnetometer";
            case Sensor.TYPE_PRESSURE:
                return "BMP380 Barometer";
            case Sensor.TYPE_LIGHT:
                return "TCS3701 Ambient Light Sensor";
            case Sensor.TYPE_PROXIMITY:
                return "TCS3701 Proximity Sensor";
            case Sensor.TYPE_GRAVITY:
                return "Gravity Sensor (Fused)";
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return "Linear Acceleration Sensor";
            case Sensor.TYPE_ROTATION_VECTOR:
                return "Rotation Vector Sensor";
            default:
                return "Bosch Sensor";
        }
    }
}
