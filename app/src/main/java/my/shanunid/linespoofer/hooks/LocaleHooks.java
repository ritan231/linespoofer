package my.shanunid.linespoofer.hooks;

import android.os.LocaleList;
import android.icu.util.ULocale;

import my.shanunid.linespoofer.utils.AppLogger;
import my.shanunid.linespoofer.utils.ConfigManager;

import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class LocaleHooks {

    private static final String TAG = "LineSpoofer-Locale";

    private LocaleHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        hookLocaleDefaults(lpparam);
        hookTimeZoneDefaults(lpparam);
    }

    private static void hookLocaleDefaults(XC_LoadPackage.LoadPackageParam lpparam) {
        Locale locale = configuredLocale();
        if (locale != null) {
            try {
                Locale.setDefault(locale);
            } catch (Throwable ignored) {
            }
            try {
                LocaleList.setDefault(new LocaleList(locale));
            } catch (Throwable ignored) {
            }
            try {
                Class<?> ulocClass = Class.forName("android.icu.util.ULocale");
                java.lang.reflect.Method m = ulocClass.getMethod("setDefault", ulocClass);
                Object uloc = ulocClass.getMethod("forLocale", Locale.class).invoke(null, locale);
                m.invoke(null, uloc);
            } catch (Throwable ignored) {
            }
        }

        XC_MethodHook localeHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Locale l = configuredLocale();
                if (l != null) {
                    param.setResult(l);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(Locale.class, "getDefault", localeHook);
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(Locale.class, "getDefault", Locale.Category.class, localeHook);
        } catch (Throwable ignored) {
        }

        // Hook android.os.LocaleList
        try {
            Class<?> localeListClass = XposedHelpers.findClassIfExists("android.os.LocaleList", lpparam.classLoader);
            if (localeListClass == null) {
                localeListClass = LocaleList.class;
            }
            XC_MethodHook localeListHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Locale l = configuredLocale();
                    if (l != null) {
                        param.setResult(new LocaleList(l));
                    }
                }
            };
            XposedHelpers.findAndHookMethod(localeListClass, "getDefault", localeListHook);
            XposedHelpers.findAndHookMethod(localeListClass, "getAdjustedDefault", localeListHook);
        } catch (Throwable ignored) {
        }

        // Hook android.icu.util.ULocale
        try {
            Class<?> uLocaleClass = XposedHelpers.findClassIfExists("android.icu.util.ULocale", lpparam.classLoader);
            if (uLocaleClass == null) {
                uLocaleClass = ULocale.class;
            }
            XC_MethodHook uLocaleHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Locale l = configuredLocale();
                    if (l != null) {
                        param.setResult(ULocale.forLocale(l));
                    }
                }
            };
            XposedHelpers.findAndHookMethod(uLocaleClass, "getDefault", uLocaleHook);
            try {
                XposedHelpers.findAndHookMethod(uLocaleClass, "getDefault", ULocale.Category.class, uLocaleHook);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }

        // Hook Configuration.getLocales()
        try {
            Class<?> configClass = XposedHelpers.findClassIfExists("android.content.res.Configuration", lpparam.classLoader);
            if (configClass != null) {
                XposedHelpers.findAndHookMethod(configClass, "getLocales", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Locale l = configuredLocale();
                        if (l != null) {
                            param.setResult(new LocaleList(l));
                        }
                    }
                });
            }
        } catch (Throwable ignored) {
        }
    }

    private static void hookTimeZoneDefaults(XC_LoadPackage.LoadPackageParam lpparam) {
        String timezone = ConfigManager.getTimezone();
        if (timezone != null && !timezone.trim().isEmpty()) {
            try {
                TimeZone.setDefault(TimeZone.getTimeZone(timezone));
            } catch (Throwable ignored) {
            }
            try {
                Class<?> icuTzClass = Class.forName("android.icu.util.TimeZone");
                java.lang.reflect.Method m = icuTzClass.getMethod("setDefault", icuTzClass);
                Object tz = icuTzClass.getMethod("getTimeZone", String.class).invoke(null, timezone);
                m.invoke(null, tz);
            } catch (Throwable ignored) {
            }
        }

        XC_MethodHook timeZoneHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String tz = ConfigManager.getTimezone();
                if (tz != null) {
                    param.setResult(TimeZone.getTimeZone(tz));
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(TimeZone.class, "getDefault", timeZoneHook);
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(TimeZone.class, "getDefaultRef", timeZoneHook);
        } catch (Throwable ignored) {
        }

        // Hook android.icu.util.TimeZone
        try {
            Class<?> icuTzClass = XposedHelpers.findClassIfExists("android.icu.util.TimeZone", lpparam.classLoader);
            if (icuTzClass == null) {
                icuTzClass = android.icu.util.TimeZone.class;
            }
            XposedHelpers.findAndHookMethod(icuTzClass, "getDefault", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String tz = ConfigManager.getTimezone();
                    if (tz != null) {
                        param.setResult(android.icu.util.TimeZone.getTimeZone(tz));
                    }
                }
            });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(ZoneId.class, "systemDefault",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String tz = ConfigManager.getTimezone();
                        if (tz == null) {
                            return;
                        }
                        try {
                            param.setResult(ZoneId.of(tz));
                        } catch (Throwable ignored) {
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static Locale configuredLocale() {
        String languageTag = ConfigManager.getLocaleTag();
        if (languageTag == null) {
            return null;
        }
        Locale locale = Locale.forLanguageTag(languageTag.replace('_', '-'));
        return locale.getLanguage().isEmpty() ? null : locale;
    }
}
