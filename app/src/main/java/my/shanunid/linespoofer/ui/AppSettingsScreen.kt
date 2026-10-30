package my.shanunid.linespoofer.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.shanunid.linespoofer.DiagnosticsActivity
import my.shanunid.linespoofer.R
import my.shanunid.linespoofer.RealInfoActivity
import my.shanunid.linespoofer.SafeModeAppsActivity
import my.shanunid.linespoofer.data.AppSettingsStore
import kotlinx.coroutines.launch

private data class SettingOption(val id: String, val labelRes: Int)

@Composable
fun AppSettingsScreen(appState: SpoofAppState, scaffoldPadding: PaddingValues) {
    val context = appState.activity
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val colors = MiuiTheme.colors
    var optionDialog = rememberMutableString()
    var presetSourceDialog = rememberMutableBoolean()

    val safeModeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        val packages = result.data
            ?.getStringArrayListExtra(SafeModeAppsActivity.EXTRA_RESULT_SELECTED_PACKAGES)
            ?.toSet()
            ?: emptySet()
        scope.launch { appState.updateSafeModePackages(packages) }
    }

    val themeOptions = listOf(
        stringResource(R.string.theme_system),
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark),
    )
    val selectedThemeIndex = when (appState.themeMode) {
        AppSettingsStore.THEME_LIGHT -> 1
        AppSettingsStore.THEME_DARK -> 2
        else -> 0
    }

    val languageOptions = listOf(
        SettingOption(AppSettingsStore.LANGUAGE_DEFAULT, R.string.language_default),
        SettingOption(AppSettingsStore.LANGUAGE_INDONESIAN, R.string.language_indonesian),
        SettingOption(AppSettingsStore.LANGUAGE_ENGLISH, R.string.language_english),
        SettingOption(AppSettingsStore.LANGUAGE_KOREAN, R.string.language_korean),
        SettingOption(AppSettingsStore.LANGUAGE_JAPANESE, R.string.language_japanese),
        SettingOption(AppSettingsStore.LANGUAGE_CHINESE_SIMPLIFIED, R.string.language_chinese),
    )

    LaunchedEffect(Unit) { listState.scrollToItem(0) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(scaffoldPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 1. Appearance Section
        item {
            MiuiSectionHeader(title = stringResource(R.string.settings_appearance_title))
            MiuiCard {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = stringResource(R.string.settings_theme_title),
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(8.dp))
                    MiuiSegmentedPill(
                        options = themeOptions,
                        selectedIndex = selectedThemeIndex,
                        onSelect = { idx ->
                            val mode = when (idx) {
                                1 -> AppSettingsStore.THEME_LIGHT
                                2 -> AppSettingsStore.THEME_DARK
                                else -> AppSettingsStore.THEME_SYSTEM
                            }
                            appState.updateThemeMode(mode)
                        },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(colors.divider)
                )

                MiuiNavigationRow(
                    title = stringResource(R.string.settings_language_title),
                    subtitle = stringResource(R.string.settings_language_helper),
                    valueText = optionLabel(languageOptions, appState.languageMode),
                    icon = Icons.Outlined.Translate,
                    onClick = { optionDialog.value = "language" },
                )
            }
        }

        // 2. Runtime Options Section
        item {
            MiuiSectionHeader(title = stringResource(R.string.settings_runtime_title))
            MiuiCard {
                MiuiSwitchRow(
                    title = stringResource(R.string.settings_resolution_title),
                    subtitle = stringResource(R.string.settings_resolution_summary),
                    icon = Icons.Outlined.AspectRatio,
                    checked = appState.isScreenMetricsEnabled(),
                    onCheckedChange = { enabled -> scope.launch { appState.updateScreenMetrics(enabled) } },
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(colors.divider)
                )

                MiuiNavigationRow(
                    title = stringResource(R.string.settings_safe_mode_title),
                    subtitle = if (appState.safeModePackages().isEmpty()) {
                        stringResource(R.string.settings_safe_mode_summary_disabled)
                    } else {
                        stringResource(R.string.settings_safe_mode_summary_count, appState.safeModePackages().size)
                    },
                    icon = Icons.Outlined.Security,
                    onClick = {
                        safeModeLauncher.launch(
                            SafeModeAppsActivity.createIntent(context, appState.safeModePackages())
                        )
                    },
                )
            }
        }

        // 3. Presets & Data Section
        item {
            MiuiSectionHeader(title = stringResource(R.string.settings_presets_title))
            MiuiCard {
                MiuiNavigationRow(
                    title = stringResource(R.string.settings_preset_source_title),
                    subtitle = AppSettingsStore.getPresetSourceUrl(context),
                    icon = Icons.Outlined.Link,
                    onClick = { presetSourceDialog.value = true },
                )
            }
        }

        // 4. Tools & Diagnostics Section
        item {
            MiuiSectionHeader(title = stringResource(R.string.settings_tools_title))
            MiuiCard {
                MiuiNavigationRow(
                    title = stringResource(R.string.settings_diagnostics_title),
                    subtitle = stringResource(R.string.real_info_button_helper),
                    icon = Icons.Outlined.BugReport,
                    onClick = { context.startActivity(Intent(context, DiagnosticsActivity::class.java)) },
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(colors.divider)
                )

                MiuiNavigationRow(
                    title = stringResource(R.string.settings_real_info_title),
                    subtitle = stringResource(R.string.settings_real_info_summary),
                    icon = Icons.Outlined.Info,
                    onClick = { context.startActivity(Intent(context, RealInfoActivity::class.java)) },
                )
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }

    when (optionDialog.value) {
        "language" -> OptionDialog(
            title = stringResource(R.string.settings_choose_language),
            options = languageOptions,
            selectedId = appState.languageMode,
            onDismiss = { optionDialog.value = null },
            onSelect = {
                appState.updateLanguageMode(it)
                optionDialog.value = null
            },
        )
    }

    if (presetSourceDialog.value) {
        PresetSourceDialog(
            currentUrl = AppSettingsStore.getPresetSourceUrl(context),
            onDismiss = { presetSourceDialog.value = false },
            onSave = {
                presetSourceDialog.value = false
                scope.launch { appState.updatePresetSource(it) }
            },
        )
    }
}

@Composable
private fun rememberMutableString() = androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf<String?>(null)
}

