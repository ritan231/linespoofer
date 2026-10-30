package my.shanunid.linespoofer.hooks;

import my.shanunid.linespoofer.utils.ConfigManager;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks PackageManager system feature queries so the target profile can present as
 * a tablet or a phone. For SM-X900, telephony features must disappear.
 */
public class PackageManagerHooks {

    private static final String TAG = "LineSpoofer-PackageManager";

    private static final Set<String> EMULATOR_FEATURES = new HashSet<>(Arrays.asList(
        "android.hardware.sensor.emulator",
        "goldfish"
    ));

    private static final Set<String> TELEPHONY_FEATURES = new HashSet<>(Arrays.asList(
        "android.hardware.telephony",
        "android.hardware.telephony.gsm",
        "android.hardware.telephony.cdma",
        "android.hardware.telephony.ims",
        "android.hardware.telephony.euicc",
        "android.hardware.telephony.mbms"
    ));

    private static final Set<String> HIDDEN_PACKAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "org.lsposed.manager",
        "org.lsposed.lspd",
        "my.shanunid.linespoofer",
        "com.topjohnwu.magisk",
        "io.github.vvb2060.magisk",
        "me.bmax.apatch",
        "io.github.a13e300.ksu",
        "com.sukernel",
        "de.robv.android.xposed.installer"
    )));

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> appPackageManagerClass = XposedHelpers.findClassIfExists(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader
            );

            if (appPackageManagerClass != null) {
                hookHasSystemFeature(appPackageManagerClass);
                hookGetSystemAvailableFeatures(appPackageManagerClass);
                hookInstallerPackage(appPackageManagerClass, lpparam);
                hookPackageHiding(appPackageManagerClass);
            }
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook PackageManager: " + throwable.getMessage());
        }
    }

    private static void hookPackageHiding(Class<?> pmClass) {
        try {
            // Hook getPackageInfo(String, ...)
            XposedBridge.hookAllMethods(pmClass, "getPackageInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof String) {
                        String pkg = (String) param.args[0];
                        if (HIDDEN_PACKAGES.contains(pkg)) {
                            param.setThrowable(new android.content.pm.PackageManager.NameNotFoundException("Package not found: " + pkg));
                        }
                    }
                }
            });

            // Hook getApplicationInfo(String, ...)
            XposedBridge.hookAllMethods(pmClass, "getApplicationInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof String) {
                        String pkg = (String) param.args[0];
                        if (HIDDEN_PACKAGES.contains(pkg)) {
                            param.setThrowable(new android.content.pm.PackageManager.NameNotFoundException("Package not found: " + pkg));
                        }
                    }
                }
            });

            // Hook getInstalledPackages(...)
            XposedBridge.hookAllMethods(pmClass, "getInstalledPackages", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object result = param.getResult();
                    if (result instanceof List) {
                        List<?> list = (List<?>) result;
                        List<Object> filtered = new ArrayList<>();
                        for (Object item : list) {
                            if (item instanceof android.content.pm.PackageInfo) {
                                if (!HIDDEN_PACKAGES.contains(((android.content.pm.PackageInfo) item).packageName)) {
                                    filtered.add(item);
                                }
                            } else {
                                filtered.add(item);
                            }
                        }
                        param.setResult(filtered);
                    }
                }
            });

            // Hook getInstalledApplications(...)
            XposedBridge.hookAllMethods(pmClass, "getInstalledApplications", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object result = param.getResult();
                    if (result instanceof List) {
                        List<?> list = (List<?>) result;
                        List<Object> filtered = new ArrayList<>();
                        for (Object item : list) {
                            if (item instanceof android.content.pm.ApplicationInfo) {
                                if (!HIDDEN_PACKAGES.contains(((android.content.pm.ApplicationInfo) item).packageName)) {
                                    filtered.add(item);
                                }
                            } else {
                                filtered.add(item);
                            }
                        }
                        param.setResult(filtered);
                    }
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook package hiding: " + throwable.getMessage());
        }
    }

    private static void hookHasSystemFeature(Class<?> pmClass) {
        try {
            XposedHelpers.findAndHookMethod(pmClass, "hasSystemFeature",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String feature = (String) param.args[0];
                        if (feature == null) {
                            return;
                        }
                        Boolean override = overrideFeature(feature);
                        if (override != null) {
                            param.setResult(override);
                        }
                    }
                });
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook hasSystemFeature(String): " + throwable.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(pmClass, "hasSystemFeature",
                String.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String feature = (String) param.args[0];
                        if (feature == null) {
                            return;
                        }
                        Boolean override = overrideFeature(feature);
                        if (override != null) {
                            param.setResult(override);
                        }
                    }
                });
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook hasSystemFeature(String, int): " + throwable.getMessage());
        }
    }

    private static void hookGetSystemAvailableFeatures(Class<?> pmClass) {
        try {
            XposedHelpers.findAndHookMethod(pmClass, "getSystemAvailableFeatures",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object[] features = (Object[]) param.getResult();
                        if (features == null) {
                            return;
                        }

                        Class<?> featureInfoClass = features.getClass().getComponentType();
                        List<Object> filtered = new ArrayList<>();

                        for (Object feature : features) {
                            try {
                                String name = (String) XposedHelpers.getObjectField(feature, "name");
                                if (name == null) {
                                    filtered.add(feature);
                                    continue;
                                }

                                Boolean override = overrideFeature(name);
                                if (override == null || override) {
                                    filtered.add(feature);
                                }
                            } catch (Throwable ignored) {
                                filtered.add(feature);
                            }
                        }

                        Object typedArray = Array.newInstance(featureInfoClass, filtered.size());
                        for (int index = 0; index < filtered.size(); index++) {
                            Array.set(typedArray, index, filtered.get(index));
                        }
                        param.setResult(typedArray);
                    }
                });
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook getSystemAvailableFeatures(): " + throwable.getMessage());
        }
    }

    private static Boolean overrideFeature(String featureName) {
        String normalized = featureName.toLowerCase(Locale.US);

        for (String denied : EMULATOR_FEATURES) {
            if (normalized.contains(denied.toLowerCase(Locale.US))) {
                return false;
            }
        }

        if (TELEPHONY_FEATURES.contains(featureName)) {
            return ConfigManager.shouldExposeTelephony();
        }

        if (ConfigManager.isTabletProfile() && "android.hardware.type.pc".equals(featureName)) {
            return false;
        }

        return null;
    }

    private static void hookInstallerPackage(Class<?> pmClass, XC_LoadPackage.LoadPackageParam lpparam) {
        // 1. Hook getInstallerPackageName(String)
        try {
            XposedHelpers.findAndHookMethod(pmClass, "getInstallerPackageName", String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult("com.android.vending");
                    }
                });
        } catch (Throwable ignored) {
        }

        // 2. Hook InstallSourceInfo on Android 11+ (API 30+)
        try {
            Class<?> installSourceInfoClass = XposedHelpers.findClassIfExists(
                "android.content.pm.InstallSourceInfo", lpparam.classLoader);
            if (installSourceInfoClass != null) {
                XposedHelpers.findAndHookMethod(installSourceInfoClass, "getInstallingPackageName",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult("com.android.vending");
                        }
                    });
                XposedHelpers.findAndHookMethod(installSourceInfoClass, "getInitiatingPackageName",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult("com.android.vending");
                        }
                    });
            }
        } catch (Throwable ignored) {
        }
    }
}
