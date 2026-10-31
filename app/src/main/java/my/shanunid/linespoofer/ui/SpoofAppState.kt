package my.shanunid.linespoofer.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaDrm
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.drawable.toBitmap
import my.shanunid.linespoofer.MainActivity
import my.shanunid.linespoofer.R
import my.shanunid.linespoofer.data.AppProfileStore
import my.shanunid.linespoofer.data.AppProfileSession
import my.shanunid.linespoofer.data.AppSettingsStore
import my.shanunid.linespoofer.data.ConfigFileManager
import my.shanunid.linespoofer.data.DevicePreset
import my.shanunid.linespoofer.data.DevicePresetCatalog
import my.shanunid.linespoofer.data.DeviceProfile
import my.shanunid.linespoofer.utils.ConfigManager
import my.shanunid.linespoofer.utils.RandomGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.LinkedHashMap
import java.util.Locale
import java.util.UUID

class SpoofAppState(val activity: AppCompatActivity) {

    private val configFileManager = ConfigFileManager()
    private val appProfileStore = AppProfileStore(configFileManager)
    private val presetCatalog = DevicePresetCatalog()

    var initialized by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var presets by mutableStateOf<List<DevicePreset>>(emptyList())
        private set
    var loadedConfig by mutableStateOf<ConfigFileManager.LoadedConfig?>(null)
        private set
    var profileState by mutableStateOf<AppProfileStore.State?>(null)
        private set
    var activeProfileId by mutableStateOf<String?>(null)
        private set
    var hideSystemAppsInAppSettings by mutableStateOf(true)
        private set
    var installedApps by mutableStateOf(cachedInstalledApps.orEmpty())
        private set
    var installedAppsLoaded by mutableStateOf(cachedInstalledApps != null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var themeMode by mutableStateOf(AppSettingsStore.getThemeMode(activity))
        private set
    var languageMode by mutableStateOf(AppSettingsStore.getLanguageMode(activity))
        private set
    var systemColorsEnabled by mutableStateOf(AppSettingsStore.isSystemColorEnabled(activity))
        private set
    var colorStyle by mutableStateOf(AppSettingsStore.getColorStyle(activity))
        private set
    var isMasterSpoofActive by mutableStateOf(AppSettingsStore.isMasterSpoofActive(activity))
        private set

    val editor = ProfileEditorState()

    fun updateThemeMode(value: String) {
        AppSettingsStore.setThemeMode(activity, value)
        themeMode = AppSettingsStore.getThemeMode(activity)
    }

    fun updateLanguageMode(value: String) {
        AppSettingsStore.setLanguageMode(activity, value)
        languageMode = AppSettingsStore.getLanguageMode(activity)
        presets = presetCatalog.localizeCurrentDevicePreset(localizedContext(), presets)
        try {
            if (value == AppSettingsStore.LANGUAGE_DEFAULT) {
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                    androidx.core.os.LocaleListCompat.getEmptyLocaleList()
                )
            } else {
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                    androidx.core.os.LocaleListCompat.forLanguageTags(value)
                )
            }
        } catch (_: Throwable) {
        }
    }

    fun updateSystemColors(enabled: Boolean) {
        AppSettingsStore.setSystemColorEnabled(activity, enabled)
        systemColorsEnabled = enabled
    }

    fun updateColorStyle(value: String) {
        AppSettingsStore.setColorStyle(activity, value)
        colorStyle = AppSettingsStore.getColorStyle(activity)
    }

    suspend fun initialize() {
        if (initialized || loading) {
            return
        }
        loading = true
        try {
            val result = withContext(Dispatchers.IO) {
                val localizedContext = localizedContext()
                val loadedPresets = presetCatalog.load(localizedContext)
                val storedProfiles = AppProfileSession.ensureLoaded(
                    activity,
                    appProfileStore,
                    loadedPresets,
                    localizedContext.getString(R.string.profile_default_name),
                )
                AppProfileStore.syncToRemotePreferences(activity, storedProfiles)
                val profile = storedProfiles.profiles.first()
                val config = configFileManager.loadContent(
                    appProfileStore.getStoreFile(activity),
                    profile.content,
                    loadedPresets,
                )
                Triple(loadedPresets, storedProfiles, config)
            }
            presets = result.first
            profileState = result.second
            loadedConfig = result.third
            activeProfileId = result.second.profiles.first().id
            editor.reset(result.third)
            initialized = true
        } catch (exception: Exception) {
            message = exception.message
        } finally {
            loading = false
        }
    }

    suspend fun saveEditor(profileName: String): String? {
        val storedProfiles = profileState ?: return null
        val profileId = activeProfileId ?: return null
        val currentConfig = loadedConfig ?: return null
        val normalizedProfileName = profileName.trim()
        if (normalizedProfileName.isEmpty()) {
            return null
        }
        loading = true
        try {
            val result = withContext(Dispatchers.IO) {
                val extraProperties = editor.buildExtraProperties(currentConfig.extraProperties).toMutableMap()
                extraProperties.remove(ConfigManager.KEY_APPLY_SCREEN_METRICS)
                extraProperties.remove(ConfigManager.KEY_SAFE_MODE_PACKAGES)
                val content = configFileManager.render(
                    editor.buildProfile(currentConfig.profile),
                    extraProperties,
                    editor.selectedPresetId,
                    editor.customMode,
                )
                val profiles = storedProfiles.profiles.map { profile ->
                    if (profile.id == profileId) {
                        AppProfileStore.Profile(
                            profile.id,
                            normalizedProfileName,
                            content,
                        )
                    } else {
                        profile
                    }
                }
                val updatedState = AppProfileStore.State(
                    profiles,
                    storedProfiles.assignments,
                    storedProfiles.globalProperties,
                )
                appProfileStore.save(activity, updatedState)
                AppProfileSession.update(updatedState)
                val config = configFileManager.loadContent(
                    appProfileStore.getStoreFile(activity),
                    content,
                    presets,
                )
                updatedState to config
            }
            profileState = result.first
            loadedConfig = result.second
            editor.reset(result.second)
            try {
                my.shanunid.linespoofer.utils.RootScopeManager.syncInBackground(
                    activity,
                    result.first.assignments.keys
                )
                val affectedPackages = result.first.assignments.filter { it.value == profileId }.keys
                if (affectedPackages.isNotEmpty()) {
                    my.shanunid.linespoofer.utils.RootScopeManager.forceStop(affectedPackages)
                }
            } catch (_: Throwable) {
            }
            val profileName = result.first.findProfile(profileId)?.name.orEmpty()
            message = localizedContext().getString(R.string.profile_saved, profileName)
            return profileId
        } catch (exception: Exception) {
            message = localizedContext().getString(R.string.save_failed) + " " + exception.message
            return null
        } finally {
            loading = false
        }
    }

    suspend fun selectProfile(profileId: String): Boolean {
        return loadProfile(profileId)
    }

    suspend fun createProfile(name: String): String? {
        val storedProfiles = profileState ?: return null
        val currentConfig = loadedConfig ?: return null
        val profileName = name.trim()
        if (profileName.isEmpty()) {
            return null
        }
        loading = true
        try {
            val result = withContext(Dispatchers.IO) {
                val extraProperties = editor.buildExtraProperties(currentConfig.extraProperties).toMutableMap()
                extraProperties.remove(ConfigManager.KEY_APPLY_SCREEN_METRICS)
                extraProperties.remove(ConfigManager.KEY_SAFE_MODE_PACKAGES)
                val content = configFileManager.render(
                    editor.buildProfile(currentConfig.profile),
                    extraProperties,
                    editor.selectedPresetId,
                    editor.customMode,
                )
                val profile = AppProfileStore.Profile(UUID.randomUUID().toString(), profileName, content)
                val profiles = storedProfiles.profiles + profile
                val updatedState = AppProfileStore.State(
                    profiles,
                    storedProfiles.assignments,
                    storedProfiles.globalProperties,
                )
                appProfileStore.save(activity, updatedState)
                AppProfileSession.update(updatedState)
                val config = configFileManager.loadContent(
                    appProfileStore.getStoreFile(activity),
                    content,
                    presets,
                )
                Triple(updatedState, profile.id, config)
            }
            profileState = result.first
            activeProfileId = result.second
            loadedConfig = result.third
            editor.reset(result.third)
            message = localizedContext().getString(R.string.profile_created, profileName)
            return result.second
        } catch (exception: Exception) {
            message = localizedContext().getString(R.string.save_failed) + " " + exception.message
            return null
        } finally {
            loading = false
        }
    }

    fun assignedProfileName(packageName: String): String? {
        val storedProfiles = profileState ?: return null
        val profileId = storedProfiles.assignments[packageName] ?: return null
        return storedProfiles.findProfile(profileId)?.name
    }

    fun profileAssignmentCount(profileId: String): Int {
        return profileState?.assignments?.values?.count { it == profileId } ?: 0
    }

    suspend fun deleteProfile(profileId: String): Boolean {
        val storedProfiles = profileState ?: return false
        if (storedProfiles.profiles.size <= 1) {
            return false
        }
        val profile = storedProfiles.findProfile(profileId) ?: return false
        loading = true
        try {
            val updatedState = withContext(Dispatchers.IO) {
                val profiles = storedProfiles.profiles.filterNot { it.id == profileId }
                val assignments = LinkedHashMap(storedProfiles.assignments).apply {
                    entries.removeAll { it.value == profileId }
                }
                val state = AppProfileStore.State(
                    profiles,
                    assignments,
                    storedProfiles.globalProperties,
                )
                appProfileStore.save(activity, state)
                AppProfileSession.update(state)
                state
            }
            profileState = updatedState
            if (activeProfileId == profileId) {
                loadProfile(updatedState.profiles.first().id)
            }
            message = localizedContext().getString(R.string.profile_deleted, profile.name)
            return true
        } catch (exception: Exception) {
            message = localizedContext().getString(R.string.profile_delete_failed) + " " + exception.message
            return false
        } finally {
            loading = false
        }
    }

    fun toggleSystemAppsInAppSettings() {
        hideSystemAppsInAppSettings = !hideSystemAppsInAppSettings
    }

    suspend fun ensureInstalledAppsLoaded() {
        if (installedAppsLoaded) {
            return
        }
        installedApps = loadInstalledApps()
        installedAppsLoaded = true
    }

    suspend fun reloadProfileState(profileId: String? = activeProfileId) {
        val storedProfiles = AppProfileSession.current() ?: return
        profileState = storedProfiles
        if (profileId != null && storedProfiles.findProfile(profileId) != null) {
            loadProfile(profileId)
        }
    }

    private suspend fun loadProfile(profileId: String): Boolean {
        val storedProfiles = profileState ?: return false
        val profile = storedProfiles.findProfile(profileId) ?: return false
        val config = withContext(Dispatchers.IO) {
            configFileManager.loadContent(
                appProfileStore.getStoreFile(activity),
                profile.content,
                presets,
            )
        }
        activeProfileId = profile.id
        loadedConfig = config
        editor.reset(config)
        return true
    }

    suspend fun refreshRemotePresets(userInitiated: Boolean) {
        val sourceUrl = AppSettingsStore.getPresetSourceUrl(activity)
        val remotePresets = withContext(Dispatchers.IO) {
            presetCatalog.refreshRemote(localizedContext(), sourceUrl)
        }
        if (remotePresets.isNotEmpty()) {
            presets = remotePresets
            if (userInitiated) {
                message = localizedContext().getString(R.string.settings_preset_source_updated)
            }
        } else if (userInitiated) {
            message = localizedContext().getString(R.string.settings_preset_source_empty)
        }
    }

    suspend fun updatePresetSource(sourceUrl: String) {
        AppSettingsStore.setPresetSourceUrl(activity, sourceUrl)
        refreshRemotePresets(true)
    }

    suspend fun updateScreenMetrics(enabled: Boolean) {
        val storedProfiles = profileState ?: return
        val globalProperties = LinkedHashMap(storedProfiles.globalProperties)
        globalProperties[ConfigManager.KEY_APPLY_SCREEN_METRICS] = enabled.toString()
        val updatedState = AppProfileStore.State(
            storedProfiles.profiles,
            storedProfiles.assignments,
            globalProperties,
        )
        withContext(Dispatchers.IO) {
            appProfileStore.save(activity, updatedState)
            AppProfileSession.update(updatedState)
        }
        profileState = updatedState
    }

    fun isScreenMetricsEnabled(): Boolean {
        val value = profileState?.globalProperties?.get(ConfigManager.KEY_APPLY_SCREEN_METRICS)
        return value == "1" || value.equals("true", true)
    }

    fun safeModePackages(): Set<String> {
        val value = profileState?.globalProperties?.get(ConfigManager.KEY_SAFE_MODE_PACKAGES).orEmpty()
        return value.split(',', '\n').map(String::trim).filter(String::isNotEmpty).toSet()
    }

    suspend fun updateSafeModePackages(packageNames: Set<String>) {
        val storedProfiles = profileState ?: return
        val globalProperties = LinkedHashMap(storedProfiles.globalProperties)
        if (packageNames.isEmpty()) {
            globalProperties.remove(ConfigManager.KEY_SAFE_MODE_PACKAGES)
        } else {
            globalProperties[ConfigManager.KEY_SAFE_MODE_PACKAGES] = packageNames.joinToString(",")
        }
        val updatedState = AppProfileStore.State(
            storedProfiles.profiles,
            storedProfiles.assignments,
            globalProperties,
        )
        withContext(Dispatchers.IO) {
            appProfileStore.save(activity, updatedState)
            AppProfileSession.update(updatedState)
        }
        profileState = updatedState
    }

    suspend fun loadInstalledApps(): List<SafeAppEntry> = installedAppsMutex.withLock {
        cachedInstalledApps?.let { return it }
        return withContext(Dispatchers.IO) {
            val packageManager = activity.packageManager
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }
            val result = ArrayList<SafeAppEntry>()
            val placeholderIcon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            packages.forEach { packageInfo ->
                val applicationInfo = packageInfo.applicationInfo ?: return@forEach
                val packageName = packageInfo.packageName
                if (packageName == activity.packageName || packageName == "android") {
                    return@forEach
                }
                result.add(
                    SafeAppEntry(
                        label = applicationInfo.loadLabel(packageManager).toString().ifBlank { packageName },
                        packageName = packageName,
                        versionName = packageInfo.versionName.orEmpty(),
                        systemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                        icon = placeholderIcon,
                    )
                )
            }

            // Muat aplikasi Dual/Clone Apps (User 999) pada HyperOS
            try {
                loadUser999Packages().forEach { (pkgName, label) ->
                    val defaultIcon = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                    result.add(
                        SafeAppEntry(
                            label = "$label [Clone]",
                            packageName = "$pkgName#999",
                            versionName = "",
                            systemApp = false,
                            icon = defaultIcon,
                        )
                    )
                }
            } catch (ignored: Throwable) {
            }

            val sorted = result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            cachedInstalledApps = sorted

            // Background pre-warm icon cache so LazyColumn scrolling achieves 120 FPS without binder IPC stalls
            CoroutineScope(Dispatchers.IO).launch {
                for (app in sorted) {
                    if (AppIconLoader.getCached(app.packageName) == null) {
                        AppIconLoader.loadIcon(activity, app.packageName)
                    }
                }
            }

            sorted
        }
    }

    private fun loadUser999Packages(): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        try {
            val method = activity.packageManager.javaClass.getMethod(
                "getInstalledPackagesAsUser",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            @Suppress("UNCHECKED_CAST")
            val list = method.invoke(activity.packageManager, 0, 999) as? List<PackageInfo>
            if (!list.isNullOrEmpty()) {
                list.forEach { pkg ->
                    val label = pkg.applicationInfo?.loadLabel(activity.packageManager)?.toString() ?: pkg.packageName
                    result[pkg.packageName] = label
                }
                return result
            }
        } catch (ignored: Throwable) {
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm list packages --user 999"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val clean = line?.trim()?.removePrefix("package:") ?: continue
                if (clean.isNotEmpty() && clean != "android") {
                    result[clean] = clean
                }
            }
            process.waitFor()
        } catch (ignored: Throwable) {
        }
        return result
    }

    fun forceStopApp(packageName: String) {
        val cleanPkg = packageName.substringBefore("#")
        my.shanunid.linespoofer.utils.RootScopeManager.forceStop(cleanPkg)
    }

    fun updateMasterSpoofActive(active: Boolean) {
        AppSettingsStore.setMasterSpoofActive(activity, active)
        isMasterSpoofActive = active
        try {
            val prefs = activity.getSharedPreferences("linespoofer_config", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("master_spoof_active", active).apply()
        } catch (ignored: Throwable) {
        }

        // Synchronize updated master spoof state to Vector's RemotePreferences and SQLite DB
        profileState?.let { state ->
            AppProfileStore.syncToRemotePreferences(activity, state)
        }

        val targets = profileState?.assignments?.keys.orEmpty()
        if (targets.isNotEmpty()) {
            my.shanunid.linespoofer.utils.RootScopeManager.forceStop(targets)
        }

        if (active) {
            val count = targets.size
            message = if (count > 0) {
                activity.getString(R.string.target_app_force_stopped, "$count target aplikasi")
            } else {
                "Spoofing aktif (tambahkan aplikasi ke profil untuk menerapkan)"
            }
        } else {
            message = "Spoofing dinonaktifkan (target aplikasi dihentikan)"
        }
    }

    suspend fun populateAdvancedDefaults() {
        val currentValues = withContext(Dispatchers.IO) {
            mapOf(
                ConfigManager.FIELD_ANDROID_ID to currentAndroidId(),
                ConfigManager.FIELD_IMEI to currentImei(),
                ConfigManager.FIELD_IMEI_2 to currentImei2(),
                ConfigManager.FIELD_MEID to currentMeid(),
                ConfigManager.FIELD_IMSI to currentImsi(),
                ConfigManager.FIELD_ICCID to currentIccid(),
                ConfigManager.FIELD_PHONE_NUMBER to currentPhoneNumber(),
                ConfigManager.FIELD_PHONE_NUMBER_2 to currentPhoneNumber2(),
                ConfigManager.FIELD_GAID to currentGaid(),
                ConfigManager.FIELD_GSF_ID to currentGsfId(),
                ConfigManager.FIELD_MEDIA_DRM_ID to currentMediaDrmId(),
                ConfigManager.FIELD_APP_SET_ID to currentAppSetId(),
            )
        }
        currentValues.forEach { (fieldId, value) ->
            if (editor.advancedValues[fieldId].isNullOrBlank() && !value.isNullOrBlank()) {
                editor.advancedValues[fieldId] = value
            }
        }
    }

    fun presetLabel(presetId: String?): String {
        return presets.firstOrNull { it.id == presetId }?.displayName
            ?: localizedContext().getString(R.string.preset_unknown)
    }

    fun isModuleActivated(): Boolean {
        return (activity as? MainActivity)?.isModuleActivated() == true
    }

    fun consumeMessage() {
        message = null
    }

    private fun localizedContext(): Context {
        if (languageMode == AppSettingsStore.LANGUAGE_DEFAULT) {
            return activity
        }
        val locales = LocaleList.forLanguageTags(languageMode)
        val configuration = Configuration(activity.resources.configuration).apply {
            setLocales(locales)
            setLayoutDirection(locales[0])
        }
        return activity.createConfigurationContext(configuration)
    }

    @SuppressLint("MissingPermission")
    private fun currentImei(): String? = runCatching {
        val manager = activity.getSystemService(TelephonyManager::class.java) ?: return null
        manager.imei
    }.getOrNull()

    @SuppressLint("MissingPermission")
    private fun currentImei2(): String? = runCatching {
        val manager = activity.getSystemService(TelephonyManager::class.java) ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            manager.getImei(1)
        } else {
            null
        }
    }.getOrNull()

    @SuppressLint("HardwareIds")
    private fun currentAndroidId(): String? = Settings.Secure.getString(
        activity.contentResolver,
        Settings.Secure.ANDROID_ID,
    )

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun currentMeid(): String? = runCatching {
        val manager = activity.getSystemService(TelephonyManager::class.java) ?: return null
        manager.meid
    }.getOrNull()

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun currentImsi(): String? = runCatching {
        activity.getSystemService(TelephonyManager::class.java)?.subscriberId
    }.getOrNull()

    @SuppressLint("MissingPermission")
    private fun currentIccid(): String? = runCatching {
        activity.getSystemService(SubscriptionManager::class.java)
            ?.activeSubscriptionInfoList
            ?.firstOrNull()
            ?.iccId
    }.getOrNull()

    @SuppressLint("HardwareIds", "MissingPermission")
    @Suppress("DEPRECATION")
    private fun currentPhoneNumber(): String? = runCatching {
        activity.getSystemService(TelephonyManager::class.java)?.line1Number
    }.getOrNull()

    @SuppressLint("HardwareIds", "MissingPermission")
    @Suppress("DEPRECATION")
    private fun currentPhoneNumber2(): String? = runCatching {
        val tm = activity.getSystemService(TelephonyManager::class.java) ?: return null
        val sm = activity.getSystemService(SubscriptionManager::class.java)
        val subs = sm?.activeSubscriptionInfoList
        if (subs != null && subs.size > 1) {
            val sub2 = subs[1]
            val num = sub2.number
            if (!num.isNullOrBlank()) {
                return num
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return tm.createForSubscriptionId(sub2.subscriptionId)?.line1Number
            }
        }
        null
    }.getOrNull()

    private fun currentGsfId(): String? {
        var cursor: Cursor? = null
        return try {
            cursor = activity.contentResolver.query(
                Uri.parse("content://com.google.android.gsf.gservices"),
                null,
                null,
                arrayOf("android_id"),
                null,
            )
            if (cursor != null && cursor.moveToFirst() && cursor.columnCount >= 2) cursor.getString(1) else null
        } catch (_: Throwable) {
            null
        } finally {
            cursor?.close()
        }
    }

    private fun currentMediaDrmId(): String? = runCatching {
        val mediaDrm = MediaDrm(WIDEVINE_UUID)
        try {
            mediaDrm.getPropertyByteArray("deviceUniqueId").toHex()
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mediaDrm.close()
            } else {
                @Suppress("DEPRECATION")
                mediaDrm.release()
            }
        }
    }.getOrNull()

    private fun currentGaid(): String? = runCatching {
        val clientClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
        val info = clientClass.getMethod("getAdvertisingIdInfo", Context::class.java).invoke(null, activity)
        info?.javaClass?.getMethod("getId")?.invoke(info) as? String
    }.getOrNull()

    private fun currentAppSetId(): String? = runCatching {
        val appSetClass = Class.forName("com.google.android.gms.appset.AppSet")
        val client = appSetClass.getMethod("getClient", Context::class.java).invoke(null, activity)
        val task = client.javaClass.getMethod("getAppSetIdInfo").invoke(client)
        val taskClass = Class.forName("com.google.android.gms.tasks.Task")
        val tasksClass = Class.forName("com.google.android.gms.tasks.Tasks")
        val info = tasksClass.getMethod("await", taskClass).invoke(null, task)
        info?.javaClass?.getMethod("getId")?.invoke(info) as? String
    }.getOrNull()

    companion object {
        private val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
        private val installedAppsMutex = Mutex()

        @Volatile
        private var cachedInstalledApps: List<SafeAppEntry>? = null
    }
}

