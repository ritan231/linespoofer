package my.shanunid.linespoofer.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.TextButton
import java.util.TimeZone
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import my.shanunid.linespoofer.AppProfileAssignmentActivity
import my.shanunid.linespoofer.ProfileEditorActivity
import my.shanunid.linespoofer.R
import my.shanunid.linespoofer.utils.ConfigManager
import kotlinx.coroutines.launch

private data class EditorField(
    val fieldId: String,
    val labelRes: Int,
    val numeric: Boolean = false,
    val multiline: Boolean = false,
    val isToggle: Boolean = false,
)

private val identityFields = listOf(
    EditorField(ConfigManager.FIELD_BRAND, R.string.settings_field_brand),
    EditorField(ConfigManager.FIELD_MANUFACTURER, R.string.settings_field_manufacturer),
    EditorField(ConfigManager.FIELD_MODEL, R.string.settings_field_model),
    EditorField(ConfigManager.FIELD_DEVICE, R.string.settings_field_device_codename),
    EditorField(ConfigManager.FIELD_PRODUCT, R.string.settings_field_product_name),
    EditorField(ConfigManager.FIELD_BOARD, R.string.settings_field_board),
    EditorField(ConfigManager.FIELD_HARDWARE, R.string.settings_field_hardware),
    EditorField(ConfigManager.FIELD_BOARD_PLATFORM, R.string.settings_field_board_platform),
)

private val buildFields = listOf(
    EditorField(ConfigManager.FIELD_ANDROID_RELEASE, R.string.settings_field_android_release),
    EditorField(ConfigManager.FIELD_SDK, R.string.settings_field_sdk_level, numeric = true),
    EditorField(ConfigManager.FIELD_SECURITY_PATCH, R.string.settings_field_security_patch),
    EditorField(ConfigManager.FIELD_BUILD_ID, R.string.settings_field_build_id),
    EditorField(ConfigManager.FIELD_BUILD_DISPLAY_ID, R.string.settings_field_build_display_id),
    EditorField(ConfigManager.FIELD_BUILD_INCREMENTAL, R.string.settings_field_build_incremental),
    EditorField(ConfigManager.FIELD_FINGERPRINT, R.string.settings_field_fingerprint, multiline = true),
    EditorField(ConfigManager.FIELD_SCREEN_WIDTH, R.string.settings_field_screen_width, numeric = true),
    EditorField(ConfigManager.FIELD_SCREEN_HEIGHT, R.string.settings_field_screen_height, numeric = true),
    EditorField(ConfigManager.FIELD_SCREEN_DENSITY, R.string.settings_field_screen_density, numeric = true),
)

private val networkFields = listOf(
    EditorField(ConfigManager.FIELD_TELEPHONY, R.string.settings_field_telephony, isToggle = true),
    EditorField(ConfigManager.FIELD_OPERATOR_ALPHA, R.string.settings_field_operator_name),
    EditorField(ConfigManager.FIELD_OPERATOR_NUMERIC, R.string.settings_field_operator_numeric, numeric = true),
    EditorField(ConfigManager.FIELD_SIM_COUNTRY, R.string.settings_field_sim_country_iso),
    EditorField(ConfigManager.FIELD_TIMEZONE, R.string.settings_field_timezone),
    EditorField(ConfigManager.FIELD_LOCALE, R.string.settings_field_locale),
    EditorField(ConfigManager.FIELD_BLOCK_LOCAL_SOCKET, R.string.settings_field_block_local_socket, isToggle = true),
    EditorField(ConfigManager.FIELD_SENSORS, R.string.settings_field_sensors, isToggle = true),
    EditorField(ConfigManager.FIELD_WEBGL, R.string.settings_field_webgl, isToggle = true),
)

private val systemFields = listOf(
    EditorField(ConfigManager.FIELD_STORAGE, R.string.settings_field_storage, isToggle = true),
    EditorField(ConfigManager.FIELD_AUDIO, R.string.settings_field_audio, isToggle = true),
    EditorField(ConfigManager.FIELD_GPU, R.string.settings_field_gpu, isToggle = true),
    EditorField(ConfigManager.FIELD_LOCATION, R.string.settings_field_location, isToggle = true),
)

