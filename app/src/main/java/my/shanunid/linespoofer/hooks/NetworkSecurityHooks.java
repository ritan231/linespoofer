package my.shanunid.linespoofer.hooks;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Proxy;

import java.net.NetworkInterface;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks to hide VPN, Proxy, and network tunneling from target applications.
 *
 * Conceals:
 * 1. NetworkCapabilities.TRANSPORT_VPN and asserts NET_CAPABILITY_NOT_VPN
 * 2. ConnectivityManager VPN network info
 * 3. System HTTP/HTTPS proxy properties and Android Proxy defaults
 * 4. Virtual network tunnel interfaces (tun*, tap*, ppp*)
 */
public class NetworkSecurityHooks {

    private static final String TAG = "LineSpoofer-Network";

    // Constants from Android Framework
    private static final int TRANSPORT_VPN = 4;
    private static final int NET_CAPABILITY_NOT_VPN = 15;
    private static final int TYPE_VPN = 17;

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        hookNetworkCapabilities(lpparam);
        hookConnectivityManager(lpparam);
        hookProxyDefaults(lpparam);
        hookNetworkInterfaces(lpparam);
        hookSystemProxyProperties();
    }

    /**
     * Masks VPN transport and asserts NOT_VPN capability in NetworkCapabilities.
     */
    private static void hookNetworkCapabilities(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> netCapsClass = XposedHelpers.findClassIfExists(
                "android.net.NetworkCapabilities", lpparam.classLoader);

            if (netCapsClass == null) {
                return;
            }

            // Hook hasTransport(int)
            XposedHelpers.findAndHookMethod(netCapsClass, "hasTransport", int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        int transport = (Integer) param.args[0];
                        if (transport == TRANSPORT_VPN) {
                            param.setResult(false);
                        }
                    }
                });

            // Hook hasCapability(int)
            XposedHelpers.findAndHookMethod(netCapsClass, "hasCapability", int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        int capability = (Integer) param.args[0];
                        if (capability == NET_CAPABILITY_NOT_VPN) {
                            param.setResult(true);
                        }
                    }
                });

        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook NetworkCapabilities: " + throwable.getMessage());
        }
    }

    /**
     * Intercepts ConnectivityManager methods checking for VPN network info.
     */
    private static void hookConnectivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> cmClass = XposedHelpers.findClassIfExists(
                "android.net.ConnectivityManager", lpparam.classLoader);

            if (cmClass == null) {
                return;
            }

            // Hook getNetworkInfo(int)
            XposedHelpers.findAndHookMethod(cmClass, "getNetworkInfo", int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        int netType = (Integer) param.args[0];
                        if (netType == TYPE_VPN) {
                            param.setResult(null);
                        }
                    }
                });

            // Hook getDefaultProxy()
            XposedHelpers.findAndHookMethod(cmClass, "getDefaultProxy",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult(null);
                    }
                });

        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook ConnectivityManager: " + throwable.getMessage());
        }
    }

    /**
     * Hides default Android proxy host and port.
     */
    private static void hookProxyDefaults(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> proxyClass = XposedHelpers.findClassIfExists(
                "android.net.Proxy", lpparam.classLoader);

            if (proxyClass != null) {
                XposedHelpers.findAndHookMethod(proxyClass, "getDefaultHost",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(null);
                        }
                    });

                XposedHelpers.findAndHookMethod(proxyClass, "getDefaultPort",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(-1);
                        }
                    });
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Hides virtual tunnel interfaces (tun0, tap0, ppp0) from NetworkInterface queries.
     */
    private static void hookNetworkInterfaces(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(NetworkInterface.class, "getName",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String name = (String) param.getResult();
                        if (name != null) {
                            String lower = name.toLowerCase();
                            if (lower.startsWith("tun") || lower.startsWith("tap") || lower.startsWith("ppp")) {
                                param.setResult("dummy0");
                            }
                        }
                    }
                });

            XposedHelpers.findAndHookMethod(NetworkInterface.class, "isPointToPoint",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        NetworkInterface ni = (NetworkInterface) param.thisObject;
                        if (ni != null && ni.getName() != null && ni.getName().startsWith("dummy")) {
                            param.setResult(false);
                        }
                    }
                });

        } catch (Throwable ignored) {
        }
    }

    /**
     * Intercepts Java System properties for HTTP/HTTPS proxy.
     */
    private static void hookSystemProxyProperties() {
        try {
            XposedHelpers.findAndHookMethod(System.class, "getProperty", String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[0];
                        if (isProxyProperty(key)) {
                            param.setResult(null);
                        }
                    }
                });

            XposedHelpers.findAndHookMethod(System.class, "getProperty", String.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[0];
                        if (isProxyProperty(key)) {
                            param.setResult(param.args[1]); // Return default value
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static boolean isProxyProperty(String key) {
        if (key == null) {
            return false;
        }
        return "http.proxyHost".equals(key)
            || "http.proxyPort".equals(key)
            || "https.proxyHost".equals(key)
            || "https.proxyPort".equals(key)
            || "socksProxyHost".equals(key)
            || "socksProxyPort".equals(key);
    }
}

