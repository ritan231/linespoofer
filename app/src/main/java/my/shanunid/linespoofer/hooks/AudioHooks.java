package my.shanunid.linespoofer.hooks;

import android.media.AudioManager;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Spoofs AudioManager HAL properties (sample rate and buffer size).
 * Prevents emulator audio HAL fingerprinting from exposing test environments.
 */
public final class AudioHooks {

    private static final String TAG = "LineSpoofer-Audio";

    private AudioHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!ConfigManager.isAudioSpoofEnabled()) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                AudioManager.class,
                "getProperty",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isAudioSpoofEnabled()) {
                                return;
                            }
                            String propKey = (String) param.args[0];
                            if (AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE.equals(propKey)) {
                                param.setResult("48000");
                            } else if (AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER.equals(propKey)) {
                                param.setResult("192");
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );
            AppLogger.debug(TAG, "AudioManager.getProperty hooks loaded");
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook AudioManager.getProperty: " + throwable.getMessage(), throwable);
        }
    }
}

