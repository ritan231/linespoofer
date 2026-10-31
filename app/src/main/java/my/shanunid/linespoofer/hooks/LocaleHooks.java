package my.shanunid.linespoofer.hooks;

import android.content.res.Configuration;
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

    public static void applyLocaleToConfig(Configuration config, Locale locale) {
        if (config == null || locale == null) return;
        try {
            config.setLocales(new LocaleList(locale));
        } catch (Throwable ignored) {
            try {
                config.locale = locale;
            } catch (Throwable ignored2) {
            }
        }
    }

    private static void hookLocaleDefaults(XC_LoadPackage.LoadPackageParam lpparam) {
        Locale locale = configuredLocale();
        if (locale != null) {
            try {
                System.setProperty("user.language", locale.getLanguage());
                System.setProperty("user.country", locale.getCountry());
                System.setProperty("user.locale", locale.toLanguageTag());
            } catch (Throwable ignored) {
            }
            try {
                Locale.setDefault(locale);
                for (String fieldName : new String[]{"defaultLocale", "defaultDisplayLocale", "defaultFormatLocale"}) {
                    try {
                        java.lang.reflect.Field f = Locale.class.getDeclaredField(fieldName);
                        f.setAccessible(true);
                        f.set(null, locale);
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
            try {
                LocaleList list = new LocaleList(locale);
                LocaleList.setDefault(list);
                for (String fieldName : new String[]{"sDefaultLocaleList", "sLastDefaultLocaleList"}) {
                    try {
                        java.lang.reflect.Field f = LocaleList.class.getDeclaredField(fieldName);
                        f.setAccessible(true);
                        f.set(null, list);
                    } catch (Throwable ignored) {
                    }
                }
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

        // Hook Configuration.getLocales(), setTo, and updateFrom
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
                XposedHelpers.findAndHookMethod(configClass, "setTo", "android.content.res.Configuration", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Locale l = configuredLocale();
                        if (l != null && param.thisObject instanceof Configuration) {
                            applyLocaleToConfig((Configuration) param.thisObject, l);
                        }
                    }
                });
                XposedHelpers.findAndHookMethod(configClass, "updateFrom", "android.content.res.Configuration", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Locale l = configuredLocale();
                        if (l != null && param.thisObject instanceof Configuration) {
                            applyLocaleToConfig((Configuration) param.thisObject, l);
                        }
                    }
                });
            }
        } catch (Throwable ignored) {
        }

        // Hook Resources.getConfiguration() and ResourcesImpl.getConfiguration()
        try {
            Class<?> resClass = XposedHelpers.findClassIfExists("android.content.res.Resources", lpparam.classLoader);
            if (resClass != null) {
                XposedHelpers.findAndHookMethod(resClass, "getConfiguration", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Locale l = configuredLocale();
                        if (l != null && param.getResult() instanceof Configuration) {
                            applyLocaleToConfig((Configuration) param.getResult(), l);
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(resClass, "getSystem", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            Object systemRes = param.getResult();
                            if (systemRes instanceof android.content.res.Resources) {
                                Configuration cfg = ((android.content.res.Resources) systemRes).getConfiguration();
                                Locale l = configuredLocale();
                                if (cfg != null && l != null) {
                                    applyLocaleToConfig(cfg, l);
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                });
            }
        } catch (Throwable ignored) {
        }

        try {
            Class<?> resImplClass = XposedHelpers.findClassIfExists("android.content.res.ResourcesImpl", lpparam.classLoader);
            if (resImplClass != null) {
                XposedHelpers.findAndHookMethod(resImplClass, "getConfiguration", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Locale l = configuredLocale();
                        if (l != null && param.getResult() instanceof Configuration) {
                            applyLocaleToConfig((Configuration) param.getResult(), l);
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
                System.setProperty("user.timezone", timezone);
            } catch (Throwable ignored) {
            }
            try {
                TimeZone.setDefault(TimeZone.getTimeZone(timezone));
                try {
                    java.lang.reflect.Field f = TimeZone.class.getDeclaredField("defaultTimeZone");
                    f.setAccessible(true);
                    f.set(null, TimeZone.getTimeZone(timezone));
                } catch (Throwable ignored) {
                }
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

        try {
            XposedHelpers.findAndHookMethod(java.util.Calendar.class, "getInstance",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            String tz = ConfigManager.getTimezone();
                            Locale l = configuredLocale();
                            if (tz != null && l != null) {
                                param.setResult(java.util.Calendar.getInstance(TimeZone.getTimeZone(tz), l));
                            } else if (tz != null) {
                                param.setResult(java.util.Calendar.getInstance(TimeZone.getTimeZone(tz)));
                            } else if (l != null) {
                                param.setResult(java.util.Calendar.getInstance(l));
                            }
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
