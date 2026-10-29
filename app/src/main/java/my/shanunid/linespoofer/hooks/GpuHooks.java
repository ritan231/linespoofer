package my.shanunid.linespoofer.hooks;

import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLES30;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Spoofs OpenGL ES vendor, renderer, and version strings.
 * Risk-Gated: Disabled by default to protect game shader compilers against incompatible shader cache bugs.
 */
public final class GpuHooks {

    private static final String TAG = "LineSpoofer-GPU";

    private static final int GL_VENDOR = 0x1F00;
    private static final int GL_RENDERER = 0x1F01;
    private static final int GL_VERSION = 0x1F02;

    private GpuHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!ConfigManager.isGpuSpoofEnabled()) {
            return;
        }

        hookGLESClass(GLES20.class);
        hookGLESClass(GLES30.class);
        hookGLESClass(GLES10.class);

        try {
            Class<?> gl10Class = XposedHelpers.findClassIfExists("javax.microedition.khronos.opengles.GL10", lpparam.classLoader);
            if (gl10Class != null) {
                XposedHelpers.findAndHookMethod(
                    gl10Class,
                    "glGetString",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                if (!ConfigManager.isGpuSpoofEnabled()) {
                                    return;
                                }
                                int name = (int) param.args[0];
                                String spoofed = resolveGlString(name);
                                if (spoofed != null) {
                                    param.setResult(spoofed);
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                );
            }
            AppLogger.debug(TAG, "GpuHooks loaded");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook GL10: " + throwable.getMessage(), throwable);
        }
    }

    private static void hookGLESClass(Class<?> glesClass) {
        try {
            XposedHelpers.findAndHookMethod(
                glesClass,
                "glGetString",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isGpuSpoofEnabled()) {
                                return;
                            }
                            int name = (int) param.args[0];
                            String spoofed = resolveGlString(name);
                            if (spoofed != null) {
                                param.setResult(spoofed);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );
        } catch (Throwable ignored) {
        }
    }

    private static String resolveGlString(int name) {
        switch (name) {
            case GL_RENDERER:
                return ConfigManager.getGpuRenderer();
            case GL_VENDOR:
                return ConfigManager.getGpuVendor();
            case GL_VERSION:
                return "OpenGL ES 3.2 V@0615.0 (GIT@8575003, I56965158ca, 1718873105) (Date:06/20/24)";
            default:
                return null;
        }
    }
}

