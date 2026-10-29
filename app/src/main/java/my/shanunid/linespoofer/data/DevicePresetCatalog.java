package my.shanunid.linespoofer.data;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;

import my.shanunid.linespoofer.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DevicePresetCatalog {

    private static final String CACHE_NAME = "remote_device_presets.json";
    private static final String CACHE_KEY_SOURCE_URL = "sourceUrl";
    private static final String CACHE_KEY_UPDATED_AT = "updatedAt";
    private static final String CACHE_KEY_PRESETS = "presets";
    private static final String CURRENT_DEVICE_PRESET_ID = "current_device";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 12000;

    public List<DevicePreset> load(Context context) {
        List<DevicePreset> cachedPresets = loadFromCache(context);
        if (!cachedPresets.isEmpty()) {
            return withCurrentDevicePreset(context, cachedPresets);
        }
        // Offline-first architecture: return robust offline bundled presets without blocking on network
        return withCurrentDevicePreset(context, getBundledOfflinePresets());
    }

    public List<DevicePreset> refreshRemote(Context context, String sourceUrl) {
        try {
            JSONArray normalizedArray = fetchRemotePresetArray(sourceUrl);
            List<DevicePreset> presets = parsePresetArray(normalizedArray);
            if (!presets.isEmpty()) {
                writeCache(context, sourceUrl, normalizedArray);
                return withCurrentDevicePreset(context, presets);
            }
        } catch (Exception ignored) {
        }
        List<DevicePreset> cached = loadFromCache(context);
        if (!cached.isEmpty()) {
            return withCurrentDevicePreset(context, cached);
        }
        return withCurrentDevicePreset(context, getBundledOfflinePresets());
    }

    /**
     * Curated list of realistic offline device presets bundled directly inside LineSpoofer.
     * Guarantees 100% offline functionality without external network dependencies.
     */
    public List<DevicePreset> getBundledOfflinePresets() {
        List<DevicePreset> presets = new ArrayList<>();

        // 1. Google Pixel 7 Pro (cheetah)
        DeviceProfile pixel = new DeviceProfile();
        pixel.setBrand("google");
        pixel.setManufacturer("Google");
        pixel.setModel("Pixel 7 Pro");
        pixel.setProductName("cheetah");
        pixel.setDeviceCode("cheetah");
        pixel.setBoard("cheetah");
        pixel.setHardware("cheetah");
        pixel.setBoardPlatform("gs201");
        pixel.setBuildFingerprint("google/cheetah/cheetah:14/AP2A.240805.005/12025142:user/release-keys");
        pixel.setBuildId("AP2A.240805.005");
        pixel.setBuildDisplayId("AP2A.240805.005");
        pixel.setBuildIncremental("12025142");
        pixel.setBuildRelease("14");
        pixel.setBuildSdk(34);
        pixel.setSecurityPatch("2024-08-05");
        pixel.setScreenWidth(1440);
        pixel.setScreenHeight(3120);
        pixel.setScreenDensity(560);
        pixel.setCpuAbi("arm64-v8a");
        pixel.setCpuAbiList("arm64-v8a");
        pixel.setCpuAbiList64("arm64-v8a");
        pixel.setCpuAbiList32("");
        pixel.setSocModel("Tensor G2");
        pixel.setSocManufacturer("Google");
        pixel.setTimezone("America/New_York");
        pixel.setLocale("en-US");
        pixel.applyFallbacks();
        presets.add(new DevicePreset("pixel_7_pro", "Google", "Pixel 7 Pro", "Tensor G2 - Android 14", pixel));

        // 2. Samsung Galaxy S23 Ultra (SM-S918B)
        DeviceProfile s23 = new DeviceProfile();
        s23.setBrand("samsung");
        s23.setManufacturer("samsung");
        s23.setModel("SM-S918B");
        s23.setProductName("dm3qxxx");
        s23.setDeviceCode("dm3q");
        s23.setBoard("kalama");
        s23.setHardware("qcom");
        s23.setBoardPlatform("kalama");
        s23.setBuildFingerprint("samsung/dm3qxxx/dm3q:14/UP1A.231005.007/S918BXXU3BWJM:user/release-keys");
        s23.setBuildId("UP1A.231005.007");
        s23.setBuildDisplayId("UP1A.231005.007.S918BXXU3BWJM");
        s23.setBuildIncremental("S918BXXU3BWJM");
        s23.setBuildRelease("14");
        s23.setBuildSdk(34);
        s23.setSecurityPatch("2024-03-01");
        s23.setScreenWidth(1440);
        s23.setScreenHeight(3088);
        s23.setScreenDensity(500);
        s23.setCpuAbi("arm64-v8a");
        s23.setCpuAbiList("arm64-v8a,armeabi-v7a,armeabi");
        s23.setCpuAbiList64("arm64-v8a");
        s23.setCpuAbiList32("armeabi-v7a,armeabi");
        s23.setSocModel("Snapdragon 8 Gen 2");
        s23.setSocManufacturer("Qualcomm");
        s23.setTimezone("America/New_York");
        s23.setLocale("en-US");
        s23.applyFallbacks();
        presets.add(new DevicePreset("galaxy_s23_ultra", "Samsung", "Galaxy S23 Ultra (SM-S918B)", "Snapdragon 8 Gen 2 - Android 14", s23));

        // 3. Xiaomi POCO F6 / Redmi Turbo 3 (malachite)
        DeviceProfile poco = new DeviceProfile();
        poco.setBrand("POCO");
        poco.setManufacturer("Xiaomi");
        poco.setModel("24095PCADG");
        poco.setProductName("malachite");
        poco.setDeviceCode("malachite");
        poco.setBoard("malachite");
        poco.setHardware("qcom");
        poco.setBoardPlatform("crow");
        poco.setBuildFingerprint("POCO/malachite_global/malachite:14/UKQ1.231003.002/V816.0.4.0.UNPMIXM:user/release-keys");
        poco.setBuildId("UKQ1.231003.002");
        poco.setBuildDisplayId("UKQ1.231003.002");
        poco.setBuildIncremental("V816.0.4.0.UNPMIXM");
        poco.setBuildRelease("14");
        poco.setBuildSdk(34);
        poco.setSecurityPatch("2024-05-01");
        poco.setScreenWidth(1220);
        poco.setScreenHeight(2712);
        poco.setScreenDensity(440);
        poco.setCpuAbi("arm64-v8a");
        poco.setCpuAbiList("arm64-v8a");
        poco.setCpuAbiList64("arm64-v8a");
        poco.setCpuAbiList32("");
        poco.setSocModel("Snapdragon 8s Gen 3");
        poco.setSocManufacturer("Qualcomm");
        poco.setTimezone("Asia/Jakarta");
        poco.setLocale("id-ID");
        poco.applyFallbacks();
        presets.add(new DevicePreset("poco_f6", "POCO", "POCO F6 (24095PCADG)", "Snapdragon 8s Gen 3 - Android 14", poco));

        // 4. Xiaomi 13 (fuxi)
        DeviceProfile xiaomi13 = new DeviceProfile();
        xiaomi13.setBrand("Xiaomi");
        xiaomi13.setManufacturer("Xiaomi");
        xiaomi13.setModel("2211133G");
        xiaomi13.setProductName("fuxi");
        xiaomi13.setDeviceCode("fuxi");
        xiaomi13.setBoard("fuxi");
        xiaomi13.setHardware("qcom");
        xiaomi13.setBoardPlatform("kalama");
        xiaomi13.setBuildFingerprint("Xiaomi/fuxi_eea/fuxi:14/UKQ1.230804.001/V816.0.1.0.UMCEUXM:user/release-keys");
        xiaomi13.setBuildId("UKQ1.230804.001");
        xiaomi13.setBuildDisplayId("UKQ1.230804.001");
        xiaomi13.setBuildIncremental("V816.0.1.0.UMCEUXM");
        xiaomi13.setBuildRelease("14");
        xiaomi13.setBuildSdk(34);
        xiaomi13.setSecurityPatch("2024-01-01");
        xiaomi13.setScreenWidth(1080);
        xiaomi13.setScreenHeight(2400);
        xiaomi13.setScreenDensity(440);
        xiaomi13.setCpuAbi("arm64-v8a");
        xiaomi13.setCpuAbiList("arm64-v8a");
        xiaomi13.setCpuAbiList64("arm64-v8a");
        xiaomi13.setCpuAbiList32("");
        xiaomi13.setSocModel("Snapdragon 8 Gen 2");
        xiaomi13.setSocManufacturer("Qualcomm");
        xiaomi13.setTimezone("Europe/Berlin");
        xiaomi13.setLocale("de-DE");
        xiaomi13.applyFallbacks();
        presets.add(new DevicePreset("xiaomi_13", "Xiaomi", "Xiaomi 13 (2211133G)", "Snapdragon 8 Gen 2 - Android 14", xiaomi13));

        return presets;
    }

    public List<DevicePreset> localizeCurrentDevicePreset(Context context, List<DevicePreset> presets) {
        return withCurrentDevicePreset(context, presets);
    }

    private List<DevicePreset> loadFromCache(Context context) {
        File cacheFile = getCacheFile(context);
        if (!cacheFile.exists()) {
            return Collections.emptyList();
        }
        try (InputStream inputStream = new FileInputStream(cacheFile)) {
            JSONArray array = extractPresetArray(readFully(inputStream));
            return parsePresetArray(array);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private void writeCache(Context context, String sourceUrl, JSONArray array) {
        try (FileOutputStream outputStream = new FileOutputStream(getCacheFile(context), false)) {
            JSONObject payload = new JSONObject();
            payload.put(CACHE_KEY_SOURCE_URL, sourceUrl == null ? "" : sourceUrl);
            payload.put(CACHE_KEY_UPDATED_AT, System.currentTimeMillis());
            payload.put(CACHE_KEY_PRESETS, array);
            outputStream.write(payload.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private File getCacheFile(Context context) {
        return new File(context.getFilesDir(), CACHE_NAME);
    }

    private List<DevicePreset> withCurrentDevicePreset(Context context, List<DevicePreset> source) {
        ArrayList<DevicePreset> merged = new ArrayList<>();
        merged.add(createCurrentDevicePreset(context));
        if (source != null) {
            for (DevicePreset preset : source) {
                if (preset == null || CURRENT_DEVICE_PRESET_ID.equals(preset.getId())) {
                    continue;
                }
                merged.add(preset);
            }
        }
        return merged;
    }

    private DevicePreset createCurrentDevicePreset(Context context) {
        DeviceProfile profile = new DeviceProfile();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        profile.setBrand(Build.BRAND);
        profile.setManufacturer(Build.MANUFACTURER);
        profile.setModel(Build.MODEL);
        profile.setProductName(Build.PRODUCT);
        profile.setDeviceCode(Build.DEVICE);
        profile.setBoard(Build.BOARD);
        profile.setHardware(Build.HARDWARE);
        profile.setBuildFingerprint(Build.FINGERPRINT);
        profile.setBuildId(Build.ID);
        profile.setBuildDisplayId(Build.DISPLAY);
        profile.setBuildIncremental(Build.VERSION.INCREMENTAL);
        profile.setBuildRelease(Build.VERSION.RELEASE);
        profile.setBuildSdk(Build.VERSION.SDK_INT);
        profile.setBuildDescription(Build.TYPE);
        profile.setBuildFlavor(Build.TYPE);
        profile.setBuildProduct(Build.PRODUCT);
        profile.setBuildCharacteristics(isTablet(metrics) ? "tablet" : "nosdcard");
        profile.setScreenWidth(metrics.widthPixels);
        profile.setScreenHeight(metrics.heightPixels);
        profile.setScreenDensity(metrics.densityDpi);
        profile.setTimezone(TimeZone.getDefault().getID());
        profile.setLocale(Locale.getDefault().toLanguageTag());
        profile.setUserAgent(buildDefaultUserAgent(profile));
        profile.setBootloader(Build.BOOTLOADER);
        profile.setCpuAbi(Build.CPU_ABI);
        profile.setCpuAbiList(joinAbis(Build.SUPPORTED_ABIS));
        profile.setCpuAbiList64(joinAbis(Build.SUPPORTED_64_BIT_ABIS));
        profile.setCpuAbiList32(joinAbis(Build.SUPPORTED_32_BIT_ABIS));
        profile.setSocModel(readBuildField("SOC_MODEL"));
        profile.setSocManufacturer(readBuildField("SOC_MANUFACTURER"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            profile.setSecurityPatch(Build.VERSION.SECURITY_PATCH);
        }

        try {
            TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
            if (telephonyManager != null) {
                profile.setOperatorAlpha(telephonyManager.getNetworkOperatorName());
                profile.setOperatorNumeric(telephonyManager.getNetworkOperator());
                profile.setSimOperatorAlpha(telephonyManager.getSimOperatorName());
                profile.setSimOperatorNumeric(telephonyManager.getSimOperator());
                profile.setSimCountryIso(telephonyManager.getSimCountryIso());
            }
        } catch (Throwable ignored) {
        }

        profile.applyFallbacks();
        return new DevicePreset(
            CURRENT_DEVICE_PRESET_ID,
            context.getString(R.string.preset_current_device_brand),
            firstNonBlank(profile.getModel(), context.getString(R.string.preset_current_device_model)),
            context.getString(R.string.preset_current_device_summary),
            profile
        );
    }

    private JSONArray extractPresetArray(String rawJson) throws Exception {
        String trimmed = rawJson == null ? "" : rawJson.trim();
        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed);
        }
        JSONObject payload = new JSONObject(trimmed);
        JSONArray presets = payload.optJSONArray(CACHE_KEY_PRESETS);
        return presets == null ? new JSONArray() : presets;
    }

    private JSONArray fetchRemotePresetArray(String sourceUrl) throws Exception {
        String apiUrl = toGithubContentsApiUrl(sourceUrl);
        JSONArray contents = new JSONArray(readUrl(apiUrl));
        JSONArray normalizedArray = new JSONArray();

        for (int index = 0; index < contents.length(); index++) {
            JSONObject item = contents.getJSONObject(index);
            if (!"file".equalsIgnoreCase(item.optString("type"))) {
                continue;
            }
            String name = item.optString("name");
            if (!name.toLowerCase(Locale.US).endsWith(".json")) {
                continue;
            }

            String downloadUrl = item.optString("download_url");
            if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
                continue;
            }

            JSONObject sourceJson = new JSONObject(readUrl(downloadUrl));
            normalizedArray.put(normalizePresetJson(name, sourceJson));
        }

        return normalizedArray;
    }

    private JSONObject normalizePresetJson(String fileName, JSONObject sourceJson) throws Exception {
        String fileBaseName = fileName.replaceFirst("(?i)\\.json$", "").trim();
        JSONObject profileJson = sourceJson.optJSONObject("profile");
        if (profileJson == null) {
            profileJson = sourceJson;
        }

        String manufacturer = firstNonBlank(
            profileJson.optString("manufacturer"),
            profileJson.optString("brand")
        );
        String model = firstNonBlank(
            profileJson.optString("model"),
            fileBaseName
        );
        String brandLabel = firstNonBlank(sourceJson.optString("brandLabel"), manufacturer);
        String modelLabel = firstNonBlank(sourceJson.optString("modelLabel"), model);

        JSONObject normalized = new JSONObject();
        normalized.put("id", firstNonBlank(sourceJson.optString("id"), slugify(fileBaseName)));
        normalized.put("brandLabel", brandLabel);
        normalized.put("modelLabel", modelLabel);
        normalized.put("summary", firstNonBlank(sourceJson.optString("summary"), buildSummary(profileJson)));
        normalized.put("profile", profileJson);
        return normalized;
    }

    private List<DevicePreset> parsePresetArray(JSONArray array) {
        List<DevicePreset> presets = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                continue;
            }
            JSONObject profileJson = item.optJSONObject("profile");
            if (profileJson == null) {
                continue;
            }
            DeviceProfile profile = readProfile(profileJson);
            profile.applyFallbacks();
            presets.add(new DevicePreset(
                item.optString("id", slugify(item.optString("modelLabel"))),
                item.optString("brandLabel", firstNonBlank(profile.getManufacturer(), profile.getBrand())),
                item.optString("modelLabel", profile.getModel()),
                item.optString("summary"),
                profile
            ));
        }
        return presets;
    }

    private String toGithubContentsApiUrl(String sourceUrl) throws Exception {
        String trimmed = AppSettingsStore.DEFAULT_PRESET_SOURCE_URL.equals(sourceUrl)
            ? sourceUrl
            : sourceUrl.trim();
        if (trimmed.startsWith("https://api.github.com/repos/")) {
            if (trimmed.endsWith("/contents")) {
                return trimmed;
            }
            if (trimmed.contains("/contents/")) {
                return trimmed;
            }
        }

        String normalized = trimmed
            .replace("git@github.com:", "https://github.com/")
            .replace(".git", "");
        if (!normalized.startsWith("https://github.com/")) {
            throw new IllegalArgumentException("Only GitHub repository URLs are supported.");
        }

        String[] parts = normalized.substring("https://github.com/".length()).split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub repository URL.");
        }

        String owner = parts[0];
        String repo = parts[1];
        StringBuilder builder = new StringBuilder("https://api.github.com/repos/")
            .append(owner)
            .append("/")
            .append(repo)
            .append("/contents");

        if (parts.length > 4 && "tree".equals(parts[2])) {
            String branch = parts[3];
            StringBuilder pathBuilder = new StringBuilder();
            for (int i = 4; i < parts.length; i++) {
                if (pathBuilder.length() > 0) {
                    pathBuilder.append("/");
                }
                pathBuilder.append(parts[i]);
            }
            builder.append("/").append(pathBuilder);
            builder.append("?ref=").append(URLEncoder.encode(branch, StandardCharsets.UTF_8.name()));
        }

        return builder.toString();
    }

    private String readUrl(String urlValue) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlValue).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "LineSpoofer");
        connection.setInstanceFollowRedirects(true);
        try (InputStream inputStream = connection.getInputStream()) {
            return readFully(inputStream);
        } finally {
            connection.disconnect();
        }
    }

    private String readFully(InputStream inputStream) throws Exception {
        try (InputStream stream = inputStream;
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        }
    }

    private String buildSummary(JSONObject profileJson) {
        String soc = firstNonBlank(
            profileJson.optString("socModel"),
            profileJson.optString("boardPlatform")
        );
        String release = firstNonBlank(profileJson.optString("buildRelease"), "Android");
        if (soc.isEmpty()) {
            return "Android " + release;
        }
        return soc + " - Android " + release;
    }

    private String buildDefaultUserAgent(DeviceProfile profile) {
        return String.format(
            Locale.US,
            "Mozilla/5.0 (Linux; Android %s; %s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            firstNonBlank(profile.getBuildRelease(), String.valueOf(Build.VERSION.SDK_INT)),
            firstNonBlank(profile.getModel(), "Android")
        );
    }

    private boolean isTablet(DisplayMetrics metrics) {
        if (metrics == null || metrics.densityDpi <= 0) {
            return false;
        }
        int smallestWidthDp = Math.round((Math.min(metrics.widthPixels, metrics.heightPixels) * 160f) / metrics.densityDpi);
        return smallestWidthDp >= 600;
    }

    private String joinAbis(String[] abis) {
        if (abis == null || abis.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String abi : abis) {
            if (abi == null || abi.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(abi.trim());
        }
        return builder.toString();
    }

    private String readBuildField(String fieldName) {
        try {
            Object value = Build.class.getField(fieldName).get(null);
            return value instanceof String ? (String) value : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private String slugify(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.US);
        return normalized.replace(" ", "_").replace("-", "_");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private DeviceProfile readProfile(JSONObject jsonObject) {
        DeviceProfile profile = new DeviceProfile();
        profile.setBrand(jsonObject.optString("brand"));
        profile.setManufacturer(jsonObject.optString("manufacturer"));
        profile.setModel(jsonObject.optString("model"));
        profile.setProductName(jsonObject.optString("productName"));
        profile.setDeviceCode(jsonObject.optString("deviceCode"));
        profile.setBoard(jsonObject.optString("board"));
        profile.setHardware(jsonObject.optString("hardware"));
        profile.setBoardPlatform(jsonObject.optString("boardPlatform"));
        profile.setBuildFingerprint(jsonObject.optString("buildFingerprint"));
        profile.setBuildId(jsonObject.optString("buildId"));
        profile.setBuildDisplayId(jsonObject.optString("buildDisplayId"));
        profile.setBuildIncremental(jsonObject.optString("buildIncremental"));
        profile.setBuildRelease(jsonObject.optString("buildRelease"));
        profile.setBuildSdk(jsonObject.optInt("buildSdk"));
        profile.setSecurityPatch(jsonObject.optString("securityPatch"));
        profile.setBuildDescription(jsonObject.optString("buildDescription"));
        profile.setBuildFlavor(jsonObject.optString("buildFlavor"));
        profile.setBuildProduct(jsonObject.optString("buildProduct"));
        profile.setBuildCharacteristics(jsonObject.optString("buildCharacteristics"));
        profile.setScreenWidth(jsonObject.optInt("screenWidth"));
        profile.setScreenHeight(jsonObject.optInt("screenHeight"));
        profile.setScreenDensity(jsonObject.optInt("screenDensity"));
        profile.setOperatorAlpha(jsonObject.optString("operatorAlpha"));
        profile.setOperatorNumeric(jsonObject.optString("operatorNumeric"));
        profile.setSimOperatorAlpha(jsonObject.optString("simOperatorAlpha"));
        profile.setSimOperatorNumeric(jsonObject.optString("simOperatorNumeric"));
        profile.setSimCountryIso(jsonObject.optString("simCountryIso"));
        profile.setTimezone(jsonObject.optString("timezone"));
        profile.setLocale(jsonObject.optString("locale"));
        profile.setUserAgent(jsonObject.optString("userAgent"));
        profile.setSerialNumber(jsonObject.optString("serialNumber"));
        profile.setBootloader(jsonObject.optString("bootloader"));
        profile.setAndroidId(jsonObject.optString("androidId"));
        profile.setCpuAbi(jsonObject.optString("cpuAbi"));
        profile.setCpuAbiList(jsonObject.optString("cpuAbiList"));
        profile.setCpuAbiList64(jsonObject.optString("cpuAbiList64"));
        profile.setCpuAbiList32(jsonObject.optString("cpuAbiList32"));
        profile.setSocModel(jsonObject.optString("socModel"));
        profile.setSocManufacturer(jsonObject.optString("socManufacturer"));
        return profile;
    }
}