class ProfileEditorState {

    var selectedPresetId by mutableStateOf<String?>(null)
    var customMode by mutableStateOf(false)
    val values = mutableStateMapOf<String, String>()
    val advancedValues = mutableStateMapOf<String, String>()
    val enabledFields = mutableStateMapOf<String, Boolean>()
    var locationLatitude by mutableStateOf("-6.2088")
    var locationLongitude by mutableStateOf("106.8456")

    // Dynamic Spoof Extended Controls (MAC, Uptime, Wi-Fi, Bluetooth, Battery, FPS)
    var macMode by mutableStateOf("random")
    var macAddress by mutableStateOf("")
    var uptimeMode by mutableStateOf("random")
    var uptimeSeconds by mutableStateOf("27720")
    var wifiMode by mutableStateOf("random")
    var wifiSsid by mutableStateOf("\"Home-WiFi-5G\"")
    var wifiBssid by mutableStateOf("")
    var wifiRssi by mutableStateOf("-55")
    var bluetoothMode by mutableStateOf("random")
    var bluetoothName by mutableStateOf("")
    var bluetoothMac by mutableStateOf("")
    var batteryMode by mutableStateOf("random")
    var batteryLevel by mutableStateOf(82f)
    var batteryTemp by mutableStateOf(31.2f)
    var batteryStatus by mutableStateOf("discharging")
    var fpsMode by mutableStateOf("custom")
    var targetFps by mutableStateOf(120f)

