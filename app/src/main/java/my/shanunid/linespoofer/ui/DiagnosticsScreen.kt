package my.shanunid.linespoofer.ui

import android.content.Intent
import android.os.Build
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
import java.util.TimeZone

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
        val buildRows = listOf(
            stringResource(R.string.settings_field_brand) to Build.BRAND,
            stringResource(R.string.settings_field_manufacturer) to Build.MANUFACTURER,
            stringResource(R.string.settings_field_model) to Build.MODEL,
            stringResource(R.string.settings_field_device_codename) to Build.DEVICE,
            stringResource(R.string.settings_field_board) to Build.BOARD,
            stringResource(R.string.settings_field_hardware) to Build.HARDWARE,
            stringResource(R.string.settings_field_build_id) to Build.ID,
            stringResource(R.string.settings_field_build_display_id) to Build.DISPLAY,
            stringResource(R.string.settings_field_fingerprint) to Build.FINGERPRINT,
            stringResource(R.string.settings_field_android_release) to Build.VERSION.RELEASE,
            stringResource(R.string.settings_field_sdk_level) to Build.VERSION.SDK_INT.toString(),
            stringResource(R.string.settings_field_security_patch) to Build.VERSION.SECURITY_PATCH,
        )
        val runtimeRows = listOf(
            stringResource(R.string.home_info_profile_display_title) to stringResource(
                R.string.home_info_display_value_format,
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
            ),
            "ABI" to Build.SUPPORTED_ABIS.joinToString(", "),
            stringResource(R.string.settings_field_timezone) to TimeZone.getDefault().id,
            stringResource(R.string.settings_field_locale) to configuration.locales[0].toLanguageTag(),
        )
        val savedRows = listOf(
            stringResource(R.string.settings_hint_preset) to appState.presetLabel(config.selectedPresetId),
            stringResource(R.string.home_info_profile_mode_title) to stringResource(if (config.isCustomMode) R.string.mode_custom else R.string.mode_preset),
            stringResource(R.string.settings_field_model) to profile.model,
            stringResource(R.string.settings_field_device_codename) to profile.deviceCode,
            stringResource(R.string.settings_field_build_id) to profile.buildId,
            stringResource(R.string.settings_field_build_display_id) to profile.buildDisplayId,
            stringResource(R.string.settings_field_fingerprint) to profile.buildFingerprint,
            stringResource(R.string.settings_field_timezone) to profile.timezone,
            stringResource(R.string.settings_field_locale) to profile.locale,
        )

        val onExportLog = {
            coroutineScope.launch(Dispatchers.IO) {
                val currentLog = AppLogger.getLogContent(context)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.log_export_title))
                    putExtra(Intent.EXTRA_TEXT, currentLog.ifBlank { "LineSpoofer Diagnostics Log: (empty)" })
                    try {
                        val logFile = AppLogger.getLogFile(context)
                        if (logFile != null && logFile.exists() && logFile.length() > 0) {
                            val fileUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                logFile
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
            item { SectionTitle(stringResource(R.string.real_info_build_title)) }
            buildRows.forEachIndexed { index, row ->
                item { ConnectedInfoRow(index, buildRows.size, row.first, row.second) }
            }
            item { SectionTitle(stringResource(R.string.real_info_runtime_title), Modifier.padding(top = 22.dp)) }
            runtimeRows.forEachIndexed { index, row ->
                item { ConnectedInfoRow(index, runtimeRows.size, row.first, row.second) }
            }
            item { SectionTitle(stringResource(R.string.real_info_saved_title), Modifier.padding(top = 22.dp)) }
            savedRows.forEachIndexed { index, row ->
                item { ConnectedInfoRow(index, savedRows.size, row.first, row.second) }
            }

            // Diagnostics Log Viewer & Export Section
            item { SectionTitle(stringResource(R.string.diagnostics_logs_title), Modifier.padding(top = 22.dp)) }
            item {
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
                        Text(stringResource(R.string.log_export_button))
                    }
                    OutlinedButton(
                        onClick = { onClearLog() },
                        modifier = Modifier.weight(1f)
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
            item {
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

            item { Spacer(Modifier.height(36.dp)) }
        }
    }
}