@Composable
private fun rememberMutableBoolean() = androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(false)
}

@Composable
private fun optionLabel(options: List<SettingOption>, selectedId: String): String {
    return options.firstOrNull { it.id == selectedId }?.let { stringResource(it.labelRes) }.orEmpty()
}

@Composable
private fun OptionDialog(
    title: String,
    options: List<SettingOption>,
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val colors = MiuiTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option.id) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option.id == selectedId,
                            onClick = { onSelect(option.id) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MiuiXColors.Blue,
                                unselectedColor = colors.textSecondary,
                            ),
                        )
                        Text(
                            text = stringResource(option.labelRes),
                            color = colors.textPrimary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel), color = MiuiXColors.Blue)
            }
        },
    )
}

@Composable
private fun PresetSourceDialog(currentUrl: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val defaultUrl = AppSettingsStore.DEFAULT_PRESET_SOURCE_URL
    val custom = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentUrl != defaultUrl) }
    val url = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(if (custom.value) currentUrl else "") }
    val colors = MiuiTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.settings_preset_source_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { custom.value = false }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = !custom.value,
                        onClick = { custom.value = false },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MiuiXColors.Blue,
                            unselectedColor = colors.textSecondary,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.settings_preset_source_option_default),
                        color = colors.textPrimary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { custom.value = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = custom.value,
                        onClick = { custom.value = true },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MiuiXColors.Blue,
                            unselectedColor = colors.textSecondary,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.settings_preset_source_option_custom),
                        color = colors.textPrimary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                if (custom.value) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = url.value,
                        onValueChange = { url.value = it },
                        label = { Text(stringResource(R.string.settings_preset_source_input_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(if (custom.value) url.value.trim() else defaultUrl) },
                enabled = !custom.value || url.value.isNotBlank(),
            ) {
                Text(stringResource(android.R.string.ok), color = MiuiXColors.Blue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel), color = colors.textSecondary)
            }
        },
    )
}