    fun reset(config: ConfigFileManager.LoadedConfig) {
        selectedPresetId = config.selectedPresetId
        customMode = config.isCustomMode
        loadProfile(config.profile)
        advancedValues[ConfigManager.FIELD_ANDROID_ID] = config.profile.androidId.orEmpty()
        advancedPropertyKeys.forEach { (fieldId, propertyKey) ->
            advancedValues[fieldId] = config.extraProperties[propertyKey].orEmpty()
        }
        if (advancedValues[ConfigManager.FIELD_PHONE_NUMBER].isNullOrEmpty() && !config.profile.phoneNumberSim1.isNullOrEmpty()) {
            advancedValues[ConfigManager.FIELD_PHONE_NUMBER] = config.profile.phoneNumberSim1
        }
        if (advancedValues[ConfigManager.FIELD_PHONE_NUMBER_2].isNullOrEmpty() && !config.profile.phoneNumberSim2.isNullOrEmpty()) {
            advancedValues[ConfigManager.FIELD_PHONE_NUMBER_2] = config.profile.phoneNumberSim2
        }
        locationLatitude = config.extraProperties["device.location.latitude"] ?: "-6.2088"
        locationLongitude = config.extraProperties["device.location.longitude"] ?: "106.8456"

        macMode = config.extraProperties["device.mac.mode"] ?: "random"
        macAddress = config.extraProperties["device.mac.address"].orEmpty()
        uptimeMode = config.extraProperties["device.uptime.mode"] ?: "random"
        uptimeSeconds = config.extraProperties["device.uptime.seconds"] ?: "27720"
        wifiMode = config.extraProperties["device.wifi.mode"] ?: "random"
        wifiSsid = config.extraProperties["device.wifi.ssid"] ?: "\"Home-WiFi-5G\""
        wifiBssid = config.extraProperties["device.wifi.bssid"].orEmpty()
        wifiRssi = config.extraProperties["device.wifi.rssi"] ?: "-55"
        bluetoothMode = config.extraProperties["device.bluetooth.mode"] ?: "random"
        bluetoothName = config.extraProperties["device.bluetooth.name"].orEmpty()
        bluetoothMac = config.extraProperties["device.bluetooth.mac"].orEmpty()
        batteryMode = config.extraProperties["device.battery.mode"] ?: "random"
        batteryLevel = config.extraProperties["device.battery.level"]?.toFloatOrNull() ?: 82f
        batteryTemp = (config.extraProperties["device.battery.temp"]?.toFloatOrNull() ?: 312f) / 10f
        batteryStatus = config.extraProperties["device.battery.status"] ?: "discharging"
        fpsMode = config.extraProperties["device.fps.mode"] ?: "custom"
        val rawFps = config.extraProperties["device.display.refresh_rate"]?.toFloatOrNull() ?: 120f
        targetFps = when (rawFps.toInt()) {
            30 -> 30f
            60 -> 60f
            90 -> 90f
            144 -> 144f
            else -> 120f
        }

        allFields.forEach { fieldId ->
            val value = config.extraProperties[ConfigManager.getTogglePropertyKey(fieldId)]
            val isRiskGated = fieldId == ConfigManager.FIELD_GPU || fieldId == ConfigManager.FIELD_LOCATION
            enabledFields[fieldId] = if (value == null) {
                !isRiskGated
            } else {
                value == "1" || value.equals("true", true)
            }
        }
    }

