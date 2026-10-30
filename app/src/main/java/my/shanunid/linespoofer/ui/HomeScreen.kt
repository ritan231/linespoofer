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
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { listState.scrollToItem(0) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MiuiXColors.DarkBg)
            .padding(scaffoldPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 1. HyperOS Hero Card (Module Status + Master Switch)
        item {
            val heroShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(heroShape)
                    .background(
                        Brush.verticalGradient(
                            colors = if (moduleActivated && appState.isMasterSpoofActive) {
                                listOf(Color(0xFF152238), Color(0xFF11141C))
                            } else {
                                listOf(Color(0xFF22171A), Color(0xFF151418))
                            }
                        )
                    )
                    .border(
                        1.dp,
                        if (moduleActivated && appState.isMasterSpoofActive) MiuiXColors.Blue.copy(alpha = 0.4f) else MiuiXColors.DarkBorder,
                        heroShape
                    )
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
                                text = if (moduleActivated) "MODULE ACTIVE" else "MODULE INACTIVE",
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
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (appState.isMasterSpoofActive) {
                            "Engine spoofing aktif — Sistem & aplikasi otomatis ter-spoof"
                        } else {
                            "Master switch dimatikan — Hook dilewati"
                        },
                        color = MiuiXColors.TextSecondary,
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
                // Card 1: Target FPS
                MiuiCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = MiuiXColors.DarkCard,
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
                                text = "DISPLAY FPS",
                                color = MiuiXColors.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                text = "${appState.editor.targetFps.toInt()} FPS",
                                color = MiuiXColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Card 2: Target RAM
                MiuiCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = MiuiXColors.DarkCard,
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
                                text = "TOTAL RAM",
                                color = MiuiXColors.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                            val ramGb = ConfigManager.getTotalRamBytes() / (1024L * 1024L * 1024L)
                            Text(
                                text = if (ramGb > 0) "$ramGb GB" else "12 GB",
                                color = MiuiXColors.TextPrimary,
                                fontSize = 15.sp,
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
                    backgroundColor = MiuiXColors.DarkCard,
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
                                text = "BOOT UPTIME",
                                color = MiuiXColors.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                            val sec = ConfigManager.getSpoofedUptimeSeconds()
                            val hrs = sec / 3600
                            val mins = (sec % 3600) / 60
                            Text(
                                text = "${hrs}h ${mins}m",
                                color = MiuiXColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Card 4: MAC Mode
                MiuiCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = MiuiXColors.DarkCard,
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
                                text = "MAC SPOOF",
                                color = MiuiXColors.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                text = appState.editor.macMode.uppercase(),
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
            MiuiSectionHeader(title = "INFORMASI PROFIL AKTIF")
        }

        // 4. Grouped Profile Details Card
        item {
            MiuiCard {
                DetailItem(
                    icon = Icons.Outlined.PhoneAndroid,
                    label = "Model Perangkat",
                    value = profile.model,
                )
                Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(MiuiXColors.DarkDivider))
                DetailItem(
                    icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    label = "Preset Perangkat",
                    value = "${appState.presetLabel(config.selectedPresetId)} (${if (config.isCustomMode) "Kustom" else "Preset"})",
                )
                Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(MiuiXColors.DarkDivider))
                DetailItem(
                    icon = Icons.Outlined.Info,
                    label = "Versi Android / SDK",
                    value = "Android ${profile.buildRelease} (API ${profile.buildSdk})",
                )
                Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(MiuiXColors.DarkDivider))
                DetailItem(
                    icon = Icons.Outlined.AspectRatio,
                    label = "Resolusi & Densitas Layar",
                    value = "${profile.screenWidth} × ${profile.screenHeight} (${profile.screenDensity} dpi)",
                )
                Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(MiuiXColors.DarkDivider))
                DetailItem(
                    icon = Icons.Outlined.Folder,
                    label = "Berkas Konfigurasi",
                    value = "${config.configFile.name} (${DateFormat.format("yyyy-MM-dd HH:mm", config.configFile.lastModified())})",
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
                color = MiuiXColors.TextSecondary,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value.ifBlank { "-" },
                color = MiuiXColors.TextPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
