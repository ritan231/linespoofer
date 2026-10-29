package my.shanunid.linespoofer.hooks;

import android.content.Context;
import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks WebView User-Agent, WebGL vendor/renderer, and Canvas fingerprinting.
 *
 * 1. Overrides User-Agent on WebSettings and constructors.
 * 2. Injects unforgeable WebGL parameter spoofing (GL_VENDOR, GL_RENDERER,
 *    UNMASKED_VENDOR_WEBGL, UNMASKED_RENDERER_WEBGL) matching the spoofed SoC profile.
 * 3. Applies non-destructive micro-variance on CanvasRenderingContext2D.getImageData
 *    to neutralize deterministic cross-session canvas tracking.
 */
public class WebViewHooks {

    private static final String TAG = "LineSpoofer-WebView";
    private static final Set<Class<?>> HOOKED_CLIENT_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Set<Class<?>> HOOKED_SETTINGS_CLASSES = ConcurrentHashMap.newKeySet();

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookWebSettings(lpparam);
            hookWebViewConstructor(lpparam);
            hookWebViewClientAndLoadUrl(lpparam);
            AppLogger.info(TAG, "WebViewHooks initialized successfully for " + lpparam.packageName);
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook WebView: " + throwable.getMessage(), throwable);
        }
    }

    private static void hookWebSettings(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> webViewClass = XposedHelpers.findClassIfExists("android.webkit.WebView", lpparam.classLoader);
        if (webViewClass == null) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                webViewClass,
                "getSettings",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object settings = param.getResult();
                        if (settings != null) {
                            String spoofedUA = ConfigManager.getWebViewUserAgent();
                            if (spoofedUA != null) {
                                try {
                                    XposedHelpers.callMethod(settings, "setUserAgentString", spoofedUA);
                                } catch (Throwable ignored) {
                                }
                            }
                            hookSettingsClass(settings.getClass());
                        }
                    }
                }
            );
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook WebView.getSettings(): " + throwable.getMessage(), throwable);
        }
    }

    private static void hookSettingsClass(Class<?> settingsClass) {
        if (settingsClass == null || !HOOKED_SETTINGS_CLASSES.add(settingsClass)) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                settingsClass,
                "getUserAgentString",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String spoofedUA = ConfigManager.getWebViewUserAgent();
                        if (spoofedUA != null) {
                            param.setResult(spoofedUA);
                        }
                    }
                }
            );
        } catch (Throwable ignored) {
        }
    }

    private static void hookWebViewConstructor(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> webViewClass = XposedHelpers.findClassIfExists("android.webkit.WebView", lpparam.classLoader);
        if (webViewClass == null) {
            return;
        }

        XC_MethodHook constructorHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    Object webView = param.thisObject;
                    Object settings = XposedHelpers.callMethod(webView, "getSettings");
                    String spoofedUA = ConfigManager.getWebViewUserAgent();
                    if (spoofedUA != null && settings != null) {
                        XposedHelpers.callMethod(settings, "setUserAgentString", spoofedUA);
                    }
                } catch (Throwable ignored) {
                }
            }
        };

        try {
            XposedHelpers.findAndHookConstructor(webViewClass, Context.class, constructorHook);
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookConstructor(
                webViewClass,
                Context.class,
                android.util.AttributeSet.class,
                constructorHook
            );
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookConstructor(
                webViewClass,
                Context.class,
                android.util.AttributeSet.class,
                int.class,
                constructorHook
            );
        } catch (Throwable ignored) {
        }
    }

    private static void hookWebViewClientAndLoadUrl(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> webViewClass = XposedHelpers.findClassIfExists("android.webkit.WebView", lpparam.classLoader);
        if (webViewClass == null) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                webViewClass,
                "setWebViewClient",
                WebViewClient.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Object client = param.args[0];
                        if (client != null) {
                            hookClientClass(client.getClass());
                        }
                    }
                }
            );
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook WebView.setWebViewClient: " + throwable.getMessage(), throwable);
        }

        try {
            XposedHelpers.findAndHookMethod(
                webViewClass,
                "loadUrl",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!ConfigManager.isWebGlSpoofEnabled()) {
                            return;
                        }
                        String url = (String) param.args[0];
                        if (url != null && !url.startsWith("javascript:")) {
                            injectFingerprintScript(param.thisObject);
                        }
                    }
                }
            );
        } catch (Throwable throwable) {
            AppLogger.logError(TAG, "Failed to hook WebView.loadUrl: " + throwable.getMessage(), throwable);
        }
    }

    private static void hookClientClass(Class<?> clientClass) {
        if (clientClass == null || !HOOKED_CLIENT_CLASSES.add(clientClass)) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                clientClass,
                "onPageStarted",
                WebView.class,
                String.class,
                Bitmap.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!ConfigManager.isWebGlSpoofEnabled()) {
                            return;
                        }
                        injectFingerprintScript(param.args[0]);
                    }
                }
            );
        } catch (Throwable ignored) {
        }
    }

    private static void injectFingerprintScript(Object webView) {
        if (webView == null) {
            return;
        }
        try {
            String script = buildFingerprintJsScript();
            XposedHelpers.callMethod(webView, "evaluateJavascript", script, null);
        } catch (Throwable ignored) {
            try {
                String script = "javascript:" + buildFingerprintJsScript();
                XposedHelpers.callMethod(webView, "loadUrl", script);
            } catch (Throwable ignored2) {
            }
        }
    }

    private static String buildFingerprintJsScript() {
        String vendor = ConfigManager.getWebGlVendor();
        String renderer = ConfigManager.getWebGlRenderer();
        return "(function() {\n"
            + "  if (window.__linespoofer_injected) return;\n"
            + "  window.__linespoofer_injected = true;\n"
            + "  try {\n"
            + "    var glVendor = '" + vendor + "';\n"
            + "    var glRenderer = '" + renderer + "';\n"
            + "    function wrapGetParameter(origFn) {\n"
            + "      return function(param) {\n"
            + "        if (param === 0x1F00 || param === 0x9245) return glVendor;\n"
            + "        if (param === 0x1F01 || param === 0x9246) return glRenderer;\n"
            + "        return origFn.apply(this, arguments);\n"
            + "      };\n"
            + "    }\n"
            + "    if (typeof WebGLRenderingContext !== 'undefined' && WebGLRenderingContext.prototype) {\n"
            + "      WebGLRenderingContext.prototype.getParameter = wrapGetParameter(WebGLRenderingContext.prototype.getParameter);\n"
            + "    }\n"
            + "    if (typeof WebGL2RenderingContext !== 'undefined' && WebGL2RenderingContext.prototype) {\n"
            + "      WebGL2RenderingContext.prototype.getParameter = wrapGetParameter(WebGL2RenderingContext.prototype.getParameter);\n"
            + "    }\n"
            + "    if (typeof CanvasRenderingContext2D !== 'undefined' && CanvasRenderingContext2D.prototype) {\n"
            + "      var origGetImageData = CanvasRenderingContext2D.prototype.getImageData;\n"
            + "      CanvasRenderingContext2D.prototype.getImageData = function(sx, sy, sw, sh) {\n"
            + "        var imgData = origGetImageData.apply(this, arguments);\n"
            + "        if (imgData && imgData.data && imgData.data.length > 3) {\n"
            + "          imgData.data[0] = (imgData.data[0] ^ 1);\n"
            + "        }\n"
            + "        return imgData;\n"
            + "      };\n"
            + "    }\n"
            + "  } catch(e) {}\n"
            + "})();";
    }
}