    fun selectPreset(preset: DevicePreset) {
        selectedPresetId = preset.id
        customMode = false
        loadProfile(preset.profile)
    }

    fun setCustomMode(enabled: Boolean, presets: List<DevicePreset>) {
        customMode = enabled
        if (!enabled) {
            val preset = presets.firstOrNull { it.id == selectedPresetId } ?: presets.firstOrNull()
            if (preset != null) {
                selectPreset(preset)
            }
        }
    }

    fun buildProfile(baseProfile: DeviceProfile): DeviceProfile {
        return baseProfile.copy().apply {
            brand = value(ConfigManager.FIELD_BRAND)
            manufacturer = value(ConfigManager.FIELD_MANUFACTURER)
            model = value(ConfigManager.FIELD_MODEL)
            deviceCode = value(ConfigManager.FIELD_DEVICE)
            productName = value(ConfigManager.FIELD_PRODUCT)
            board = value(ConfigManager.FIELD_BOARD)
            hardware = value(ConfigManager.FIELD_HARDWARE)
            boardPlatform = value(ConfigManager.FIELD_BOARD_PLATFORM)
            buildRelease = value(ConfigManager.FIELD_ANDROID_RELEASE)
            buildSdk = value(ConfigManager.FIELD_SDK).toIntOrNull() ?: buildSdk
            securityPatch = value(ConfigManager.FIELD_SECURITY_PATCH)
            buildId = value(ConfigManager.FIELD_BUILD_ID)
            buildDisplayId = value(ConfigManager.FIELD_BUILD_DISPLAY_ID)
            buildIncremental = value(ConfigManager.FIELD_BUILD_INCREMENTAL)
            buildFingerprint = value(ConfigManager.FIELD_FINGERPRINT)
            screenWidth = value(ConfigManager.FIELD_SCREEN_WIDTH).toIntOrNull() ?: screenWidth
            screenHeight = value(ConfigManager.FIELD_SCREEN_HEIGHT).toIntOrNull() ?: screenHeight
            screenDensity = value(ConfigManager.FIELD_SCREEN_DENSITY).toIntOrNull() ?: screenDensity
            operatorAlpha = value(ConfigManager.FIELD_OPERATOR_ALPHA)
            operatorNumeric = value(ConfigManager.FIELD_OPERATOR_NUMERIC)
            simOperatorAlpha = operatorAlpha
            simOperatorNumeric = operatorNumeric
            simCountryIso = value(ConfigManager.FIELD_SIM_COUNTRY)
            timezone = value(ConfigManager.FIELD_TIMEZONE)
            locale = value(ConfigManager.FIELD_LOCALE)
            androidId = advancedValue(ConfigManager.FIELD_ANDROID_ID)
            phoneNumberSim1 = advancedValue(ConfigManager.FIELD_PHONE_NUMBER)
            phoneNumberSim2 = advancedValue(ConfigManager.FIELD_PHONE_NUMBER_2)
            phoneNumber = phoneNumberSim1
        }
    }

