package my.shanunid.linespoofer.hooks;

import my.shanunid.linespoofer.utils.ConfigManager;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks to block target applications from scanning localhost / LAN probe ports.
 *
 * Targets:
 * - Frida default port: 27042 and scan range (20000-40000)
 * - Common debugger ports: 1234 (GDB), 5039 (LLDB), 23946 (IDA)
 * - ADB over Wi-Fi / localhost forwarding: 5555, 5554
 * - Common local proxy ports: 8080, 8888, 8081, 9090
 *
 * When the toggle is enabled, attempts to connect to these localhost ports throw
 * java.net.ConnectException immediately (simulating port closed / connection refused).
 */
public class SocketSecurityHooks {

    private static final String TAG = "LineSpoofer-Socket";

    private static final Set<Integer> PROBE_PORTS = new HashSet<>(Arrays.asList(
        27042, // Frida default
        1234,  // GDB server
        5039,  // LLDB server
        23946, // IDA Pro android_server
        23947,
        5555,  // ADB over network
        5554,
        8080,  // Proxies
        8888,
        8081,
        9090
    ));

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        hookSocketConnect();
        hookInetAddressReachable();
    }

    private static void hookSocketConnect() {
        try {
            // Hook Socket.connect(SocketAddress, int)
            XposedHelpers.findAndHookMethod(Socket.class, "connect",
                SocketAddress.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!ConfigManager.isBlockLocalSocketEnabled()) {
                            return;
                        }

                        SocketAddress endpoint = (SocketAddress) param.args[0];
                        if (endpoint instanceof InetSocketAddress) {
                            InetSocketAddress isa = (InetSocketAddress) endpoint;
                            if (isBlockedLocalEndpoint(isa)) {
                                XposedBridge.log(TAG + ": Blocked localhost probe to port " + isa.getPort());
                                throw new ConnectException("Connection refused");
                            }
                        }
                    }
                });

            // Hook Socket.connect(SocketAddress)
            XposedHelpers.findAndHookMethod(Socket.class, "connect",
                SocketAddress.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!ConfigManager.isBlockLocalSocketEnabled()) {
                            return;
                        }

                        SocketAddress endpoint = (SocketAddress) param.args[0];
                        if (endpoint instanceof InetSocketAddress) {
                            InetSocketAddress isa = (InetSocketAddress) endpoint;
                            if (isBlockedLocalEndpoint(isa)) {
                                XposedBridge.log(TAG + ": Blocked localhost probe to port " + isa.getPort());
                                throw new ConnectException("Connection refused");
                            }
                        }
                    }
                });

        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook Socket.connect(): " + throwable.getMessage());
        }
    }

    private static void hookInetAddressReachable() {
        try {
            XposedHelpers.findAndHookMethod(InetAddress.class, "isReachable", int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!ConfigManager.isBlockLocalSocketEnabled()) {
                            return;
                        }
                        InetAddress addr = (InetAddress) param.thisObject;
                        if (addr != null && addr.isLoopbackAddress()) {
                            param.setResult(false);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static boolean isBlockedLocalEndpoint(InetSocketAddress isa) {
        if (isa == null) {
            return false;
        }

        InetAddress addr = isa.getAddress();
        String host = isa.getHostString();
        int port = isa.getPort();

        boolean isLocal = (addr != null && addr.isLoopbackAddress())
            || "127.0.0.1".equals(host)
            || "localhost".equalsIgnoreCase(host)
            || "::1".equals(host);

        if (!isLocal) {
            return false;
        }

        // Check if port matches known reverse engineering or scanner ports
        if (PROBE_PORTS.contains(port)) {
            return true;
        }

        // Frida dynamic port scan range (20000 - 40000)
        return port >= 20000 && port <= 40000;
    }
}
