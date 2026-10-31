package my.shanunid.linespoofer.hooks;

import my.shanunid.linespoofer.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks java.lang.System.getProperty for values that some apps use as device metadata.
 */
public final class JavaSystemPropertyHooks {

    private JavaSystemPropertyHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(System.class, "getProperty",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[0];
                        String spoofed = getSpoofedValue(key);
                        if (spoofed != null) {
                            param.setResult(spoofed);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(System.class, "getProperty",
                String.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[0];
                        String spoofed = getSpoofedValue(key);
                        if (spoofed != null) {
                            param.setResult(spoofed);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static String getSpoofedValue(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        if ("os.arch".equals(key)) {
            String abi = ConfigManager.getCpuAbi();
            if (abi == null) {
                return null;
            }
            if (abi.contains("arm64")) {
                return "aarch64";
            }
            if (abi.contains("armeabi")) {
                return "arm";
            }
            if (abi.contains("x86_64")) {
                return "x86_64";
            }
            if (abi.contains("x86")) {
                return "x86";
            }
            return abi;
        }
        if ("http.agent".equals(key)) {
            return ConfigManager.getWebViewUserAgent();
        }
        if ("user.timezone".equals(key)) {
            String tz = ConfigManager.getTimezone();
            if (tz != null && !tz.isEmpty()) return tz;
        }
        if ("user.language".equals(key)) {
            String tag = ConfigManager.getLocaleTag();
            if (tag != null && !tag.isEmpty()) {
                java.util.Locale loc = java.util.Locale.forLanguageTag(tag.replace('_', '-'));
                if (!loc.getLanguage().isEmpty()) return loc.getLanguage();
            }
        }
        if ("user.country".equals(key) || "user.region".equals(key)) {
            String tag = ConfigManager.getLocaleTag();
            if (tag != null && !tag.isEmpty()) {
                java.util.Locale loc = java.util.Locale.forLanguageTag(tag.replace('_', '-'));
                if (!loc.getCountry().isEmpty()) return loc.getCountry();
            }
        }
        if ("user.locale".equals(key)) {
            String tag = ConfigManager.getLocaleTag();
            if (tag != null && !tag.isEmpty()) return tag;
        }
        return null;
    }
}