    fun buildExtraProperties(baseProperties: Map<String, String>): Map<String, String> {
        val result = LinkedHashMap(baseProperties)
        advancedPropertyKeys.forEach { (fieldId, key) ->
            val value = advancedValues[fieldId].orEmpty().trim()
            if (value.isEmpty()) result.remove(key) else result[key] = value
        }
        allFields.forEach { fieldId ->
            val key = ConfigManager.getTogglePropertyKey(fieldId)
            val isRiskGated = fieldId == ConfigManager.FIELD_GPU || fieldId == ConfigManager.FIELD_LOCATION
            val isEnabled = enabledFields[fieldId] ?: (!isRiskGated)
            if (isRiskGated) {
                if (isEnabled) result[key] = "true" else result.remove(key)
            } else {
                if (!isEnabled) result[key] = "false" else result.remove(key)
            }
        }
        result["device.location.latitude"] = locationLatitude.trim()
        result["device.location.longitude"] = locationLongitude.trim()

        result["device.mac.mode"] = macMode
        if (macAddress.isNotBlank()) result["device.mac.address"] = macAddress.trim()
        result["device.uptime.mode"] = uptimeMode
        result["device.uptime.seconds"] = uptimeSeconds.trim()
        result["device.wifi.mode"] = wifiMode
        result["device.wifi.ssid"] = wifiSsid.trim()
        if (wifiBssid.isNotBlank()) result["device.wifi.bssid"] = wifiBssid.trim()
        result["device.wifi.rssi"] = wifiRssi.trim()
        result["device.bluetooth.mode"] = bluetoothMode
        if (bluetoothName.isNotBlank()) result["device.bluetooth.name"] = bluetoothName.trim()
        if (bluetoothMac.isNotBlank()) result["device.bluetooth.mac"] = bluetoothMac.trim()
        result["device.battery.mode"] = batteryMode
        result["device.battery.level"] = batteryLevel.toInt().toString()
        result["device.battery.temp"] = (batteryTemp * 10).toInt().toString()
        result["device.battery.status"] = batteryStatus
        result["device.fps.mode"] = fpsMode
        result["device.display.refresh_rate"] = targetFps.toInt().toString()

        // Sync toggle flags with dynamic environment modes
        result[ConfigManager.getTogglePropertyKey(ConfigManager.FIELD_WIFI)] = if (wifiMode != "off") "true" else "false"
        result[ConfigManager.getTogglePropertyKey(ConfigManager.FIELD_BLUETOOTH)] = if (bluetoothMode != "off") "true" else "false"
        result[ConfigManager.getTogglePropertyKey(ConfigManager.FIELD_BATTERY)] = if (batteryMode != "off") "true" else "false"
        result[ConfigManager.getTogglePropertyKey(ConfigManager.FIELD_UPTIME)] = if (uptimeMode != "off") "true" else "false"
        result[ConfigManager.getTogglePropertyKey(ConfigManager.FIELD_REFRESH_RATE)] = if (fpsMode != "off") "true" else "false"
        result[ConfigManager.getTogglePropertyKey(ConfigManager.FIELD_LOCATION)] = "true"

        return result
    }

