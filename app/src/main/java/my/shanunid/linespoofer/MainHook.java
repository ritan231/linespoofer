package my.shanunid.linespoofer;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;

import my.shanunid.linespoofer.hooks.AdvertisingIdHooks;
import my.shanunid.linespoofer.hooks.AppSetIdHooks;
import my.shanunid.linespoofer.hooks.AudioHooks;
import my.shanunid.linespoofer.hooks.BatteryHooks;
import my.shanunid.linespoofer.hooks.BluetoothHooks;
import my.shanunid.linespoofer.hooks.BuildHooks;
import my.shanunid.linespoofer.hooks.DisplayHooks;
import my.shanunid.linespoofer.hooks.EmulatorDetectionHooks;
import my.shanunid.linespoofer.hooks.GetPropHooks;
import my.shanunid.linespoofer.hooks.GpuHooks;
import my.shanunid.linespoofer.hooks.HardwareHooks;
import my.shanunid.linespoofer.hooks.JavaSystemPropertyHooks;
import my.shanunid.linespoofer.hooks.LocaleHooks;
import my.shanunid.linespoofer.hooks.LocationHooks;
import my.shanunid.linespoofer.hooks.MediaDrmHooks;
import my.shanunid.linespoofer.hooks.NetworkSecurityHooks;
import my.shanunid.linespoofer.hooks.PackageManagerHooks;
import my.shanunid.linespoofer.hooks.SensorHooks;
import my.shanunid.linespoofer.hooks.SettingsHooks;
import my.shanunid.linespoofer.hooks.SocketSecurityHooks;
import my.shanunid.linespoofer.hooks.StorageHooks;
import my.shanunid.linespoofer.hooks.SystemPropertiesHooks;
import my.shanunid.linespoofer.hooks.TelephonyHooks;
import my.shanunid.linespoofer.hooks.TimeHooks;
import my.shanunid.linespoofer.hooks.VendorSystemPropertiesHooks;
import my.shanunid.linespoofer.hooks.WebViewHooks;
import my.shanunid.linespoofer.hooks.WifiHooks;
import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Main entry point for LineSpoofer LSPosed module.
 *
 * STANDALONE MODULE - No Magisk dependency.
 *
 * This module hooks Android APIs to spoof device identifiers and properties
 * that some apps read directly from Java APIs or SystemProperties.
 */
public class MainHook extends XposedModule implements IXposedHookLoadPackage {

    private static final String TAG = "LineSpoofer";
    private static final String MODULE_PACKAGE = "my.shanunid.linespoofer";
    private static final int REAL_SDK_INT = Build.VERSION.SDK_INT;
    private static final Set<String> INITIALIZED_HOOK_PROCESSES = ConcurrentHashMap.newKeySet();
    private static final Set<String> CONFIG_RECEIVER_PROCESSES = ConcurrentHashMap.newKeySet();

