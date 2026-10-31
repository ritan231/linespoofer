package my.shanunid.linespoofer.hooks;

import android.os.StatFs;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Spoofs android.os.StatFs disk size and free storage blocks.
 * Aligns app-visible storage metrics with the target device profile (e.g. 256 GB / 512 GB).
 */
public final class StorageHooks {

    private static final String TAG = "LineSpoofer-Storage";
    private static final long BLOCK_SIZE = 4096L;

    private StorageHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                StatFs.class,
                "getBlockSizeLong",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (ConfigManager.isStorageSpoofEnabled()) {
                                param.setResult(BLOCK_SIZE);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                StatFs.class,
                "getBlockCountLong",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isStorageSpoofEnabled()) {
                                return;
                            }
                            long storageGb = ConfigManager.getStorageGb();
                            long totalBytes = storageGb * 1024L * 1024L * 1024L;
                            long totalBlocks = totalBytes / BLOCK_SIZE;
                            param.setResult(totalBlocks);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XC_MethodHook freeBlocksHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        if (!ConfigManager.isStorageSpoofEnabled()) {
                            return;
                        }
                        long storageGb = ConfigManager.getStorageGb();
                        long totalBytes = storageGb * 1024L * 1024L * 1024L;
                        long freeBytes = (long) (totalBytes * 0.62); // Plausible ~62% free storage
                        long freeBlocks = freeBytes / BLOCK_SIZE;
                        param.setResult(freeBlocks);
                    } catch (Throwable ignored) {
                    }
                }
            };

            XposedHelpers.findAndHookMethod(StatFs.class, "getFreeBlocksLong", freeBlocksHook);
            XposedHelpers.findAndHookMethod(StatFs.class, "getAvailableBlocksLong", freeBlocksHook);

            XposedHelpers.findAndHookMethod(
                StatFs.class,
                "getTotalBytes",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isStorageSpoofEnabled()) {
                                return;
                            }
                            long storageGb = ConfigManager.getStorageGb();
                            param.setResult(storageGb * 1024L * 1024L * 1024L);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );

            XC_MethodHook freeBytesHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        if (!ConfigManager.isStorageSpoofEnabled()) {
                            return;
                        }
                        long storageGb = ConfigManager.getStorageGb();
                        long totalBytes = storageGb * 1024L * 1024L * 1024L;
                        param.setResult((long) (totalBytes * 0.62));
                    } catch (Throwable ignored) {
                    }
                }
            };
            XposedHelpers.findAndHookMethod(StatFs.class, "getFreeBytes", freeBytesHook);
            XposedHelpers.findAndHookMethod(StatFs.class, "getAvailableBytes", freeBytesHook);

            AppLogger.debug(TAG, "StatFs storage hooks loaded");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook StatFs: " + throwable.getMessage(), throwable);
        }
    }
}