    fun randomize(fieldId: String) {
        advancedValues[fieldId] = when (fieldId) {
            ConfigManager.FIELD_ANDROID_ID -> RandomGenerator.generateAndroidId()
            ConfigManager.FIELD_IMEI -> RandomGenerator.generateIMEI()
            ConfigManager.FIELD_IMEI_2 -> RandomGenerator.generateIMEI()
            ConfigManager.FIELD_MEID -> RandomGenerator.generateMEID()
            ConfigManager.FIELD_IMSI -> RandomGenerator.generateIMSI()
            ConfigManager.FIELD_ICCID -> RandomGenerator.generateICCID()
            ConfigManager.FIELD_PHONE_NUMBER -> RandomGenerator.generatePhoneNumber()
            ConfigManager.FIELD_PHONE_NUMBER_2 -> RandomGenerator.generatePhoneNumber()
            ConfigManager.FIELD_GAID -> RandomGenerator.generateGAID()
            ConfigManager.FIELD_GSF_ID -> RandomGenerator.generateGSFId()
            ConfigManager.FIELD_MEDIA_DRM_ID -> RandomGenerator.generateMediaDrmId().toHex()
            ConfigManager.FIELD_APP_SET_ID -> RandomGenerator.generateGAID()
            else -> return
        }
    }