private val advancedFields = listOf(
    EditorField(ConfigManager.FIELD_ANDROID_ID, R.string.settings_field_device_id),
    EditorField(ConfigManager.FIELD_IMEI, R.string.settings_field_imei, numeric = true),
    EditorField(ConfigManager.FIELD_IMEI_2, R.string.settings_field_imei_2, numeric = true),
    EditorField(ConfigManager.FIELD_MEID, R.string.settings_field_meid),
    EditorField(ConfigManager.FIELD_IMSI, R.string.settings_field_imsi, numeric = true),
    EditorField(ConfigManager.FIELD_ICCID, R.string.settings_field_iccid, numeric = true),
    EditorField(ConfigManager.FIELD_PHONE_NUMBER, R.string.settings_field_phone_number),
    EditorField(ConfigManager.FIELD_GAID, R.string.settings_field_gaid),
    EditorField(ConfigManager.FIELD_GSF_ID, R.string.settings_field_gsf_id),
    EditorField(ConfigManager.FIELD_MEDIA_DRM_ID, R.string.settings_field_media_drm_id),
    EditorField(ConfigManager.FIELD_APP_SET_ID, R.string.settings_field_app_set_id),
)

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun AppProfilesScreen(appState: SpoofAppState, scaffoldPadding: PaddingValues) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val assignmentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        scope.launch { appState.reloadProfileState() }
    }

    LaunchedEffect(Unit) {
        appState.ensureInstalledAppsLoaded()
    }

    val visibleApps = remember(appState.installedApps, appState.hideSystemAppsInAppSettings) {
        if (appState.hideSystemAppsInAppSettings) {
            appState.installedApps.filterNot(SafeAppEntry::systemApp)
        } else {
            appState.installedApps
        }
    }
    AppSelectionList(
        apps = visibleApps,
        loading = !appState.installedAppsLoaded,
        scaffoldPadding = scaffoldPadding,
        profileName = appState::assignedProfileName,
        onSelect = { app ->
            assignmentLauncher.launch(
                AppProfileAssignmentActivity.createIntent(context, app.packageName),
            )
        },
    )
}

