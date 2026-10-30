package my.shanunid.linespoofer.ui

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.shanunid.linespoofer.R
import my.shanunid.linespoofer.utils.ConfigManager

@Composable
fun HomeScreen(appState: SpoofAppState, scaffoldPadding: PaddingValues) {
    val config = appState.loadedConfig ?: return
    val profile = config.profile
    val moduleActivated = appState.isModuleActivated()
    val colors = MiuiTheme.colors
    val isDark = colors.isDark
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { listState.scrollToItem(0) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(scaffoldPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 1. HyperOS Hero Card (Module Status + Master Switch)
        item {
            val heroShape = RoundedCornerShape(24.dp)
            val heroGradientColors = if (isDark) {
                if (moduleActivated && appState.isMasterSpoofActive) {
                    listOf(Color(0xFF152238), Color(0xFF11141C))
                } else {
                    listOf(Color(0xFF22171A), Color(0xFF151418))
                }
            } else {
                if (moduleActivated && appState.isMasterSpoofActive) {
                    listOf(Color(0xFFE5F1FF), Color(0xFFD6E8FF))
                } else {
                    listOf(Color(0xFFFFEEEE), Color(0xFFFFF0F0))
                }
            }
            val heroBorderColor = if (moduleActivated && appState.isMasterSpoofActive) {
                MiuiXColors.Blue.copy(alpha = 0.4f)
            } else {
                if (isDark) MiuiXColors.DarkBorder else MiuiXColors.LightBorder
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(heroShape)
                    .background(Brush.verticalGradient(colors = heroGradientColors))
                    .border(1.dp, heroBorderColor, heroShape)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Status Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (moduleActivated) MiuiXColors.GreenContainer else Color(0x22FF3B30)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (moduleActivated) MiuiXColors.Green else MiuiXColors.Red)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (moduleActivated) {
                                    stringResource(R.string.home_badge_active)
                                } else {
                                    stringResource(R.string.home_badge_inactive)
                                },
                                color = if (moduleActivated) MiuiXColors.Green else MiuiXColors.Red,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                        }

                        // Master Switch
                        MiuiSwitch(
                            checked = appState.isMasterSpoofActive,
                            onCheckedChange = { appState.updateMasterSpoofActive(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = profile.model.ifBlank { "LineSpoofer Target" },
                        color = if (isDark) Color.White else colors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (appState.isMasterSpoofActive) {
                            stringResource(R.string.home_master_active)
                        } else {
                            stringResource(R.string.home_master_inactive)
                        },
                        color = if (isDark) MiuiXColors.TextSecondary else colors.textSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }

        // 2. HyperOS Quick Telemetry Grid (2x2 Cards)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Card 1: Target FPS (Strictly 30, 60, 90, 120, 144)
                MiuiCard(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MiuiXColors.BlueContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AspectRatio,
                                contentDescription = null,
                                tint = MiuiXColors.Blue,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.home_telemetry_fps),
                                color = colors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                            val validFps = when (appState.editor.targetFps.toInt()) {
                                30 -> 30
                                60 -> 60
                                90 -> 90
                                144 -> 144
                                else -> 120
                            }
                            Text(
                                text = "$validFps FPS",
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Card 2: Target RAM (Physical + Virtual)
                MiuiCard(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MiuiXColors.BlueContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Memory,
                                contentDescription = null,
                                tint = MiuiXColors.Blue,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.home_telemetry_ram),
                                color = colors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                            val physicalGb = remember { ConfigManager.getPhysicalRamBytes() / (1024L * 1024L * 1024L) }
                            val virtualGb = remember { ConfigManager.getVirtualRamBytes() / (1024L * 1024L * 1024L) }
                            val ramText = if (virtualGb > 0) "$physicalGb GB (+$virtualGb GB)" else "$physicalGb GB"
                            Text(
                                text = ramText,
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Card 3: Uptime
                MiuiCard(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MiuiXColors.BlueContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = MiuiXColors.Blue,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.home_telemetry_uptime),
                                color = colors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                            val isSpoofEnabled = appState.editor.uptimeMode != "off"
                            val baseSec = remember(isSpoofEnabled, appState.editor.uptimeSeconds) {
                                if (isSpoofEnabled) {
                                    (appState.editor.uptimeSeconds.toLongOrNull() ?: ConfigManager.getSpoofedUptimeSeconds())
                                } else 0L
                            }
                            var liveUptimeSec by remember(baseSec) {
                                mutableStateOf(baseSec + (android.os.SystemClock.elapsedRealtime() / 1000L))
                            }
                            LaunchedEffect(baseSec) {
                                while (true) {
                                    liveUptimeSec = baseSec + (android.os.SystemClock.elapsedRealtime() / 1000L)
                                    kotlinx.coroutines.delay(1000L)
                                }
                            }
                            val hrs = liveUptimeSec / 3600
                            val mins = (liveUptimeSec % 3600) / 60
                            Text(
                                text = "${hrs}h ${mins}m",
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Card 4: MAC Mode
                MiuiCard(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MiuiXColors.BlueContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Wifi,
                                contentDescription = null,
                                tint = MiuiXColors.Blue,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.home_telemetry_mac),
                                color = colors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                            val macLabel = when (appState.editor.macMode.lowercase()) {
                                "random" -> stringResource(R.string.mode_random)
                                "custom" -> stringResource(R.string.mode_custom)
                                else -> stringResource(R.string.mode_off)
                            }
                            Text(
                                text = macLabel.uppercase(),
                                color = MiuiXColors.Blue,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // 3. Section Title
        item {
            MiuiSectionHeader(title = stringResource(R.string.home_section_active_profile))
        }

        // 4. Grouped Profile Details Card
        item {
            MiuiCard {
                DetailItem(
                    icon = Icons.Outlined.PhoneAndroid,
                    label = stringResource(R.string.home_detail_model),
                    value = profile.model,
                )
                Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(colors.divider))
                DetailItem(
                    icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    label = stringResource(R.string.home_detail_preset),
                    value = "${appState.presetLabel(config.selectedPresetId)} (${if (config.isCustomMode) stringResource(R.string.home_mode_custom) else stringResource(R.string.home_mode_preset)})",
                )
                Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(colors.divider))
                DetailItem(
                    icon = Icons.Outlined.Info,
                    label = stringResource(R.string.home_detail_android),
                    value = "Android ${profile.buildRelease} (API ${profile.buildSdk})",
                )
                Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(colors.divider))
                DetailItem(
                    icon = Icons.Outlined.AspectRatio,
                    label = stringResource(R.string.home_detail_display),
                    value = "${profile.screenWidth} × ${profile.screenHeight} (${profile.screenDensity} dpi)",
                )
                Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(colors.divider))
                val configDateText = remember(config.configFile) {
                    try {
                        DateFormat.format("yyyy-MM-dd HH:mm", config.configFile.lastModified()).toString()
                    } catch (e: Exception) {
                        "Active"
                    }
                }
                DetailItem(
                    icon = Icons.Outlined.Folder,
                    label = stringResource(R.string.home_detail_config),
                    value = "${config.configFile.name} ($configDateText)",
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String,
) {
    val colors = MiuiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MiuiXColors.BlueContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuiXColors.Blue,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = colors.textSecondary,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value.ifBlank { "-" },
                color = colors.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