    fun randomizeAll() {
        advancedFields.forEach(::randomize)
    }

    fun clearAdvanced() {
        advancedFields.forEach { advancedValues[it] = "" }
    }

    private fun loadProfile(profile: DeviceProfile) {
        values[ConfigManager.FIELD_BRAND] = profile.brand.orEmpty()
        values[ConfigManager.FIELD_MANUFACTURER] = profile.manufacturer.orEmpty()
        values[ConfigManager.FIELD_MODEL] = profile.model.orEmpty()
        values[ConfigManager.FIELD_DEVICE] = profile.deviceCode.orEmpty()
        values[ConfigManager.FIELD_PRODUCT] = profile.productName.orEmpty()
        values[ConfigManager.FIELD_BOARD] = profile.board.orEmpty()
        values[ConfigManager.FIELD_HARDWARE] = profile.hardware.orEmpty()
        values[ConfigManager.FIELD_BOARD_PLATFORM] = profile.boardPlatform.orEmpty()
        values[ConfigManager.FIELD_ANDROID_RELEASE] = profile.buildRelease.orEmpty()
        values[ConfigManager.FIELD_SDK] = profile.buildSdk.toString()
        values[ConfigManager.FIELD_SECURITY_PATCH] = profile.securityPatch.orEmpty()
        values[ConfigManager.FIELD_BUILD_ID] = profile.buildId.orEmpty()
        values[ConfigManager.FIELD_BUILD_DISPLAY_ID] = profile.buildDisplayId.orEmpty()
        values[ConfigManager.FIELD_BUILD_INCREMENTAL] = profile.buildIncremental.orEmpty()
        values[ConfigManager.FIELD_FINGERPRINT] = profile.buildFingerprint.orEmpty()
        values[ConfigManager.FIELD_SCREEN_WIDTH] = profile.screenWidth.toString()
        values[ConfigManager.FIELD_SCREEN_HEIGHT] = profile.screenHeight.toString()
        values[ConfigManager.FIELD_SCREEN_DENSITY] = profile.screenDensity.toString()
        values[ConfigManager.FIELD_OPERATOR_ALPHA] = profile.operatorAlpha.orEmpty()
        values[ConfigManager.FIELD_OPERATOR_NUMERIC] = profile.operatorNumeric.orEmpty()
        values[ConfigManager.FIELD_SIM_COUNTRY] = profile.simCountryIso.orEmpty()
        values[ConfigManager.FIELD_TIMEZONE] = profile.timezone.orEmpty()
        values[ConfigManager.FIELD_LOCALE] = profile.locale.orEmpty()
        if (!profile.phoneNumberSim1.isNullOrEmpty()) {
            advancedValues[ConfigManager.FIELD_PHONE_NUMBER] = profile.phoneNumberSim1
        }
        if (!profile.phoneNumberSim2.isNullOrEmpty()) {
            advancedValues[ConfigManager.FIELD_PHONE_NUMBER_2] = profile.phoneNumberSim2
        }
    }

