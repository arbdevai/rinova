package com.rinova.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinova.app.BuildConfig
import com.rinova.app.core.model.AdbConnectionState
import com.rinova.app.core.model.DeviceInfo
import com.rinova.app.core.update.UpdateManager
import com.rinova.app.core.update.UpdateState
import com.rinova.app.core.util.FileSizeFormatter
import com.rinova.app.data.repository.AdbRepository
import com.rinova.app.data.repository.StorageRepository
import com.rinova.app.ui.components.AlertType
import com.rinova.app.ui.components.CustomActionDialog
import com.rinova.app.ui.theme.Outline
import com.rinova.app.ui.theme.OutlineVariant
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusSafe
import com.rinova.app.ui.theme.StatusWarning
import com.rinova.app.ui.theme.SurfaceContainer
import com.rinova.app.ui.theme.SurfaceContainerHigh
import com.rinova.app.ui.theme.SurfaceContainerLow
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    connectionState: AdbConnectionState,
    adbRepository: AdbRepository,
    storageRepository: StorageRepository,
    updateManager: UpdateManager,
    onNavigateToStorage: () -> Unit,
    onNavigateToAdware: () -> Unit,
    onNavigateToDebloat: () -> Unit,
    onNavigateToAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var dialogCommand by remember { mutableStateOf<String?>(null) }
    var dialogAlertType by remember { mutableStateOf(AlertType.WARNING) }
    var onConfirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val updateState by updateManager.updateState.collectAsStateWithLifecycle()

    if (onConfirmAction != null) {
        CustomActionDialog(
            title = dialogTitle,
            message = dialogMessage,
            commandPreview = dialogCommand,
            alertType = dialogAlertType,
            onConfirm = {
                val action = onConfirmAction
                onConfirmAction = null
                action?.invoke()
            },
            onDismiss = { onConfirmAction = null }
        )
    }

    if (connectionState !is AdbConnectionState.Connected) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "Perangkat Belum Terhubung",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Buka tab 'Sambungan' untuk menghubungkan ponsel target melalui ADB Wireless (Kode 6-Digit atau QR Code).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val device = connectionState.device

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HERO CARD: DEVICE IDENTITY (Google Material 3 Standard)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Device Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "${device.brand} ${device.model}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "${device.manufacturer} • ${device.deviceCodename.ifEmpty { "Generic" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tonal Online Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(StatusSafe.copy(alpha = 0.15f))
                            .border(0.8.dp, StatusSafe.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StatusSafe)
                            )
                            Text(
                                text = "ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusSafe,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // BENTO TELEMETRY GRID (4 Balanced Metric Tiles)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoTile(
                        icon = Icons.Default.Speed,
                        label = "Sistem Operasi",
                        primaryValue = "Android ${device.androidVersion}",
                        subValue = "SDK ${device.sdkInt} • Patch ${device.securityPatch}",
                        modifier = Modifier.weight(1f)
                    )
                    BentoTile(
                        icon = Icons.Default.Build,
                        label = "Custom ROM",
                        primaryValue = device.customRomBrand,
                        subValue = device.cpuAbi,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val batText = if (device.batteryLevel >= 0) "${device.batteryLevel}%" else "N/A"
                    val batStatus = if (device.isCharging) "Mengisi Daya" else "Baterai Normal"
                    BentoTile(
                        icon = Icons.Default.BatteryChargingFull,
                        label = "Baterai",
                        primaryValue = batText,
                        subValue = batStatus,
                        modifier = Modifier.weight(1f)
                    )

                    val ramPrimary = if (device.totalRamMb > 0) "${device.freeRamMb} MB" else "N/A"
                    val ramSub = if (device.totalRamMb > 0) "Bebas dari ${device.totalRamMb} MB" else "RAM Telemetri"
                    BentoTile(
                        icon = Icons.Default.Memory,
                        label = "Memori RAM",
                        primaryValue = ramPrimary,
                        subValue = ramSub,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // QUICK TECHNICIAN CONTROLS (Grouped Clean Architecture)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Aksi Cepat Teknisi",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Primary Quick Action: Trim System Caches
                Button(
                    onClick = {
                        dialogTitle = "Paksa Bersihkan Cache Sistem"
                        dialogMessage = "Jalankan 'pm trim-caches' untuk menghapus seluruh file cache sementara tanpa menghapus data login penting pengguna?"
                        dialogCommand = "pm trim-caches 9999999999999"
                        dialogAlertType = AlertType.SUCCESS
                        onConfirmAction = {
                            coroutineScope.launch {
                                val res = storageRepository.trimSystemCaches()
                                res.fold(
                                    onSuccess = { actionFeedback = "Cache sistem berhasil dipangkas." },
                                    onFailure = { actionFeedback = "Gagal memangkas cache: ${it.localizedMessage}" }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trim Cache Sistem Instan",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Secondary Actions Row: Reboot Target / Safe Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            dialogTitle = "Reboot Target"
                            dialogMessage = "Mulai ulang HP target ke sistem normal?"
                            dialogCommand = "reboot"
                            dialogAlertType = AlertType.WARNING
                            onConfirmAction = {
                                coroutineScope.launch {
                                    adbRepository.rebootTarget()
                                    actionFeedback = "Perintah reboot normal terkirim."
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = SurfaceContainerHigh),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reboot", style = MaterialTheme.typography.labelMedium)
                    }

                    FilledTonalButton(
                        onClick = {
                            dialogTitle = "Reboot ke Safe Mode"
                            dialogMessage = "Masuk ke Safe Mode target? Seluruh aplikasi pihak ketiga dan malware akan dinonaktifkan sementara."
                            dialogCommand = "setprop persist.sys.safemode 1 && reboot"
                            dialogAlertType = AlertType.CRITICAL
                            onConfirmAction = {
                                coroutineScope.launch {
                                    adbRepository.executeCommand("setprop persist.sys.safemode 1")
                                    adbRepository.rebootTarget()
                                    actionFeedback = "Target dimatikan untuk masuk ke Safe Mode."
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = SurfaceContainerHigh),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Safe Mode", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // IN-APP UPDATE CARD (Pixel System Update Style)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Pembaruan Rinova",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Versi Saat Ini: v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    when (val state = updateState) {
                        is UpdateState.Idle, is UpdateState.Error -> {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        updateManager.checkForUpdates()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cek Update", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        is UpdateState.Checking -> {
                            Text("Memeriksa...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is UpdateState.UpdateAvailable -> {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        updateManager.downloadUpdate(
                                            state.downloadUrl,
                                            state.newVersion,
                                            state.fileSizeBytes
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unduh ${state.newVersion}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        is UpdateState.Downloading -> {
                            Text("${state.progressPercent}%", style = MaterialTheme.typography.labelSmall, color = Primary)
                        }
                        is UpdateState.ReadyToInstall -> {
                            Button(
                                onClick = { updateManager.installApk(state.apkFile) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusSafe)
                            ) {
                                Text("Pasang Update", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                when (val state = updateState) {
                    is UpdateState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { state.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Primary,
                            trackColor = SurfaceContainerHigh
                        )
                    }
                    is UpdateState.UpdateAvailable -> {
                        Text(
                            text = "Tersedia versi baru ${state.newVersion} (${FileSizeFormatter.format(state.fileSizeBytes)}).",
                            color = StatusSafe,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is UpdateState.Error -> {
                        Text(
                            text = state.message,
                            color = StatusCritical,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    else -> {}
                }
            }
        }

        if (actionFeedback != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = actionFeedback ?: "",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // DIAGNOSTIC MODULES (Clean Production List)
        Text(
            text = "Modul Diagnostik",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        FeatureRowCard(
            title = "Deep Storage & Ghost Cache Buster",
            subtitle = "Pindai sisa folder OBB aplikasi terhapus dan crash dump di /sdcard",
            icon = Icons.Default.CleaningServices,
            onClick = onNavigateToStorage
        )

        FeatureRowCard(
            title = "Virus Iklan & Rogue Overlay Hunter",
            subtitle = "Tangkap iklan pop-up aktif dan audit aplikasi tanpa ikon launcher",
            icon = Icons.Default.Shield,
            onClick = onNavigateToAdware
        )

        FeatureRowCard(
            title = "Universal Vendor Debloater",
            subtitle = "Nonaktifkan aman paket bloatware Xiaomi, Samsung, Oppo, Vivo, Infinix",
            icon = Icons.Default.Build,
            onClick = onNavigateToDebloat
        )

        FeatureRowCard(
            title = "Gemini AI Diagnostic Engine",
            subtitle = "Audit sistem berbasis bukti empiris dan rekomendasi eksekusi terkontrol",
            icon = Icons.Default.AutoAwesome,
            onClick = onNavigateToAi
        )
    }
}

@Composable
private fun BentoTile(
    icon: ImageVector,
    label: String,
    primaryValue: String,
    subValue: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, OutlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FeatureRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