    public MainHook() {
        super();
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        super.onPackageLoaded(param);
        String packageName = param.getPackageName();
        if (packageName == null) {
            return;
        }

        if (MODULE_PACKAGE.equals(packageName)) {
            try {
                Class<?> activityClass = param.getDefaultClassLoader().loadClass("my.shanunid.linespoofer.MainActivity");
                java.lang.reflect.Field field = activityClass.getDeclaredField("isHookActivated");
                field.setAccessible(true);
                field.setBoolean(null, true);
                AppLogger.info(TAG, "libxposed: isHookActivated set to true in MainActivity");
            } catch (Throwable t) {
                AppLogger.info(TAG, "libxposed: Failed to set isHookActivated: " + t.getMessage());
            }

            try {
                java.lang.reflect.Constructor<?> ctor = XC_LoadPackage.LoadPackageParam.class.getDeclaredConstructors()[0];
                ctor.setAccessible(true);
                XC_LoadPackage.LoadPackageParam lpparam = (XC_LoadPackage.LoadPackageParam) ctor.newInstance(new Object[]{null});
                lpparam.packageName = packageName;
                lpparam.processName = packageName;
                lpparam.classLoader = param.getDefaultClassLoader();
                lpparam.appInfo = param.getApplicationInfo();
                lpparam.isFirstApplication = param.isFirstPackage();
                hookActivationCheck(lpparam);
            } catch (Throwable ignored) {
            }
            return;
        }

        if ("android".equals(packageName)) {
            AppLogger.info(TAG, "libxposed: Hooking Android system framework (system_server)");
            try {
                SharedPreferences remotePrefs = getRemotePreferences("linespoofer_config");
                ConfigManager.initFromRemotePreferences("android", remotePrefs);
                HardwareHooks.hookSystemServer(param.getDefaultClassLoader());
                BuildHooks.reapply(param.getDefaultClassLoader(), "android");
            } catch (Throwable t) {
                AppLogger.logError(TAG, "Failed hooking system framework: " + t.getMessage(), t);
            }
            return;
        }

        int userId = android.os.Process.myUid() / 100000;
        String targetKey = userId > 0 ? (packageName + "#" + userId) : packageName;

        try {
            SharedPreferences remotePrefs = getRemotePreferences("linespoofer_config");
            if (remotePrefs != null && !remotePrefs.getBoolean("master_spoof_active", true)) {
                AppLogger.info(TAG, "libxposed: Master spoof switch is disabled. Skipping hooks for " + packageName);
                return;
            }
            ConfigManager.initFromRemotePreferences(targetKey, remotePrefs);
            if (!ConfigManager.hasAssignedProfile() && userId > 0) {
                ConfigManager.initFromRemotePreferences(packageName, remotePrefs);
            }
        } catch (Throwable throwable) {
            AppLogger.info(TAG, "libxposed RemotePreferences fallback for " + packageName + ": " + throwable.getMessage());
            ConfigManager.init(targetKey);
            if (!ConfigManager.hasAssignedProfile() && userId > 0) {
                ConfigManager.init(packageName);
            }
        }

        XC_LoadPackage.LoadPackageParam lpparam = null;
        try {
            java.lang.reflect.Constructor<?> ctor = XC_LoadPackage.LoadPackageParam.class.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            lpparam = (XC_LoadPackage.LoadPackageParam) ctor.newInstance(new Object[]{null});
        } catch (Throwable t) {
            AppLogger.info(TAG, "Failed to instantiate LoadPackageParam: " + t.getMessage());
        }
        if (lpparam == null) {
            return;
        }
        lpparam.packageName = packageName;
        lpparam.processName = packageName;
        lpparam.classLoader = param.getDefaultClassLoader();
        lpparam.appInfo = param.getApplicationInfo();
        lpparam.isFirstApplication = param.isFirstPackage();

        hookApplicationAttachForInitialization(lpparam);

        if (!ConfigManager.hasAssignedProfile()) {
            AppLogger.info(TAG, "libxposed: profile not yet loaded for " + packageName + "; waiting for Application.attach");
            return;
        }

        initializeHooks(lpparam);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        AppLogger.info(TAG, "Loading hooks for " + lpparam.packageName);

        if ("android".equals(lpparam.packageName)) {
            AppLogger.info(TAG, "Legacy Xposed: Hooking Android system framework");
            try {
                HardwareHooks.hookSystemServer(lpparam.classLoader);
                BuildHooks.reapply(lpparam.classLoader, "android");
            } catch (Throwable t) {
                AppLogger.logError(TAG, "Failed hooking system framework: " + t.getMessage(), t);
            }
            return;
        }

        if (MODULE_PACKAGE.equals(lpparam.packageName)) {
            hookActivationCheck(lpparam);
            return;
        }

        hookApplicationAttachForInitialization(lpparam);

        int userId = android.os.Process.myUid() / 100000;
        String targetKey = userId > 0 ? (lpparam.packageName + "#" + userId) : lpparam.packageName;

        try {
            ConfigManager.init(targetKey);
            if (!ConfigManager.hasAssignedProfile() && userId > 0) {
                ConfigManager.init(lpparam.packageName);
            }
            if (!ConfigManager.hasAssignedProfile()) {
                AppLogger.info(TAG, "Profile unavailable before Application.attach; deferring hooks for " + lpparam.packageName);
                return;
            }
            AppLogger.info(TAG, "Config initialized successfully for " + lpparam.packageName);
        } catch (Exception exception) {
            AppLogger.logError(TAG, "Failed to init config for " + lpparam.packageName + ": " + exception.getMessage(), exception);
            return;
        }

        initializeHooks(lpparam);
    }