    private fun value(fieldId: String): String = values[fieldId].orEmpty().trim()
    private fun advancedValue(fieldId: String): String = advancedValues[fieldId].orEmpty().trim()

    companion object {
        val profileFields = listOf(
            ConfigManager.FIELD_BRAND,
            ConfigManager.FIELD_MANUFACTURER,
            ConfigManager.FIELD_MODEL,
            ConfigManager.FIELD_DEVICE,
            ConfigManager.FIELD_PRODUCT,
            ConfigManager.FIELD_BOARD,
            ConfigManager.FIELD_HARDWARE,
            ConfigManager.FIELD_BOARD_PLATFORM,
            ConfigManager.FIELD_ANDROID_RELEASE,
            ConfigManager.FIELD_SDK,
            ConfigManager.FIELD_SECURITY_PATCH,
            ConfigManager.FIELD_BUILD_ID,
            ConfigManager.FIELD_BUILD_DISPLAY_ID,
            ConfigManager.FIELD_BUILD_INCREMENTAL,
            ConfigManager.FIELD_FINGERPRINT,
            ConfigManager.FIELD_SCREEN_WIDTH,
            ConfigManager.FIELD_SCREEN_HEIGHT,
            ConfigManager.FIELD_SCREEN_DENSITY,
            ConfigManager.FIELD_OPERATOR_ALPHA,
            ConfigManager.FIELD_OPERATOR_NUMERIC,
            ConfigManager.FIELD_SIM_COUNTRY,
            ConfigManager.FIELD_TIMEZONE,
            ConfigManager.FIELD_LOCALE,
        )
        val advancedFields = listOf(
            ConfigManager.FIELD_ANDROID_ID,
            ConfigManager.FIELD_IMEI,
            ConfigManager.FIELD_IMEI_2,
            ConfigManager.FIELD_MEID,
            ConfigManager.FIELD_IMSI,
            ConfigManager.FIELD_ICCID,
            ConfigManager.FIELD_PHONE_NUMBER,
            ConfigManager.FIELD_PHONE_NUMBER_2,
            ConfigManager.FIELD_GAID,
            ConfigManager.FIELD_GSF_ID,
            ConfigManager.FIELD_MEDIA_DRM_ID,
            ConfigManager.FIELD_APP_SET_ID,
        )
        val toggleFields = listOf(
            ConfigManager.FIELD_TELEPHONY,
            ConfigManager.FIELD_BLOCK_LOCAL_SOCKET,
            ConfigManager.FIELD_SENSORS,
            ConfigManager.FIELD_WEBGL,
            ConfigManager.FIELD_BATTERY,
            ConfigManager.FIELD_UPTIME,
            ConfigManager.FIELD_STORAGE,
            ConfigManager.FIELD_AUDIO,
            ConfigManager.FIELD_GPU,
            ConfigManager.FIELD_REFRESH_RATE,
            ConfigManager.FIELD_BLUETOOTH,
            ConfigManager.FIELD_LOCATION,
            ConfigManager.FIELD_WIFI,
        )
        val allFields = profileFields + advancedFields + toggleFields
        val advancedPropertyKeys = mapOf(
            ConfigManager.FIELD_IMEI to ConfigManager.KEY_SPOOF_IMEI,
            ConfigManager.FIELD_IMEI_2 to ConfigManager.KEY_SPOOF_IMEI_2,
            ConfigManager.FIELD_MEID to ConfigManager.KEY_SPOOF_MEID,
            ConfigManager.FIELD_IMSI to ConfigManager.KEY_SPOOF_IMSI,
            ConfigManager.FIELD_ICCID to ConfigManager.KEY_SPOOF_ICCID,
            ConfigManager.FIELD_PHONE_NUMBER to ConfigManager.KEY_SPOOF_PHONE_NUMBER,
            ConfigManager.FIELD_PHONE_NUMBER_2 to ConfigManager.KEY_SPOOF_PHONE_NUMBER_2,
            ConfigManager.FIELD_GAID to ConfigManager.KEY_SPOOF_GAID,
            ConfigManager.FIELD_GSF_ID to ConfigManager.KEY_SPOOF_GSF_ID,
            ConfigManager.FIELD_MEDIA_DRM_ID to ConfigManager.KEY_SPOOF_MEDIA_DRM_ID,
            ConfigManager.FIELD_APP_SET_ID to ConfigManager.KEY_SPOOF_APP_SET_ID,
        )
    }
}

data class SafeAppEntry(
    val label: String,
    val packageName: String,
    val versionName: String,
    val systemApp: Boolean,
    val icon: Bitmap,
)

private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte) }