@Composable
private fun AppSelectionList(
    apps: List<SafeAppEntry>,
    loading: Boolean,
    scaffoldPadding: PaddingValues,
    profileName: (String) -> String?,
    onSelect: (SafeAppEntry) -> Unit,
) {
    if (loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LoadingIndicator(modifier = Modifier.progressSemantics())
                Spacer(Modifier.height(MdSpacing.sm))
                Text(
                    text = stringResource(R.string.app_profile_loading_apps),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MiuiXColors.TextSecondary,
                )
            }
        }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(apps, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            apps
        } else {
            apps.filter { app ->
                app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding),
    ) {
        // Sticky/Fixed HyperOS Search Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MiuiXColors.DarkCard,
                    unfocusedContainerColor = MiuiXColors.DarkCard,
                    focusedBorderColor = MiuiXColors.Blue,
                    unfocusedBorderColor = MiuiXColors.DarkBorder,
                    focusedTextColor = MiuiXColors.TextPrimary,
                    unfocusedTextColor = MiuiXColors.TextPrimary,
                ),
                placeholder = {
                    Text(stringResource(R.string.app_profile_search), color = MiuiXColors.TextSecondary)
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = MiuiXColors.Blue)
                },
                singleLine = true,
            )
        }

        // High Performance 120Hz Fluid LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = filteredApps,
                key = { it.packageName },
                contentType = { "app_row" },
            ) { app ->
                val assignedProfile = profileName(app.packageName)
                MiuiAppRow(
                    app = app,
                    assignedProfile = assignedProfile,
                    onClick = { onSelect(app) },
                )
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun MiuiAppRow(
    app: SafeAppEntry,
    assignedProfile: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MiuiXColors.DarkCard)
            .border(0.8.dp, MiuiXColors.DarkBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(
                packageName = app.packageName,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    color = MiuiXColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    color = MiuiXColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (assignedProfile != null) MiuiXColors.BlueContainer else androidx.compose.ui.graphics.Color(0xFF222630)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = assignedProfile ?: stringResource(R.string.app_profile_unassigned),
                    color = if (assignedProfile != null) MiuiXColors.Blue else MiuiXColors.TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = if (assignedProfile != null) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ProfileManagementScreen(
    appState: SpoofAppState,
    scaffoldPadding: PaddingValues,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profiles = appState.profileState?.profiles.orEmpty()
    var profileIdPendingDeletion by remember { mutableStateOf<String?>(null) }
    val editorLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        val profileId = result.data?.getStringExtra(ProfileEditorActivity.EXTRA_RESULT_PROFILE_ID)
        scope.launch { appState.reloadProfileState(profileId) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MiuiSectionHeader(title = stringResource(R.string.profile_management_list_title))
        }
        items(
            items = profiles,
            key = { it.id },
            contentType = { "profile_card" },
        ) { profile ->
            val isActive = profile.id == appState.activeProfileId
            val assignmentCount = appState.profileAssignmentCount(profile.id)

            MiuiCard(
                onClick = {
                    editorLauncher.launch(ProfileEditorActivity.createEditIntent(context, profile.id))
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isActive) MiuiXColors.BlueContainer else androidx.compose.ui.graphics.Color(0xFF222631)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBox,
                                contentDescription = null,
                                tint = if (isActive) MiuiXColors.Blue else MiuiXColors.TextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.name,
                                    color = MiuiXColors.TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (isActive) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MiuiXColors.BlueContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            color = MiuiXColors.Blue,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = stringResource(R.string.app_profile_assignment_count, assignmentCount),
                                color = MiuiXColors.TextSecondary,
                                fontSize = 12.5.sp,
                            )
                        }
                    }

                    if (profiles.size > 1) {
                        IconButton(
                            onClick = { profileIdPendingDeletion = profile.id },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.profile_delete),
                                tint = MiuiXColors.Red,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MiuiXColors.Blue)
                    .clickable {
                        editorLauncher.launch(ProfileEditorActivity.createCreateIntent(context))
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.app_profile_create),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(100.dp)) }
    }

    val profilePendingDeletion = profiles.firstOrNull { it.id == profileIdPendingDeletion }
    if (profilePendingDeletion != null) {
        val assignmentCount = appState.profileAssignmentCount(profilePendingDeletion.id)
        AlertDialog(
            onDismissRequest = { profileIdPendingDeletion = null },
            title = { Text(stringResource(R.string.profile_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.profile_delete_message,
                        profilePendingDeletion.name,
                        assignmentCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileIdPendingDeletion = null
                        scope.launch { appState.deleteProfile(profilePendingDeletion.id) }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MiuiXColors.Red,
                    ),
                ) {
                    Text(stringResource(R.string.profile_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { profileIdPendingDeletion = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun ProfileEditorForm(
    appState: SpoofAppState,
    scaffoldPadding: PaddingValues,
    profileName: String,
    onProfileNameChange: (String) -> Unit,
) {
    val editor = appState.editor
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var advancedExpanded by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        scope.launch { appState.populateAdvancedDefaults() }
    }

    fun loadAdvancedValues() {
        val permissions = listOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS)
        val missing = permissions.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            scope.launch { appState.populateAdvancedDefaults() }
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(Unit) { listState.scrollToItem(0) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            DeviceSettingsSection(stringResource(R.string.profile_details_title)) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = onProfileNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.app_profile_name)) },
                    singleLine = true,
                )
            }
        }
        item {
            DeviceSettingsSection(stringResource(R.string.settings_section_preset_mode)) {
                PresetSelector(appState)
                Spacer(Modifier.height(14.dp))
                val modeOptions = listOf(stringResource(R.string.mode_preset), stringResource(R.string.mode_custom))
                MiuiSegmentedPill(
                    options = modeOptions,
                    selectedIndex = if (editor.customMode) 1 else 0,
                    onSelect = { idx ->
                        editor.setCustomMode(idx == 1, appState.presets)
                    }
                )
            }
        }
        item {
            EditorSection(
                title = stringResource(R.string.settings_section_identity),
                fields = identityFields,
                editor = editor,
            )
        }
        item {
            EditorSection(
                title = stringResource(R.string.settings_section_build_display),
                fields = buildFields,
                editor = editor,
            )
        }
        item {
            EditorSection(
                title = stringResource(R.string.settings_section_network_region),
                fields = networkFields,
                editor = editor,
            )
        }
        item {
            EditorSection(
                title = stringResource(R.string.settings_section_system_hardware),
                fields = systemFields,
                editor = editor,
            )
        }
        item {
            DynamicEnvironmentSpoofSection(editor)
        }
        item {
            DeviceSettingsSection(stringResource(R.string.settings_section_advanced)) {
                Text(
                    text = stringResource(R.string.settings_advanced_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = {
                        advancedExpanded = !advancedExpanded
                        if (advancedExpanded) loadAdvancedValues()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (advancedExpanded) R.string.advanced_hide else R.string.advanced_show))
                }
                AnimatedVisibility(advancedExpanded) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(
                                onClick = editor::randomizeAll,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Casino,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.settings_generate_all))
                            }
                            FilledTonalButton(
                                onClick = editor::clearAdvanced,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.settings_clear_all))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        advancedFields.forEach { field ->
                            AdvancedEditorField(field, editor)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(MdSpacing.xl)) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PresetSelector(appState: SpoofAppState) {
    var expanded by remember { mutableStateOf(false) }
    val editor = appState.editor
    val selectedLabel = appState.presets.firstOrNull { it.id == editor.selectedPresetId }?.displayName.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_hint_preset)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            appState.presets.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(preset.displayName)
                            if (preset.summary.isNotBlank()) {
                                Text(
                                    preset.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        editor.selectPreset(preset)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EditorSection(title: String, fields: List<EditorField>, editor: ProfileEditorState) {
    DeviceSettingsSection(title) {
        fields.forEach { field ->
            ProfileEditorField(field, editor)
        }
    }
}

@Composable
private fun DeviceSettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MiuiSectionHeader(title)
        MiuiCard(content = content)
    }
}

@Composable
private fun ProfileEditorField(field: EditorField, editor: ProfileEditorState) {
    val spoofEnabled = editor.enabledFields[field.fieldId] != false
    if (field.isToggle) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(field.labelRes),
                    color = MiuiXColors.TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                MiuiSwitch(
                    checked = spoofEnabled,
                    onCheckedChange = { editor.enabledFields[field.fieldId] = it },
                )
            }
            if (field.fieldId == ConfigManager.FIELD_LOCATION && spoofEnabled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 8.dp)
                ) {
                    var rawInput by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = rawInput,
                        onValueChange = { input ->
                            rawInput = input
                            val parsed = my.shanunid.linespoofer.utils.LocationCoordinatesParser.parse(input)
                            if (parsed != null) {
                                editor.locationLatitude = parsed.latitude.toString()
                                editor.locationLongitude = parsed.longitude.toString()
                            }
                        },
                        label = { Text("Paste Link / Koordinat Google Maps") },
                        placeholder = { Text("-6.208763, 106.845599") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editor.locationLatitude,
                            onValueChange = { editor.locationLatitude = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = editor.locationLongitude,
                            onValueChange = { editor.locationLongitude = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                }
            }
        }
    } else {
        var showPicker by remember { mutableStateOf(false) }
        val isTimezone = field.fieldId == ConfigManager.FIELD_TIMEZONE
        val isLocale = field.fieldId == ConfigManager.FIELD_LOCALE

        if (showPicker && isTimezone) {
            val tzList = remember {
                TimeZone.getAvailableIDs().sorted().map { it to it }
            }
            SearchableListDialog(
                title = stringResource(R.string.dialog_select_timezone),
                searchHint = stringResource(R.string.search_timezone),
                items = tzList,
                onSelect = { editor.values[field.fieldId] = it },
                onDismiss = { showPicker = false }
            )
        }

        if (showPicker && isLocale) {
            val localeList = remember {
                Locale.getAvailableLocales()
                    .filter { it.language.isNotEmpty() }
                    .distinctBy { it.toLanguageTag() }
                    .sortedBy { it.displayName }
                    .map { it.toLanguageTag() to "${it.displayName} (${it.toLanguageTag()})" }
            }
            SearchableListDialog(
                title = stringResource(R.string.dialog_select_locale),
                searchHint = stringResource(R.string.search_locale),
                items = localeList,
                onSelect = { editor.values[field.fieldId] = it },
                onDismiss = { showPicker = false }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = spoofEnabled,
                onCheckedChange = { editor.enabledFields[field.fieldId] = it },
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = editor.values[field.fieldId].orEmpty(),
                onValueChange = { editor.values[field.fieldId] = it },
                label = { Text(stringResource(field.labelRes)) },
                enabled = spoofEnabled && editor.customMode,
                minLines = if (field.multiline) 2 else 1,
                trailingIcon = if ((isTimezone || isLocale) && editor.customMode) {
                    {
                        IconButton(onClick = { showPicker = true }) {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        }
                    }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = if (field.numeric) KeyboardType.Number else KeyboardType.Text),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun SearchableListDialog(
    title: String,
    searchHint: String,
    items: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, items) {
        val q = query.trim()
        if (q.isEmpty()) items else items.filter {
            it.first.contains(q, ignoreCase = true) || it.second.contains(q, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(searchHint) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(filtered) { _, item ->
                        androidx.compose.material3.ListItem(
                            headlineContent = {
                                Text(
                                    item.second,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = { Text(item.first, maxLines = 1) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(item.first)
                                    onDismiss()
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.back))
            }
        }
    )
}

@Composable
private fun AdvancedEditorField(field: EditorField, editor: ProfileEditorState) {
    val spoofEnabled = editor.enabledFields[field.fieldId] != false
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = spoofEnabled,
            onCheckedChange = { editor.enabledFields[field.fieldId] = it },
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = editor.advancedValues[field.fieldId].orEmpty(),
            onValueChange = { editor.advancedValues[field.fieldId] = it },
            label = { Text(stringResource(field.labelRes)) },
            enabled = spoofEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = if (field.numeric) KeyboardType.Number else KeyboardType.Text),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(
            onClick = { editor.randomize(field.fieldId) },
            enabled = spoofEnabled,
        ) {
            Icon(Icons.Filled.Casino, stringResource(R.string.settings_generate_random))
        }
    }
}

@Composable
private fun DynamicEnvironmentSpoofSection(editor: ProfileEditorState) {
    MiuiSectionHeader(title = "DYNAMIC HARDWARE & ENVIRONMENT")
    MiuiCard {
        // 1. Custom FPS
        MiuiSwitchRow(
            icon = Icons.Outlined.AspectRatio,
            title = "Custom Display FPS",
            subtitle = "Kustomisasi refresh rate render aplikasi (${editor.targetFps.toInt()} FPS)",
            checked = editor.fpsMode != "off",
            onCheckedChange = { editor.fpsMode = if (it) "custom" else "off" },
        )
        AnimatedVisibility(editor.fpsMode != "off") {
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)) {
                MiuiSliderRow(
                    title = "Target Refresh Rate",
                    value = editor.targetFps,
                    valueRange = 30f..144f,
                    steps = 3,
                    valueText = "${editor.targetFps.toInt()} FPS",
                    onValueChange = { editor.targetFps = it },
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(MiuiXColors.DarkDivider))

        // 2. Spoof MAC Address (Random atau Custom)
        val macModes = listOf("Off", "Random", "Custom")
        val currentMacIndex = when (editor.macMode.lowercase()) {
            "random" -> 1
            "custom" -> 2
            else -> 0
        }
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Spoof Hardware MAC Address",
                    color = MiuiXColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (editor.macMode == "custom") {
                    IconButton(
                        onClick = { editor.macAddress = my.shanunid.linespoofer.utils.ConfigManager.generateRandomMac() },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Casino,
                            contentDescription = "Acak",
                            tint = MiuiXColors.Blue,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Intersepsi NetworkInterface & WifiInfo untuk wlan0 / eth0",
                color = MiuiXColors.TextSecondary,
                fontSize = 12.5.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            MiuiSegmentedPill(
                options = macModes,
                selectedIndex = currentMacIndex,
                onSelect = { idx ->
                    editor.macMode = when (idx) {
                        1 -> "random"
                        2 -> "custom"
                        else -> "off"
                    }
                    if (editor.macMode == "custom" && editor.macAddress.isBlank()) {
                        editor.macAddress = my.shanunid.linespoofer.utils.ConfigManager.generateRandomMac()
                    }
                }
            )
            AnimatedVisibility(editor.macMode == "custom") {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    OutlinedTextField(
                        value = editor.macAddress,
                        onValueChange = { editor.macAddress = it },
                        label = { Text("Custom MAC (XX:XX:XX:XX:XX:XX)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(MiuiXColors.DarkDivider))

        // 3. Boot Time / Uptime Spoofing
        val uptimeModes = listOf("Off", "Random", "Custom")
        val currentUptimeIndex = when (editor.uptimeMode.lowercase()) {
            "random" -> 1
            "custom" -> 2
            else -> 0
        }
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Text(
                text = "Spoof Boot Time & Uptime",
                color = MiuiXColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Palsukan durasi menyala perangkat di SystemClock dan /proc/uptime",
                color = MiuiXColors.TextSecondary,
                fontSize = 12.5.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            MiuiSegmentedPill(
                options = uptimeModes,
                selectedIndex = currentUptimeIndex,
                onSelect = { idx ->
                    editor.uptimeMode = when (idx) {
                        1 -> "random"
                        2 -> "custom"
                        else -> "off"
                    }
                }
            )
            AnimatedVisibility(editor.uptimeMode == "custom") {
                val currentHours = (editor.uptimeSeconds.toLongOrNull() ?: 27720L) / 3600f
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    MiuiSliderRow(
                        title = "Durasi Uptime",
                        value = currentHours.coerceIn(1f, 72f),
                        valueRange = 1f..72f,
                        steps = 70,
                        valueText = "${currentHours.toInt()} Jam",
                        onValueChange = {
                            val sec = (it * 3600).toLong()
                            editor.uptimeSeconds = sec.toString()
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(MiuiXColors.DarkDivider))

        // 4. Wi-Fi & Bluetooth Spoofing
        val wifiModes = listOf("Off", "Random", "Custom")
        val currentWifiIndex = when (editor.wifiMode.lowercase()) {
            "random" -> 1
            "custom" -> 2
            else -> 0
        }
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Text(
                text = "Spoof Wi-Fi & Bluetooth",
                color = MiuiXColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Samarkan SSID, BSSID, RSSI, dan nama adapter Bluetooth",
                color = MiuiXColors.TextSecondary,
                fontSize = 12.5.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            MiuiSegmentedPill(
                options = wifiModes,
                selectedIndex = currentWifiIndex,
                onSelect = { idx ->
                    val mode = when (idx) {
                        1 -> "random"
                        2 -> "custom"
                        else -> "off"
                    }
                    editor.wifiMode = mode
                    editor.bluetoothMode = mode
                }
            )
            AnimatedVisibility(editor.wifiMode == "custom") {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    OutlinedTextField(
                        value = editor.wifiSsid,
                        onValueChange = { editor.wifiSsid = it },
                        label = { Text("Wi-Fi SSID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editor.bluetoothName,
                        onValueChange = { editor.bluetoothName = it },
                        label = { Text("Bluetooth Adapter Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(MiuiXColors.DarkDivider))

        // 5. Battery Telemetry Spoofing
        val batteryModes = listOf("Off", "Random", "Custom")
        val currentBatteryIndex = when (editor.batteryMode.lowercase()) {
            "random" -> 1
            "custom" -> 2
            else -> 0
        }
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Text(
                text = "Spoof Status & Kapasitas Baterai",
                color = MiuiXColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cegah fingerprint status daya dan emulator static battery detection",
                color = MiuiXColors.TextSecondary,
                fontSize = 12.5.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            MiuiSegmentedPill(
                options = batteryModes,
                selectedIndex = currentBatteryIndex,
                onSelect = { idx ->
                    editor.batteryMode = when (idx) {
                        1 -> "random"
                        2 -> "custom"
                        else -> "off"
                    }
                }
            )
            AnimatedVisibility(editor.batteryMode == "custom") {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    MiuiSliderRow(
                        title = "Persentase Baterai",
                        value = editor.batteryLevel,
                        valueRange = 1f..100f,
                        steps = 98,
                        valueText = "${editor.batteryLevel.toInt()}%",
                        onValueChange = { editor.batteryLevel = it }
                    )
                    MiuiSliderRow(
                        title = "Suhu Baterai (°C)",
                        value = editor.batteryTemp,
                        valueRange = 25f..45f,
                        steps = 40,
                        valueText = String.format(java.util.Locale.US, "%.1f°C", editor.batteryTemp),
                        onValueChange = { editor.batteryTemp = it }
                    )
                }
            }
        }
    }
}