    private void initializeHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        String processName = lpparam.processName == null ? lpparam.packageName : lpparam.processName;
        if (!INITIALIZED_HOOK_PROCESSES.add(processName)) {
            return;
        }

        AppLogger.info(TAG, "Initializing hooks for process " + processName);

        try {
            SystemPropertiesHooks.hook(lpparam);
            AppLogger.debug(TAG, "SystemPropertiesHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "SystemPropertiesHooks failed: " + exception.getMessage(), exception);
        }

        try {
            VendorSystemPropertiesHooks.hook(lpparam);
            AppLogger.debug(TAG, "VendorSystemPropertiesHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "VendorSystemPropertiesHooks failed: " + exception.getMessage(), exception);
        }

        try {
            JavaSystemPropertyHooks.hook(lpparam);
            AppLogger.debug(TAG, "JavaSystemPropertyHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "JavaSystemPropertyHooks failed: " + exception.getMessage(), exception);
        }

        try {
            GetPropHooks.hook(lpparam);
            AppLogger.debug(TAG, "GetPropHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "GetPropHooks failed: " + exception.getMessage(), exception);
        }

        try {
            BuildHooks.hook(lpparam);
            AppLogger.debug(TAG, "BuildHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "BuildHooks failed: " + exception.getMessage(), exception);
        }

        hookActivityLifecycleForReapply(lpparam);

        try {
            LocaleHooks.hook(lpparam);
            AppLogger.debug(TAG, "LocaleHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "LocaleHooks failed: " + exception.getMessage(), exception);
        }

        try {
            HardwareHooks.hook(lpparam);
            AppLogger.debug(TAG, "HardwareHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "HardwareHooks failed: " + exception.getMessage(), exception);
        }

        try {
            DisplayHooks.hook(lpparam);
            AppLogger.debug(TAG, "DisplayHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "DisplayHooks failed: " + exception.getMessage(), exception);
        }

        try {
            EmulatorDetectionHooks.hook(lpparam);
            AppLogger.debug(TAG, "EmulatorDetectionHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "EmulatorDetectionHooks failed: " + exception.getMessage(), exception);
        }

        try {
            TelephonyHooks.hook(lpparam);
            AppLogger.debug(TAG, "TelephonyHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "TelephonyHooks failed: " + exception.getMessage(), exception);
        }

        try {
            SettingsHooks.hook(lpparam);
            AppLogger.debug(TAG, "SettingsHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "SettingsHooks failed: " + exception.getMessage(), exception);
        }

        try {
            NetworkSecurityHooks.hook(lpparam);
            AppLogger.debug(TAG, "NetworkSecurityHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "NetworkSecurityHooks failed: " + exception.getMessage(), exception);
        }

        try {
            SocketSecurityHooks.hook(lpparam);
            AppLogger.debug(TAG, "SocketSecurityHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "SocketSecurityHooks failed: " + exception.getMessage(), exception);
        }

        try {
            AdvertisingIdHooks.hook(lpparam);
            AppLogger.debug(TAG, "AdvertisingIdHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "AdvertisingIdHooks failed: " + exception.getMessage(), exception);
        }

        if (REAL_SDK_INT >= 30) {
            try {
                AppSetIdHooks.hook(lpparam);
                AppLogger.debug(TAG, "AppSetIdHooks loaded");
            } catch (Exception exception) {
                AppLogger.logError(TAG, "AppSetIdHooks failed: " + exception.getMessage(), exception);
            }
        }

        try {
            MediaDrmHooks.hook(lpparam);
            AppLogger.debug(TAG, "MediaDrmHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "MediaDrmHooks failed: " + exception.getMessage(), exception);
        }

        try {
            WebViewHooks.hook(lpparam);
            AppLogger.debug(TAG, "WebViewHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "WebViewHooks failed: " + exception.getMessage(), exception);
        }

        try {
            SensorHooks.hook(lpparam);
            AppLogger.debug(TAG, "SensorHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "SensorHooks failed: " + exception.getMessage(), exception);
        }

        try {
            PackageManagerHooks.hook(lpparam);
            AppLogger.debug(TAG, "PackageManagerHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "PackageManagerHooks failed: " + exception.getMessage(), exception);
        }

        try {
            BatteryHooks.hook(lpparam);
            AppLogger.debug(TAG, "BatteryHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "BatteryHooks failed: " + exception.getMessage(), exception);
        }

        try {
            TimeHooks.hook(lpparam);
            AppLogger.debug(TAG, "TimeHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "TimeHooks failed: " + exception.getMessage(), exception);
        }

        try {
            StorageHooks.hook(lpparam);
            AppLogger.debug(TAG, "StorageHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "StorageHooks failed: " + exception.getMessage(), exception);
        }

        try {
            AudioHooks.hook(lpparam);
            AppLogger.debug(TAG, "AudioHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "AudioHooks failed: " + exception.getMessage(), exception);
        }

        try {
            GpuHooks.hook(lpparam);
            AppLogger.debug(TAG, "GpuHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "GpuHooks failed: " + exception.getMessage(), exception);
        }

        try {
            BluetoothHooks.hook(lpparam);
            AppLogger.debug(TAG, "BluetoothHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "BluetoothHooks failed: " + exception.getMessage(), exception);
        }

        try {
            LocationHooks.hook(lpparam);
            AppLogger.debug(TAG, "LocationHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "LocationHooks failed: " + exception.getMessage(), exception);
        }

        try {
            WifiHooks.hook(lpparam);
            AppLogger.debug(TAG, "WifiHooks loaded");
        } catch (Exception exception) {
            AppLogger.logError(TAG, "WifiHooks failed: " + exception.getMessage(), exception);
        }

        AppLogger.info(TAG, "All hooks initialized for " + lpparam.packageName);
    }

    private void hookApplicationAttachForInitialization(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Application",
                null,
                "attach",
                android.content.Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            Context context = (Context) param.args[0];
                            ConfigManager.forceReload(context);
                            registerConfigChangeReceiver(context, lpparam);
                            if (!ConfigManager.hasAssignedProfile()) {
                                AppLogger.info(TAG, "No profile assigned after Application.attach for " + lpparam.packageName);
                                return;
                            }
                            initializeHooks(lpparam);
                            BuildHooks.reapply(context.getClassLoader(), lpparam.packageName);
                            AppLogger.info(TAG, "Profile loaded after Application.attach for " + lpparam.packageName);
                        } catch (Throwable throwable) {
                            AppLogger.logError(TAG, "Failed to initialize hooks after attach for " + lpparam.packageName, throwable);
                        }
                    }
                }
            );
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook Application.attach initialization path: " + throwable.getMessage(), throwable);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerConfigChangeReceiver(Context context, XC_LoadPackage.LoadPackageParam lpparam) {
        String processName = lpparam.processName == null ? lpparam.packageName : lpparam.processName;
        if (!CONFIG_RECEIVER_PROCESSES.add(processName)) {
            return;
        }

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                if (intent == null || !ConfigManager.ACTION_CONFIG_CHANGED.equals(intent.getAction())) {
                    return;
                }
                ConfigManager.forceReload(receiverContext);
                if (!ConfigManager.hasAssignedProfile()) {
                    return;
                }
                initializeHooks(lpparam);
                BuildHooks.reapply(receiverContext.getClassLoader(), lpparam.packageName);
            }
        };

        IntentFilter filter = new IntentFilter(ConfigManager.ACTION_CONFIG_CHANGED);
        if (REAL_SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
    }

    private void hookActivityLifecycleForReapply(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Activity",
                null,
                "onCreate",
                android.os.Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isUsingEmbeddedDefaults()) {
                                return;
                            }
                            android.app.Activity activity = (android.app.Activity) param.thisObject;
                            ConfigManager.forceReload(activity);
                            BuildHooks.reapply(activity.getClassLoader(), lpparam.packageName);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );
        } catch (Throwable ignored) {
        }
    }

    private void hookActivationCheck(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                MainActivity.class.getName(),
                lpparam.classLoader,
                "isModuleActivated",
                XC_MethodReplacement.returnConstant(true)
            );
            AppLogger.info(TAG, "Activation check hooked for companion app");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook activation check: " + throwable.getMessage(), throwable);
        }
    }
}
