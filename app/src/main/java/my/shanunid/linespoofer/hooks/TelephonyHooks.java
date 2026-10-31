package my.shanunid.linespoofer.hooks;

import android.telephony.TelephonyManager;

import my.shanunid.linespoofer.utils.ConfigManager;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks TelephonyManager methods to spoof device identifiers.
 * Spoofs: IMEI, MEID, IMSI, ICCID, phone number
 */
public class TelephonyHooks {

    private static final int SYNTHETIC_SUBSCRIPTION_ID = 1;
    private static final int SYNTHETIC_SIM_SLOT_INDEX = 0;
    private static final Set<Object> SYNTHETIC_SUBSCRIPTION_INFOS =
        Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Object, Integer> SYNTHETIC_SUB_IDS =
        Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Integer> SYNTHETIC_SLOTS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private static int getSlotForInfo(Object thisObject) {
        if (thisObject == null) return 0;
        Integer synthSlot = SYNTHETIC_SLOTS.get(thisObject);
        if (synthSlot != null) return synthSlot;
        Integer synthSubId = SYNTHETIC_SUB_IDS.get(thisObject);
        if (synthSubId != null && synthSubId == 2) return 1;
        try {
            java.lang.reflect.Field slotField = XposedHelpers.findFieldIfExists(thisObject.getClass(), "mSimSlotIndex");
            if (slotField != null) {
                slotField.setAccessible(true);
                return slotField.getInt(thisObject);
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> telephonyManager = XposedHelpers.findClassIfExists(
                "android.telephony.TelephonyManager",
                lpparam.classLoader
        );
        Class<?> subscriptionManager = XposedHelpers.findClassIfExists(
                "android.telephony.SubscriptionManager",
                lpparam.classLoader
        );
        Class<?> subscriptionInfo = XposedHelpers.findClassIfExists(
                "android.telephony.SubscriptionInfo",
                lpparam.classLoader
        );

        if (telephonyManager == null) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getDeviceId",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getIMEI();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getDeviceId", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            int slot = (int) param.args[0];
                            String spoofedValue = (slot == 1) ? ConfigManager.getIMEI2() : ConfigManager.getIMEI();
                            if (spoofedValue == null && slot == 1) {
                                spoofedValue = ConfigManager.getIMEI();
                            }
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getImei",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getIMEI();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getImei", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            int slot = (int) param.args[0];
                            String spoofedValue = (slot == 1) ? ConfigManager.getIMEI2() : ConfigManager.getIMEI();
                            if (spoofedValue == null && slot == 1) {
                                spoofedValue = ConfigManager.getIMEI();
                            }
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getMeid",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getMEID();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getMeid", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getMEID();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSubscriberId",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getIMSI();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSubscriberId", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getIMSI();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimSerialNumber",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getICCID();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimSerialNumber", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = ConfigManager.getICCID();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getLine1Number",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String spoofedValue = ConfigManager.getPhoneNumberSim1();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                                return;
                            }
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getLine1Number", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            int subId = (int) param.args[0];
                            String spoofedValue = (subId == 2) ? ConfigManager.getPhoneNumberSim2() : ConfigManager.getPhoneNumberSim1();
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                                return;
                            }
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getPhoneType",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(TelephonyManager.PHONE_TYPE_NONE);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(TelephonyManager.PHONE_TYPE_GSM);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "hasIccCard",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(false);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "hasIccCard", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(false);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimState",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(TelephonyManager.SIM_STATE_ABSENT);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(TelephonyManager.SIM_STATE_READY);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimState", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(TelephonyManager.SIM_STATE_ABSENT);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(TelephonyManager.SIM_STATE_READY);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getPhoneCount",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(0);
                                return;
                            }
                            param.setResult(1);
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimCount",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(0);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(1);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getActiveModemCount",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(0);
                                return;
                            }
                            param.setResult(1);
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimCardState",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(TelephonyManager.SIM_STATE_ABSENT);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(TelephonyManager.SIM_STATE_READY);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimApplicationState",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(TelephonyManager.SIM_STATE_ABSENT);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(TelephonyManager.SIM_STATE_READY);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSubscriptionId",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(SYNTHETIC_SUBSCRIPTION_ID);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "createForSubscriptionId",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony() || !ConfigManager.shouldReportSimPresent()) {
                                return;
                            }
                            Object manager = param.getResult();
                            if (manager != null) {
                                try {
                                    XposedHelpers.setIntField(manager, "mSubId", SYNTHETIC_SUBSCRIPTION_ID);
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "isVoiceCapable",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(false);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        if (subscriptionManager != null) {
            hookSubscriptionManager(subscriptionManager, subscriptionInfo);
        }
        if (subscriptionInfo != null) {
            hookSubscriptionInfo(subscriptionInfo);
        }

        hookNetworkState(telephonyManager);

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "isSmsCapable",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(false);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        // Hook network operator methods (MCC/MNC) - both no-arg and (int subId)
        XC_MethodHook networkOperatorHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String mccMnc = ConfigManager.getSystemProperty("gsm.operator.numeric", null);
                if (mccMnc != null) {
                    param.setResult(mccMnc);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getNetworkOperator", networkOperatorHook);
        } catch (NoSuchMethodError ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getNetworkOperator", int.class, networkOperatorHook);
        } catch (NoSuchMethodError ignored) {
        }

        XC_MethodHook networkOperatorNameHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String operatorName = ConfigManager.getSystemProperty("gsm.operator.alpha", null);
                if (operatorName != null) {
                    param.setResult(operatorName);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getNetworkOperatorName", networkOperatorNameHook);
        } catch (NoSuchMethodError ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getNetworkOperatorName", int.class, networkOperatorNameHook);
        } catch (NoSuchMethodError ignored) {
        }

        XC_MethodHook simOperatorHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String simMccMnc = ConfigManager.getSystemProperty("gsm.sim.operator.numeric", null);
                if (simMccMnc != null) {
                    param.setResult(simMccMnc);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimOperator", simOperatorHook);
        } catch (NoSuchMethodError ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimOperator", int.class, simOperatorHook);
        } catch (NoSuchMethodError ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimOperatorNumeric", simOperatorHook);
        } catch (NoSuchMethodError ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimOperatorNumeric", int.class, simOperatorHook);
        } catch (NoSuchMethodError ignored) {
        }

        XC_MethodHook simOperatorNameHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String simOperatorName = ConfigManager.getSystemProperty("gsm.sim.operator.alpha", null);
                if (simOperatorName != null) {
                    param.setResult(simOperatorName);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimOperatorName", simOperatorNameHook);
        } catch (NoSuchMethodError ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimOperatorName", int.class, simOperatorNameHook);
        } catch (NoSuchMethodError ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimCarrierIdName", simOperatorNameHook);
        } catch (NoSuchMethodError ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimSpecificCarrierIdName", simOperatorNameHook);
        } catch (NoSuchMethodError ignored) {
        }

        XC_MethodHook simCountryIsoHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String simCountry = ConfigManager.getSystemProperty("gsm.sim.operator.iso-country", null);
                if (simCountry != null) {
                    param.setResult(simCountry);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimCountryIso", simCountryIsoHook);
        } catch (NoSuchMethodError ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getSimCountryIso", int.class, simCountryIsoHook);
        } catch (NoSuchMethodError ignored) {
        }

        XC_MethodHook networkCountryIsoHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String networkCountry = firstNonBlank(
                    ConfigManager.getSystemProperty("gsm.operator.iso-country", null),
                    ConfigManager.getSystemProperty("gsm.sim.operator.iso-country", null)
                );
                if (networkCountry != null) {
                    param.setResult(networkCountry);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getNetworkCountryIso", networkCountryIsoHook);
        } catch (NoSuchMethodError ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getNetworkCountryIso", int.class, networkCountryIsoHook);
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getVoiceMailNumber",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = firstNonBlank(ConfigManager.getPhoneNumber(), null);
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getDeviceSoftwareVersion",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            String spoofedValue = firstNonBlank(
                                ConfigManager.getBuildDisplay(),
                                ConfigManager.getBuildVersionIncremental()
                            );
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }

    private static void hookSubscriptionManager(Class<?> subscriptionManager, Class<?> subscriptionInfoClass) {
        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getActiveSubscriptionInfoCount",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(0);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(2);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getActiveSubscriptionInfoCountMax",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(0);
                                return;
                            }
                            param.setResult(2);
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getActiveSubscriptionIdList",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(new int[0]);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(new int[]{1, 2});
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        // Modern Android 13+ (API 33+) SubscriptionManager.getPhoneNumber
        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getPhoneNumber",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult("");
                                return;
                            }
                            int subId = (int) param.args[0];
                            String number = (subId == 2) ? ConfigManager.getPhoneNumberSim2() : ConfigManager.getPhoneNumberSim1();
                            param.setResult(number != null ? number : "");
                        }
                    });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getPhoneNumber",
                    int.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult("");
                                return;
                            }
                            int subId = (int) param.args[0];
                            String number = (subId == 2) ? ConfigManager.getPhoneNumberSim2() : ConfigManager.getPhoneNumberSim1();
                            param.setResult(number != null ? number : "");
                        }
                    });
        } catch (Throwable ignored) {
        }

        hookSubscriptionInfoListMethod(subscriptionManager, "getActiveSubscriptionInfoList", subscriptionInfoClass);
        hookSubscriptionInfoListMethod(subscriptionManager, "getCompleteActiveSubscriptionInfoList", subscriptionInfoClass);
        hookSubscriptionInfoListMethod(subscriptionManager, "getAccessibleSubscriptionInfoList", subscriptionInfoClass);
        hookSubscriptionInfoListMethod(subscriptionManager, "getAllSubscriptionInfoList", subscriptionInfoClass);

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getActiveSubscriptionInfo",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                int subId = (int) param.args[0];
                                int slot = (subId == 2) ? 1 : 0;
                                param.setResult(createSyntheticSubscriptionInfo(subscriptionInfoClass, subId, slot));
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getActiveSubscriptionInfoForSimSlotIndex",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            int slotIndex = param.args[0] instanceof Integer ? (Integer) param.args[0] : 0;
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(null);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                if (slotIndex == 0) {
                                    param.setResult(createSyntheticSubscriptionInfo(subscriptionInfoClass, 1, 0));
                                } else if (slotIndex == 1) {
                                    param.setResult(createSyntheticSubscriptionInfo(subscriptionInfoClass, 2, 1));
                                } else {
                                    param.setResult(null);
                                }
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        hookStaticSubscriptionIds(subscriptionManager);
    }

    private static void hookSubscriptionInfoListMethod(Class<?> subscriptionManager, String methodName, Class<?> subscriptionInfoClass) {
        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, methodName,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(Collections.emptyList());
                                return;
                            }
                            if (!ConfigManager.shouldReportSimPresent()) {
                                return;
                            }
                            List<Object> list = new ArrayList<>();
                            Object sim1 = createSyntheticSubscriptionInfo(subscriptionInfoClass, 1, 0);
                            if (sim1 != null) {
                                list.add(sim1);
                            }
                            Object sim2 = createSyntheticSubscriptionInfo(subscriptionInfoClass, 2, 1);
                            if (sim2 != null) {
                                list.add(sim2);
                            }
                            param.setResult(list);
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }

    private static void hookStaticSubscriptionIds(Class<?> subscriptionManager) {
        hookStaticSubscriptionInt(subscriptionManager, "getDefaultSubscriptionId", SYNTHETIC_SUBSCRIPTION_ID);
        hookStaticSubscriptionInt(subscriptionManager, "getDefaultVoiceSubscriptionId", SYNTHETIC_SUBSCRIPTION_ID);
        hookStaticSubscriptionInt(subscriptionManager, "getDefaultSmsSubscriptionId", SYNTHETIC_SUBSCRIPTION_ID);
        hookStaticSubscriptionInt(subscriptionManager, "getDefaultDataSubscriptionId", SYNTHETIC_SUBSCRIPTION_ID);
        hookStaticSubscriptionInt(subscriptionManager, "getActiveDataSubscriptionId", SYNTHETIC_SUBSCRIPTION_ID);

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getSubscriptionId",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            int slotIndex = param.args[0] instanceof Integer ? (Integer) param.args[0] : 0;
                            if (ConfigManager.shouldExposeTelephony() && ConfigManager.shouldReportSimPresent()) {
                                param.setResult(slotIndex == 1 ? 2 : 1);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getSlotIndex",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            int subId = param.args[0] instanceof Integer ? (Integer) param.args[0] : 1;
                            if (ConfigManager.shouldExposeTelephony() && ConfigManager.shouldReportSimPresent()) {
                                param.setResult(subId == 2 ? 1 : 0);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "getSubscriptionIds",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            int slotIndex = param.args[0] instanceof Integer ? (Integer) param.args[0] : 0;
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(new int[0]);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(slotIndex == 1 ? new int[]{2} : new int[]{1});
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "isActiveSubscriptionId",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                param.setResult(false);
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "isValidSubscriptionId",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (ConfigManager.shouldExposeTelephony() && ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, "isUsableSubscriptionId",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (ConfigManager.shouldExposeTelephony() && ConfigManager.shouldReportSimPresent()) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }

    private static void hookStaticSubscriptionInt(Class<?> subscriptionManager, String methodName, int value) {
        try {
            XposedHelpers.findAndHookMethod(subscriptionManager, methodName,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!ConfigManager.shouldExposeTelephony()) {
                                return;
                            }
                            if (ConfigManager.shouldReportSimPresent()) {
                                param.setResult(value);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }

    private static void hookSubscriptionInfo(Class<?> subscriptionInfoClass) {
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getSubscriptionId", thisObj -> (getSlotForInfo(thisObj) == 1 ? 2 : 1));
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getSimSlotIndex", TelephonyHooks::getSlotForInfo);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getCarrierId", thisObj -> 0);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getDisplayName", thisObj -> {
            int slot = getSlotForInfo(thisObj);
            String carrier = firstNonBlank(
                    ConfigManager.getSystemProperty("gsm.sim.operator.alpha", null),
                    ConfigManager.getSystemProperty("gsm.operator.alpha", null),
                    "SIM"
            );
            return carrier + " " + (slot + 1);
        });
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getCarrierName", thisObj -> firstNonBlank(
                ConfigManager.getSystemProperty("gsm.sim.operator.alpha", null),
                ConfigManager.getSystemProperty("gsm.operator.alpha", null),
                "Carrier"
        ));
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getNumber", thisObj -> {
            int slot = getSlotForInfo(thisObj);
            String num = (slot == 1) ? ConfigManager.getPhoneNumberSim2() : ConfigManager.getPhoneNumberSim1();
            return firstNonBlank(num, "");
        });
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getDataRoaming", thisObj -> 0);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getMcc", thisObj -> parseMcc());
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getMnc", thisObj -> parseMnc());
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getMccString", thisObj -> parseMccString());
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getMncString", thisObj -> parseMncString());
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getCountryIso", thisObj -> firstNonBlank(
                ConfigManager.getSystemProperty("gsm.sim.operator.iso-country", null),
                "us"
        ));
        hookSubscriptionInfoGetter(subscriptionInfoClass, "isEmbedded", thisObj -> false);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "isOpportunistic", thisObj -> false);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getSubscriptionType", thisObj -> 0);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getCardId", thisObj -> (getSlotForInfo(thisObj) == 1 ? 1 : 0));
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getPortIndex", thisObj -> 0);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getUsageSetting", thisObj -> 0);
        hookSubscriptionInfoGetter(subscriptionInfoClass, "getIccId", thisObj -> {
            String base = firstNonBlank(ConfigManager.getICCID(), "8901260123456789011");
            return (getSlotForInfo(thisObj) == 1) ? (base.substring(0, Math.max(0, base.length() - 1)) + "2") : base;
        });
    }

    private static void hookSubscriptionInfoGetter(Class<?> subscriptionInfoClass, String methodName, ValueProvider valueProvider) {
        try {
            XposedHelpers.findAndHookMethod(subscriptionInfoClass, methodName,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (ConfigManager.shouldExposeTelephony() && ConfigManager.shouldReportSimPresent()) {
                                Object val = valueProvider.get(param.thisObject);
                                if (val != null) {
                                    param.setResult(val);
                                }
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }

    private static Object createSyntheticSubscriptionInfo(Class<?> subscriptionInfoClass) {
        return createSyntheticSubscriptionInfo(subscriptionInfoClass, 1, 0);
    }

    private static Object createSyntheticSubscriptionInfo(Class<?> subscriptionInfoClass, int subId, int slotIndex) {
        if (subscriptionInfoClass == null) {
            return null;
        }
        Object instance = null;
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            java.lang.reflect.Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            java.lang.reflect.Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
            instance = allocateInstance.invoke(unsafe, subscriptionInfoClass);
        } catch (Throwable ignored) {
        }
        if (instance == null) {
            try {
                Constructor<?> constructor = subscriptionInfoClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                instance = constructor.newInstance();
            } catch (Throwable ignored) {
                return null;
            }
        }
        if (instance != null) {
            SYNTHETIC_SUBSCRIPTION_INFOS.add(instance);
            SYNTHETIC_SUB_IDS.put(instance, subId);
            SYNTHETIC_SLOTS.put(instance, slotIndex);
            try {
                java.lang.reflect.Field slotField = XposedHelpers.findFieldIfExists(subscriptionInfoClass, "mSimSlotIndex");
                if (slotField != null) {
                    slotField.setAccessible(true);
                    slotField.setInt(instance, slotIndex);
                }
                java.lang.reflect.Field idField = XposedHelpers.findFieldIfExists(subscriptionInfoClass, "mId");
                if (idField != null) {
                    idField.setAccessible(true);
                    idField.setInt(instance, subId);
                }
            } catch (Throwable ignored) {
            }
        }
        return instance;
    }

    private static String firstNonBlank(String value, String fallback) {
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        return fallback;
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        String resolved = firstNonBlank(first, null);
        if (resolved != null && !resolved.isEmpty()) {
            return resolved;
        }
        resolved = firstNonBlank(second, null);
        if (resolved != null && !resolved.isEmpty()) {
            return resolved;
        }
        return fallback;
    }

    private static int parseMcc() {
        String mcc = parseMccString();
        try {
            return Integer.parseInt(mcc);
        } catch (Exception ignored) {
            return 310;
        }
    }

    private static int parseMnc() {
        String mnc = parseMncString();
        try {
            return Integer.parseInt(mnc);
        } catch (Exception ignored) {
            return 260;
        }
    }

    private static String parseMccString() {
        String numeric = firstNonBlank(
                ConfigManager.getSystemProperty("gsm.sim.operator.numeric", null),
                ConfigManager.getSystemProperty("gsm.operator.numeric", null),
                "310260"
        );
        if (numeric.length() >= 3) {
            return numeric.substring(0, 3);
        }
        return "310";
    }

    private static String parseMncString() {
        String numeric = firstNonBlank(
                ConfigManager.getSystemProperty("gsm.sim.operator.numeric", null),
                ConfigManager.getSystemProperty("gsm.operator.numeric", null),
                "310260"
        );
        if (numeric.length() > 3) {
            return numeric.substring(3);
        }
        return "260";
    }

    private static void hookNetworkState(Class<?> telephonyManager) {
        // Hook getNetworkType() -> NETWORK_TYPE_LTE (13)
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getNetworkType",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (ConfigManager.shouldExposeTelephony()) {
                            param.setResult(13); // NETWORK_TYPE_LTE
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        // Hook getDataNetworkType() -> NETWORK_TYPE_LTE (13)
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getDataNetworkType",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (ConfigManager.shouldExposeTelephony()) {
                            param.setResult(13);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        // Hook getVoiceNetworkType() -> NETWORK_TYPE_LTE (13)
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getVoiceNetworkType",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (ConfigManager.shouldExposeTelephony()) {
                            param.setResult(13);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        // Hook getDataState() -> DATA_CONNECTED (2)
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "getDataState",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (ConfigManager.shouldExposeTelephony()) {
                            param.setResult(2); // DATA_CONNECTED
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        // Hook isNetworkRoaming() -> false
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "isNetworkRoaming",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (ConfigManager.shouldExposeTelephony()) {
                            param.setResult(false);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        // Hook isDataEnabled() -> true
        try {
            XposedHelpers.findAndHookMethod(telephonyManager, "isDataEnabled",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (ConfigManager.shouldExposeTelephony()) {
                            param.setResult(true);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private interface ValueProvider {
        Object get(Object thisObject);
    }
}
