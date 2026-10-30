package my.shanunid.linespoofer.hooks;

import android.app.ActivityManager;
import android.os.Debug;

import my.shanunid.linespoofer.utils.ConfigManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks to spoof hardware specifications (CPU cores, RAM, heap size, /proc/cpuinfo, /proc/meminfo)
 * to match the configured device profile dynamically.
 */
public class HardwareHooks {

    private static final String TAG = "LineSpoofer-Hardware";

    // Track file reader instances opening /proc/cpuinfo or /proc/meminfo
    private static final Map<Object, String[]> VIRTUAL_FILE_LINES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Integer> VIRTUAL_FILE_OFFSETS = Collections.synchronizedMap(new WeakHashMap<>());

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookRuntimeCores();
            hookActivityManagerMemory(lpparam);
            hookDebugMemory();
            hookFileReads();
            hookFileInputStream(lpparam);
            XposedBridge.log(TAG + ": Successfully hooked hardware specs");
        } catch (Throwable e) {
            XposedBridge.log(TAG + ": Failed to hook hardware: " + e.getMessage());
        }
    }

    /**
     * Hook Runtime.availableProcessors() to return the configured CPU core count.
     */
    private static void hookRuntimeCores() {
        try {
            XposedHelpers.findAndHookMethod(Runtime.class, "availableProcessors",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(ConfigManager.getCpuCoreCount());
                    }
                });
        } catch (Throwable e) {
            XposedBridge.log(TAG + ": Failed to hook Runtime.availableProcessors(): " + e.getMessage());
        }
    }

    /**
     * Hook ActivityManager memory info methods with accurate proportional calculation.
     */
    private static void hookActivityManagerMemory(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClassIfExists(
                "android.app.ActivityManager", lpparam.classLoader);

            if (activityManagerClass == null) {
                return;
            }

            // Hook getMemoryInfo()
            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryInfo",
                ActivityManager.MemoryInfo.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ActivityManager.MemoryInfo memInfo = (ActivityManager.MemoryInfo) param.args[0];
                        if (memInfo != null) {
                            long targetTotal = ConfigManager.getTotalRamBytes();
                            long originalTotal = memInfo.totalMem;

                            // Calculate available memory ratio before modifying totalMem
                            if (originalTotal > 0) {
                                double availRatio = (double) memInfo.availMem / (double) originalTotal;
                                if (availRatio < 0.0) availRatio = 0.0;
                                if (availRatio > 1.0) availRatio = 1.0;
                                memInfo.availMem = (long) (targetTotal * availRatio);
                            }

                            // Overwrite totalMem after calculating proportional availMem
                            memInfo.totalMem = targetTotal;
                        }
                    }
                });

            // Hook getMemoryClass() - returns heap size in MB
            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        long totalMb = ConfigManager.getTotalRamBytes() / (1024L * 1024L);
                        int heapMb = totalMb >= 8192 ? 512 : (totalMb >= 4096 ? 256 : 192);
                        param.setResult(heapMb);
                    }
                });

            // Hook getLargeMemoryClass()
            XposedHelpers.findAndHookMethod(activityManagerClass, "getLargeMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        long totalMb = ConfigManager.getTotalRamBytes() / (1024L * 1024L);
                        int largeHeapMb = totalMb >= 8192 ? 1024 : (totalMb >= 4096 ? 512 : 384);
                        param.setResult(largeHeapMb);
                    }
                });

        } catch (Throwable e) {
            XposedBridge.log(TAG + ": Failed to hook ActivityManager memory: " + e.getMessage());
        }
    }

    /**
     * Hook Debug.getNativeHeapSize() and related memory methods
     */
    private static void hookDebugMemory() {
        try {
            XposedHelpers.findAndHookMethod(Debug.class, "getNativeHeapSize",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object result = param.getResult();
                        if (result instanceof Long) {
                            long originalSize = (Long) result;
                            long totalBytes = ConfigManager.getTotalRamBytes();
                            long standardRef = 12L * 1024 * 1024 * 1024L;
                            double scale = Math.max(1.0, (double) totalBytes / (standardRef / 4.0));
                            param.setResult((long) (originalSize * scale));
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    /**
     * Hook file reads to intercept /proc/cpuinfo and /proc/meminfo
     */
    private static void hookFileReads() {
        // 1. Hook FileReader(String) and FileReader(File)
        try {
            XposedHelpers.findAndHookConstructor(FileReader.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String path = (String) param.args[0];
                        bindVirtualFile(param.thisObject, path);
                    }
                });

            XposedHelpers.findAndHookConstructor(FileReader.class, File.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        File file = (File) param.args[0];
                        if (file != null) {
                            bindVirtualFile(param.thisObject, file.getAbsolutePath());
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        // 2. Hook BufferedReader constructor to propagate virtual file binding
        try {
            XposedHelpers.findAndHookConstructor(BufferedReader.class, Reader.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Reader inner = (Reader) param.args[0];
                        if (inner != null && VIRTUAL_FILE_LINES.containsKey(inner)) {
                            VIRTUAL_FILE_LINES.put(param.thisObject, VIRTUAL_FILE_LINES.get(inner));
                            VIRTUAL_FILE_OFFSETS.put(param.thisObject, 0);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        // 3. Hook BufferedReader.readLine()
        try {
            XposedHelpers.findAndHookMethod(BufferedReader.class, "readLine",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String[] lines = VIRTUAL_FILE_LINES.get(param.thisObject);
                        if (lines != null) {
                            Integer offset = VIRTUAL_FILE_OFFSETS.get(param.thisObject);
                            int cur = offset != null ? offset : 0;
                            if (cur < lines.length) {
                                VIRTUAL_FILE_OFFSETS.put(param.thisObject, cur + 1);
                                param.setResult(lines[cur]);
                            } else {
                                param.setResult(null); // EOF
                            }
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        // 4. Hook RandomAccessFile constructors & readLine()
        try {
            XposedHelpers.findAndHookConstructor(RandomAccessFile.class, String.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String path = (String) param.args[0];
                        bindVirtualFile(param.thisObject, path);
                    }
                });

            XposedHelpers.findAndHookConstructor(RandomAccessFile.class, File.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        File file = (File) param.args[0];
                        if (file != null) {
                            bindVirtualFile(param.thisObject, file.getAbsolutePath());
                        }
                    }
                });

            XposedHelpers.findAndHookMethod(RandomAccessFile.class, "readLine",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String[] lines = VIRTUAL_FILE_LINES.get(param.thisObject);
                        if (lines != null) {
                            Integer offset = VIRTUAL_FILE_OFFSETS.get(param.thisObject);
                            int cur = offset != null ? offset : 0;
                            if (cur < lines.length) {
                                VIRTUAL_FILE_OFFSETS.put(param.thisObject, cur + 1);
                                param.setResult(lines[cur]);
                            } else {
                                param.setResult(null);
                            }
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static void hookFileInputStream(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookConstructor(FileInputStream.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String path = (String) param.args[0];
                    File fake = getFakeFileForPath(path, lpparam.packageName);
                    if (fake != null) {
                        param.args[0] = fake.getAbsolutePath();
                    }
                }
            });

            XposedHelpers.findAndHookConstructor(FileInputStream.class, File.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    File file = (File) param.args[0];
                    if (file != null) {
                        File fake = getFakeFileForPath(file.getAbsolutePath(), lpparam.packageName);
                        if (fake != null) {
                            param.args[0] = fake;
                        }
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static File getFakeFileForPath(String path, String packageName) {
        if (path == null) {
            return null;
        }
        if (path.equals("/proc/cpuinfo")) {
            return createCachedVirtualFile("fake_cpuinfo", generateCpuInfo(), packageName);
        }
        if (path.equals("/proc/meminfo")) {
            return createCachedVirtualFile("fake_meminfo", generateMemInfo(), packageName);
        }
        if (path.equals("/proc/uptime")) {
            long baseSec = TimeHooks.isUptimeSpoofEnabled() ? (TimeHooks.getFakeBaseMs() / 1000L) : (ConfigManager.isUptimeSpoofEnabled() ? ConfigManager.getSpoofedUptimeSeconds() : 0L);
            long realElapsedSec = android.os.SystemClock.elapsedRealtime() / 1000L;
            long uptime = (realElapsedSec >= baseSec && baseSec > 0) ? realElapsedSec : (baseSec + realElapsedSec);
            long idle = uptime * 4;
            String content = uptime + ".50 " + idle + ".20\n";
            return createCachedVirtualFile("fake_uptime", content, packageName);
        }
        if (path.startsWith("/sys/devices/system/cpu/present")
            || path.startsWith("/sys/devices/system/cpu/possible")
            || path.startsWith("/sys/devices/system/cpu/online")) {
            int cores = Math.max(1, ConfigManager.getCpuCoreCount());
            return createCachedVirtualFile("fake_cpu_range", "0-" + (cores - 1) + "\n", packageName);
        }
        if (path.endsWith("build.prop") || path.equals("/default.prop")) {
            return createCachedVirtualFile("fake_build_prop", generateBuildProp(), packageName);
        }
        return null;
    }

    private static File createCachedVirtualFile(String prefix, String content, String packageName) {
        try {
            File cacheDir = null;
            try {
                Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
                Object app = activityThreadClass.getMethod("currentApplication").invoke(null);
                if (app instanceof android.content.Context) {
                    cacheDir = ((android.content.Context) app).getCacheDir();
                }
            } catch (Throwable ignored) {
            }
            if (cacheDir == null) {
                int uid = android.os.Process.myUid();
                int userId = uid / 100000;
                String basePath = (userId > 0) ? ("/data/user/" + userId + "/" + packageName) : ("/data/data/" + packageName);
                cacheDir = new File(basePath, "cache");
            }
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            File target = new File(cacheDir, "." + prefix);
            try (FileOutputStream fos = new FileOutputStream(target, false)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
            }
            return target;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void bindVirtualFile(Object readerObj, String path) {
        if (path == null) {
            return;
        }
        if (path.equals("/proc/cpuinfo")) {
            String[] lines = generateCpuInfo().split("\n");
            VIRTUAL_FILE_LINES.put(readerObj, lines);
            VIRTUAL_FILE_OFFSETS.put(readerObj, 0);
        } else if (path.equals("/proc/meminfo")) {
            String[] lines = generateMemInfo().split("\n");
            VIRTUAL_FILE_LINES.put(readerObj, lines);
            VIRTUAL_FILE_OFFSETS.put(readerObj, 0);
        } else if (path.equals("/proc/uptime")) {
            long baseSec = TimeHooks.isUptimeSpoofEnabled() ? (TimeHooks.getFakeBaseMs() / 1000L) : (ConfigManager.isUptimeSpoofEnabled() ? ConfigManager.getSpoofedUptimeSeconds() : 0L);
            long realElapsedSec = android.os.SystemClock.elapsedRealtime() / 1000L;
            long uptime = (realElapsedSec >= baseSec && baseSec > 0) ? realElapsedSec : (baseSec + realElapsedSec);
            long idle = uptime * 4;
            String[] lines = new String[] { uptime + ".50 " + idle + ".20" };
            VIRTUAL_FILE_LINES.put(readerObj, lines);
            VIRTUAL_FILE_OFFSETS.put(readerObj, 0);
        } else if (path.endsWith("build.prop") || path.equals("/default.prop")) {
            String[] lines = generateBuildProp().split("\n");
            VIRTUAL_FILE_LINES.put(readerObj, lines);
            VIRTUAL_FILE_OFFSETS.put(readerObj, 0);
        }
    }

    /**
     * Generates a realistic /proc/cpuinfo matching the spoofed profile.
     */
    public static String generateCpuInfo() {
        int cores = ConfigManager.getCpuCoreCount();
        String hardware = ConfigManager.getBuildHardware();
        if (hardware == null || hardware.trim().isEmpty()) {
            hardware = "qcom";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cores; i++) {
            sb.append("processor\t: ").append(i).append("\n");
            sb.append("BogoMIPS\t: 38.40\n");
            sb.append("Features\t: fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop asimddp\n");
            sb.append("CPU implementer\t: 0x41\n");
            sb.append("CPU architecture: 8\n");
            sb.append("CPU variant\t: 0x1\n");
            sb.append("CPU part\t: 0xd05\n");
            sb.append("CPU revision\t: 0\n\n");
        }
        sb.append("Hardware\t: ").append(hardware).append("\n");
        sb.append("Revision\t: 0000\n");
        String serial = ConfigManager.getSerial();
        if (serial != null && !serial.trim().isEmpty()) {
            sb.append("Serial\t\t: ").append(serial).append("\n");
        }
        return sb.toString();
    }

    /**
     * Generates a realistic /proc/meminfo matching the spoofed profile.
     */
    public static String generateMemInfo() {
        long totalKb = ConfigManager.getTotalRamBytes() / 1024L;
        long freeKb = totalKb / 3L;
        long availKb = (long) (totalKb * 0.45);
        StringBuilder sb = new StringBuilder();
        sb.append("MemTotal:       ").append(totalKb).append(" kB\n");
        sb.append("MemFree:        ").append(freeKb).append(" kB\n");
        sb.append("MemAvailable:   ").append(availKb).append(" kB\n");
        sb.append("Buffers:          128000 kB\n");
        sb.append("Cached:          2048000 kB\n");
        sb.append("SwapCached:            0 kB\n");
        sb.append("Active:          3200000 kB\n");
        sb.append("Inactive:        1200000 kB\n");
        return sb.toString();
    }

    /**
     * Generates virtual build.prop content matching the active spoofed profile.
     */
    public static String generateBuildProp() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Autogenerated spoofed build.prop\n");
        sb.append("ro.product.model=").append(ConfigManager.getBuildModel()).append("\n");
        sb.append("ro.product.brand=").append(ConfigManager.getBuildBrand()).append("\n");
        sb.append("ro.product.manufacturer=").append(ConfigManager.getBuildManufacturer()).append("\n");
        sb.append("ro.product.device=").append(ConfigManager.getBuildDevice()).append("\n");
        sb.append("ro.product.board=").append(ConfigManager.getBuildBoard()).append("\n");
        sb.append("ro.hardware=").append(ConfigManager.getBuildHardware()).append("\n");
        sb.append("ro.board.platform=").append(ConfigManager.getSystemProperty("ro.board.platform", ConfigManager.getBuildHardware())).append("\n");
        sb.append("ro.soc.model=").append(ConfigManager.getSystemProperty("ro.soc.model", ConfigManager.getBuildHardware())).append("\n");
        sb.append("ro.build.id=").append(ConfigManager.getBuildId()).append("\n");
        sb.append("ro.build.display.id=").append(ConfigManager.getBuildDisplay()).append("\n");
        sb.append("ro.build.version.release=").append(ConfigManager.getBuildVersionRelease()).append("\n");
        sb.append("ro.build.version.sdk=").append(ConfigManager.getBuildVersionSdk()).append("\n");
        sb.append("ro.build.fingerprint=").append(ConfigManager.getBuildFingerprint()).append("\n");
        return sb.toString();
    }

    /**
     * Intercepts ActivityManagerService in system_server (Android system framework)
     * so all apps on the device receive the spoofed RAM specifications without needing individual scopes.
     */
    public static void hookSystemServer(ClassLoader classLoader) {
        try {
            Class<?> amsClass = XposedHelpers.findClassIfExists("com.android.server.am.ActivityManagerService", classLoader);
            if (amsClass != null) {
                XposedHelpers.findAndHookMethod(amsClass, "getMemoryInfo", ActivityManager.MemoryInfo.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ActivityManager.MemoryInfo memInfo = (ActivityManager.MemoryInfo) param.args[0];
                        if (memInfo != null) {
                            long targetTotal = ConfigManager.getTotalRamBytes();
                            if (targetTotal > 0) {
                                memInfo.totalMem = targetTotal;
                                memInfo.availMem = (long) (targetTotal * 0.48);
                                memInfo.lowMemory = false;
                            }
                        }
                    }
                });
                XposedBridge.log(TAG + ": Successfully hooked ActivityManagerService.getMemoryInfo in system_server");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook system_server AMS: " + t.getMessage());
        }
    }
}
