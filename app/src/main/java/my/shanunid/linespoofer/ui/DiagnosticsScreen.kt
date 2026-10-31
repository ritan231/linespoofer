package my.shanunid.linespoofer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.shanunid.linespoofer.R
import my.shanunid.linespoofer.utils.AppLogger
import my.shanunid.linespoofer.utils.ConfigManager
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.TimeZone

@SuppressLint("MissingPermission", "HardwareIds")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiagnosticsScreen(appState: SpoofAppState, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val configuration = LocalConfiguration.current
    var logText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val content = AppLogger.getLogContent(context)
            withContext(Dispatchers.Main) {
                logText = content
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings_diagnostics_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.safe_mode_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        val config = appState.loadedConfig
        if (!appState.initialized || config == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val profile = config.profile
        val metrics = appState.activity.resources.displayMetrics

        // 1. Telephony & SIM Verifier
        val telephonyManager = remember { context.getSystemService(TelephonyManager::class.java) }
        val subManager = remember { context.getSystemService(SubscriptionManager::class.java) }
        val sim1Number = remember {
            try {
                telephonyManager?.line1Number.orEmpty().ifBlank { profile.phoneNumberSim1 ?: "-" }
            } catch (_: Throwable) {
                profile.phoneNumberSim1 ?: "-"
            }
        }
        val sim2Number = remember {
            try {
                val subs = subManager?.activeSubscriptionInfoList
                if (subs != null && subs.size > 1) {
                    val sub2 = subs[1]
                    val num = sub2.number
                    if (!num.isNullOrBlank()) {
                        num
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        telephonyManager?.createForSubscriptionId(sub2.subscriptionId)?.line1Number ?: (profile.phoneNumberSim2 ?: "-")
                    } else {
                        profile.phoneNumberSim2 ?: "-"
                    }
                } else {
                    profile.phoneNumberSim2 ?: "-"
                }
            } catch (_: Throwable) {
                profile.phoneNumberSim2 ?: "-"
            }
        }
        val simRows: List<Pair<String, String>> = listOf(
            stringResource(R.string.settings_field_operator_name) to telephonyManager?.networkOperatorName.orEmpty().ifBlank { profile.operatorAlpha ?: "-" },
            stringResource(R.string.settings_field_operator_numeric) to telephonyManager?.networkOperator.orEmpty().ifBlank { profile.operatorNumeric ?: "-" },
            stringResource(R.string.settings_field_sim_country_iso) to telephonyManager?.simCountryIso.orEmpty().ifBlank { profile.simCountryIso ?: "-" },
            stringResource(R.string.settings_field_phone_number) to sim1Number,
            stringResource(R.string.settings_field_phone_number_2) to sim2Number,
        )

        // 2. Locale & Timezone Verifier
        val icuTz = remember {
            try {
                val icuClass = Class.forName("android.icu.util.TimeZone")
                val def = icuClass.getMethod("getDefault").invoke(null)
                icuClass.getMethod("getID").invoke(def)?.toString() ?: "-"
            } catch (_: Throwable) {
                "-"
            }
        }
        val localeRows: List<Pair<String, String>> = listOf(
            "Java TimeZone" to "${TimeZone.getDefault().id} (${TimeZone.getDefault().displayName})",
            "ICU TimeZone" to icuTz,
            "Java Default Locale" to Locale.getDefault().toLanguageTag(),
            "Configuration Locale" to configuration.locales[0].toLanguageTag(),
            "Spoofed TimeZone Target" to (profile.timezone ?: "-"),
            "Spoofed Locale Target" to (profile.locale ?: "-"),
        )

        // 3. Display & FPS Verifier
        val displayRefreshRate = remember {
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                @Suppress("DEPRECATION")
                wm?.defaultDisplay?.refreshRate?.let { "%.1f Hz".format(it) } ?: "-"
            } catch (_: Throwable) {
                "-"
            }
        }
        val displayRows: List<Pair<String, String>> = listOf(
            "Current Refresh Rate" to displayRefreshRate,
            "Target FPS Mode" to "${appState.editor.targetFps.toInt()} FPS (${appState.editor.fpsMode})",
            "Screen Resolution" to "${metrics.widthPixels} × ${metrics.heightPixels}",
            "Screen Density" to "${metrics.densityDpi} dpi",
        )

        // 4. Battery & Suhu Verifier
        val batteryIntent = remember {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }
        val batteryLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val batteryTempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val batteryTempCelsius = if (batteryTempRaw > 0) "%.1f °C".format(batteryTempRaw / 10.0f) else "-"
        val batteryStatusInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val batteryStatusStr = when (batteryStatusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }
        val batteryRows: List<Pair<String, String>> = listOf(
            "Battery Level" to if (batteryLevel >= 0) "$batteryLevel %" else "-",
            "Battery Temperature" to batteryTempCelsius,
            "Battery Status" to batteryStatusStr,
            "Spoofed Mode" to appState.editor.batteryMode,
        )

        // 5. Network & Wireless Verifier
        val networkRows: List<Pair<String, String>> = listOf(
            "Wi-Fi SSID" to (config.extraProperties["device.wifi.ssid"] ?: "\"Home-WiFi-5G\""),
            "Wi-Fi BSSID" to (config.extraProperties["device.wifi.bssid"] ?: "-"),
            "MAC Mode" to appState.editor.macMode.uppercase(),
            "Bluetooth Name" to (config.extraProperties["device.bluetooth.name"] ?: "-"),
            "Bluetooth MAC" to (config.extraProperties["device.bluetooth.mac"] ?: "-"),
        )

        // 6. Build & Device Identifiers
        val buildRows: List<Pair<String, String>> = listOf(
            stringResource(R.string.settings_field_brand) to Build.BRAND.orEmpty().ifBlank { "-" },
            stringResource(R.string.settings_field_manufacturer) to Build.MANUFACTURER.orEmpty().ifBlank { "-" },
            stringResource(R.string.settings_field_model) to Build.MODEL.orEmpty().ifBlank { "-" },
            stringResource(R.string.settings_field_device_codename) to Build.DEVICE.orEmpty().ifBlank { "-" },
            stringResource(R.string.settings_field_board) to Build.BOARD.orEmpty().ifBlank { "-" },
            stringResource(R.string.settings_field_hardware) to Build.HARDWARE.orEmpty().ifBlank { "-" },
            stringResource(R.string.settings_field_build_id) to Build.ID.orEmpty().ifBlank { "-" },
            stringResource(R.string.settings_field_build_display_id) to Build.DISPLAY.orEmpty().ifBlank { "-" },
            stringResource(R.string.settings_field_fingerprint) to Build.FINGERPRINT.orEmpty().ifBlank { "-" },
            stringResource(R.string.settings_field_android_release) to Build.VERSION.RELEASE.orEmpty().ifBlank { "-" },
            stringResource(R.string.settings_field_sdk_level) to Build.VERSION.SDK_INT.toString(),
            stringResource(R.string.settings_field_security_patch) to Build.VERSION.SECURITY_PATCH.orEmpty().ifBlank { "-" },
            stringResource(R.string.settings_field_device_id) to (Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "-"),
        )

        val onExportLog = {
            coroutineScope.launch(Dispatchers.IO) {
                val reportBuilder = StringBuilder()
                reportBuilder.append("=== LineSpoofer Comprehensive Diagnostics Report ===\n")
                reportBuilder.append("Generated At: ").append(java.util.Date()).append("\n")
                reportBuilder.append("Master Switch: ").append(if (appState.isMasterSpoofActive) "ACTIVE" else "DISABLED").append("\n")
                reportBuilder.append("Active Profile: ").append(profile.model).append(" (Preset: ").append(appState.presetLabel(config.selectedPresetId)).append(")\n\n")

                reportBuilder.append("--- [1. Telephony & SIM] ---\n")
                simRows.forEach { reportBuilder.append("  ").append(it.first).append(": ").append(it.second).append("\n") }

                reportBuilder.append("\n--- [2. Locale & TimeZone] ---\n")
                localeRows.forEach { reportBuilder.append("  ").append(it.first).append(": ").append(it.second).append("\n") }

                reportBuilder.append("\n--- [3. Display & FPS] ---\n")
                displayRows.forEach { reportBuilder.append("  ").append(it.first).append(": ").append(it.second).append("\n") }

                reportBuilder.append("\n--- [4. Battery & Environment] ---\n")
                batteryRows.forEach { reportBuilder.append("  ").append(it.first).append(": ").append(it.second).append("\n") }

                reportBuilder.append("\n--- [5. Network & Wireless] ---\n")
                networkRows.forEach { reportBuilder.append("  ").append(it.first).append(": ").append(it.second).append("\n") }

                reportBuilder.append("\n--- [6. Build & Device Identifiers] ---\n")
                buildRows.forEach { reportBuilder.append("  ").append(it.first).append(": ").append(it.second).append("\n") }

                reportBuilder.append("\n--- [7. App & Vector Logs] ---\n")
                val currentLog = AppLogger.getLogContent(context)
                reportBuilder.append(currentLog.ifBlank { "(No internal logs recorded)" }).append("\n")

                val reportContent = reportBuilder.toString()

                // Save report to file for attachment
                val reportFile = File(context.cacheDir, "LineSpoofer_Diagnostics_Report.txt")
                try {
                    FileOutputStream(reportFile).use { it.write(reportContent.toByteArray(StandardCharsets.UTF_8)) }
                } catch (_: Throwable) {
                }

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "LineSpoofer Diagnostics Report")
                    putExtra(Intent.EXTRA_TEXT, reportContent)
                    try {
                        if (reportFile.exists() && reportFile.length() > 0) {
                            val fileUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                reportFile
                            )
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    } catch (_: Throwable) {
                    }
                }
                withContext(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.log_export_title)))
                }
            }
        }

        val onClearLog = {
            coroutineScope.launch(Dispatchers.IO) {
                AppLogger.clearLog(context)
                withContext(Dispatchers.Main) {
                    logText = ""
                    Toast.makeText(context, context.getString(R.string.log_cleared_toast), Toast.LENGTH_SHORT).show()
                }
            }
        }

        val onRefreshLog = {
            coroutineScope.launch(Dispatchers.IO) {
                val content = AppLogger.getLogContent(context)
                withContext(Dispatchers.Main) {
                    logText = content
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
        ) {
            // Section 1: SIM & Telephony Verification
            item(key = "hdr_sim", contentType = "header") {
                SectionTitle(stringResource(R.string.diagnostics_section_telephony))
            }
            items(
                count = simRows.size,
                key = { "sim_$it" },
                contentType = { "info_row" },
            ) { index ->
                ConnectedInfoRow(index, simRows.size, simRows[index].first, simRows[index].second)
            }

            // Section 2: Locale & Timezone Verification
            item(key = "hdr_locale", contentType = "header") {
                SectionTitle(stringResource(R.string.diagnostics_section_locale), Modifier.padding(top = 22.dp))
            }
            items(
                count = localeRows.size,
                key = { "locale_$it" },
                contentType = { "info_row" },
            ) { index ->
                ConnectedInfoRow(index, localeRows.size, localeRows[index].first, localeRows[index].second)
            }

            // Section 3: Display & Refresh Rate
            item(key = "hdr_display", contentType = "header") {
                SectionTitle(stringResource(R.string.diagnostics_section_display), Modifier.padding(top = 22.dp))
            }
            items(
                count = displayRows.size,
                key = { "display_$it" },
                contentType = { "info_row" },
            ) { index ->
                ConnectedInfoRow(index, displayRows.size, displayRows[index].first, displayRows[index].second)
            }

            // Section 4: Battery & Temperature
            item(key = "hdr_battery", contentType = "header") {
                SectionTitle(stringResource(R.string.diagnostics_section_battery), Modifier.padding(top = 22.dp))
            }
            items(
                count = batteryRows.size,
                key = { "battery_$it" },
                contentType = { "info_row" },
            ) { index ->
                ConnectedInfoRow(index, batteryRows.size, batteryRows[index].first, batteryRows[index].second)
            }

            // Section 5: Network & Wireless
            item(key = "hdr_network", contentType = "header") {
                SectionTitle(stringResource(R.string.diagnostics_section_network), Modifier.padding(top = 22.dp))
            }
            items(
                count = networkRows.size,
                key = { "network_$it" },
                contentType = { "info_row" },
            ) { index ->
                ConnectedInfoRow(index, networkRows.size, networkRows[index].first, networkRows[index].second)
            }

            // Section 6: Build & Identifiers
            item(key = "hdr_build", contentType = "header") {
                SectionTitle(stringResource(R.string.diagnostics_section_build), Modifier.padding(top = 22.dp))
            }
            items(
                count = buildRows.size,
                key = { "build_$it" },
                contentType = { "info_row" },
            ) { index ->
                ConnectedInfoRow(index, buildRows.size, buildRows[index].first, buildRows[index].second)
            }

            // Section 7: Diagnostics Log Viewer & Export Full Report
            item(key = "hdr_logs", contentType = "header") {
                SectionTitle(stringResource(R.string.diagnostics_logs_title), Modifier.padding(top = 22.dp))
            }
            item(key = "export_actions", contentType = "actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { onExportLog() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.diagnostics_export_full_report))
                    }
                    OutlinedButton(
                        onClick = { onClearLog() },
                        modifier = Modifier.weight(0.6f)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.log_clear_button))
                    }
                    IconButton(onClick = { onRefreshLog() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.log_refresh_button))
                    }
                }
            }
            item(key = "log_viewer_surface", contentType = "log_viewer") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 280.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(scrollState)
                    ) {
                        if (logText.isBlank()) {
                            Text(
                                text = stringResource(R.string.log_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = logText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item(key = "bottom_spacer", contentType = "spacer") { Spacer(Modifier.height(36.dp)) }
        }
    }
}
