package com.rinova.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.rinova.app.core.update.UpdateManager
import com.rinova.app.core.update.UpdateState
import com.rinova.app.core.util.FileSizeFormatter
import com.rinova.app.data.repository.AdbRepository
import com.rinova.app.data.repository.StorageRepository
import com.rinova.app.ui.components.AlertType
import com.rinova.app.ui.components.CustomActionDialog
import com.rinova.app.core.health.HealthAuditEngine
import com.rinova.app.core.health.HealthCategory
import com.rinova.app.core.health.HealthIssue
import com.rinova.app.core.health.HealthScanResult
import com.rinova.app.core.health.HealthSeverity
import com.rinova.app.data.repository.SettingsRepository
import com.rinova.app.ui.components.HealthIssueCard
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.CircularProgressIndicator
import com.rinova.app.ui.components.RinovaBentoStat
import com.rinova.app.ui.components.RinovaSurfaceCard
import com.rinova.app.ui.components.RinovaTactileButton
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusSafe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    connectionState: AdbConnectionState,
    adbRepository: AdbRepository,
    storageRepository: StorageRepository,
    settingsRepository: SettingsRepository,
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
    val isProMode by settingsRepository.isProMode.collectAsStateWithLifecycle(false)
    val updateState by updateManager.updateState.collectAsStateWithLifecycle()
    val healthEngine = remember(adbRepository) { HealthAuditEngine(adbRepository) }
    var isScanningHealth by remember { mutableStateOf(false) }
    var healthScanResult by remember { mutableStateOf<HealthScanResult?>(null) }
    var scanStepText by remember { mutableStateOf("") }

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
            RinovaSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "Perangkat Belum Terhubung",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
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
        // HERO CARD: DEVICE IDENTITY (Industrial Bento Header)
        RinovaSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            shapeRadius = 20.dp
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
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "${device.brand} ${device.model}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
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
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(StatusSafe)
                            )
                            Text(
                                text = "ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusSafe,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // BENTO TELEMETRY GRID (4 Balanced Metric Tiles)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RinovaBentoStat(
                        icon = Icons.Default.Speed,
                        label = "Sistem Operasi",
                        primaryValue = "Android ${device.androidVersion}",
                        subValue = "SDK ${device.sdkInt} • Patch ${device.securityPatch}",
                        modifier = Modifier.weight(1f)
                    )
                    RinovaBentoStat(
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
                    RinovaBentoStat(
                        icon = Icons.Default.BatteryChargingFull,
                        label = "Baterai",
                        primaryValue = batText,
                        subValue = batStatus,
                        modifier = Modifier.weight(1f)
                    )

                    val ramPrimary = if (device.totalRamMb > 0) "${device.freeRamMb} MB" else "N/A"
                    val ramSub = if (device.totalRamMb > 0) "Bebas dari ${device.totalRamMb} MB" else "RAM Telemetri"
                    RinovaBentoStat(
                        icon = Icons.Default.Memory,
                        label = "Memori RAM",
                        primaryValue = ramPrimary,
                        subValue = ramSub,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 🩺 PIVOT UTAMA: 1-CLICK SMART HEALTH CHECK (DOKTER HP PINTAR)
        RinovaSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            shapeRadius = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Dokter HP Pintar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pemeriksaan 1-Klik Storage, Adware, RAM & Keamanan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (healthScanResult != null && !isScanningHealth) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (healthScanResult!!.totalIssuesFound > 0) StatusCritical.copy(alpha = 0.15f) else StatusSafe.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (healthScanResult!!.totalIssuesFound > 0) "${healthScanResult!!.totalIssuesFound} Masalah" else "Sehat Prima",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (healthScanResult!!.totalIssuesFound > 0) StatusCritical else StatusSafe,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (isScanningHealth) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF14171E))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Primary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = scanStepText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    RinovaTactileButton(
                        text = if (healthScanResult == null) "Periksa Kesehatan HP (1-Klik)" else "Pindai Ulang Kesehatan HP",
                        onClick = {
                            coroutineScope.launch {
                                isScanningHealth = true
                                scanStepText = "Memeriksa kapasitas memori & cache galeri..."
                                delay(350)
                                scanStepText = "Mendeteksi adware sponsor pabrikan..."
                                delay(350)
                                scanStepText = "Mengecek RAM Plus & throttler sistem..."
                                val res = healthEngine.runSmartHealthAudit(device)
                                healthScanResult = res
                                isScanningHealth = false
                            }
                        },
                        icon = Icons.Default.MedicalServices,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Daftar Kartu Masalah Temuan Dokter HP
                if (healthScanResult != null && !isScanningHealth) {
                    val result = healthScanResult!!
                    if (result.issues.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StatusSafe.copy(alpha = 0.15f))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusSafe,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Ponsel Dalam Kondisi Prima!",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StatusSafe,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tidak ditemukan cache membengkak, adware sponsor, atau pembatas performa.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            result.issues.forEach { issue ->
                                HealthIssueCard(
                                    issue = issue,
                                    isProMode = isProMode,
                                    onFixClick = { targetIssue ->
                                        dialogTitle = if (isProMode) "Eksekusi: ${targetIssue.title}" else "Perbaiki: ${targetIssue.title}"
                                        dialogMessage = if (isProMode) {
                                            "${targetIssue.humanDescription}\n\nPerintah ADB Shell:\n${targetIssue.technicalCommand}"
                                        } else {
                                            "${targetIssue.humanDescription}\n\n100% aman, tidak akan menghapus foto atau data pribadi Anda."
                                        }
                                        dialogCommand = if (isProMode) targetIssue.technicalCommand else null
                                        dialogAlertType = when (targetIssue.severity) {
                                            HealthSeverity.CRITICAL -> AlertType.CRITICAL
                                            HealthSeverity.WARNING -> AlertType.WARNING
                                            else -> AlertType.INFO
                                        }
                                        onConfirmAction = {
                                            coroutineScope.launch {
                                                val fixRes = healthEngine.executeFix(targetIssue)
                                                fixRes.fold(
                                                    onSuccess = {
                                                        actionFeedback = "Perbaikan '${targetIssue.title}' berhasil diterapkan!"
                                                        val updatedList = healthScanResult?.issues?.map {
                                                            if (it.id == targetIssue.id) it.copy(isResolved = true) else it
                                                        } ?: emptyList()
                                                        healthScanResult = healthScanResult?.copy(issues = updatedList)
                                                    },
                                                    onFailure = {
                                                        actionFeedback = "Gagal memperbaiki: ${it.localizedMessage}"
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // QUICK TECHNICIAN CONTROLS (Bespoke Tactile Actions)
        RinovaSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            shapeRadius = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Aksi Cepat Teknisi",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                // Primary Quick Action: Trim System Caches
                RinovaTactileButton(
                    text = "Trim Cache Sistem Instan",
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
                    icon = Icons.Default.CleaningServices,
                    modifier = Modifier.fillMaxWidth()
                )

                // Secondary Actions Row: Reboot Target / Safe Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RinovaTactileButton(
                        text = "Reboot",
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
                        icon = Icons.Default.RestartAlt,
                        isSecondary = true,
                        modifier = Modifier.weight(1f),
                        height = 44.dp
                    )

                    RinovaTactileButton(
                        text = "Safe Mode",
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
                        icon = Icons.Default.Shield,
                        isSecondary = true,
                        modifier = Modifier.weight(1f),
                        height = 44.dp
                    )
                }
            }
        }

        // IN-APP UPDATE CARD (Pixel System Update Style)
        RinovaSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            shapeRadius = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryContainer),
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
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
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
                            RinovaTactileButton(
                                text = "Cek Update",
                                onClick = {
                                    coroutineScope.launch {
                                        updateManager.checkForUpdates()
                                    }
                                },
                                isSecondary = true,
                                height = 36.dp
                            )
                        }
                        is UpdateState.Checking -> {
                            Text("Memeriksa...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is UpdateState.UpdateAvailable -> {
                            RinovaTactileButton(
                                text = "Unduh ${state.newVersion}",
                                onClick = {
                                    coroutineScope.launch {
                                        updateManager.downloadUpdate(
                                            state.downloadUrl,
                                            state.newVersion,
                                            state.fileSizeBytes
                                        )
                                    }
                                },
                                icon = Icons.Default.Download,
                                height = 36.dp
                            )
                        }
                        is UpdateState.Downloading -> {
                            Text("${state.progressPercent}%", style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold)
                        }
                        is UpdateState.ReadyToInstall -> {
                            RinovaTactileButton(
                                text = "Pasang Update",
                                onClick = { updateManager.installApk(state.apkFile) },
                                height = 36.dp
                            )
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
                            trackColor = Color(0xFF282D38)
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
            RinovaSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 14.dp
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
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
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
private fun FeatureRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    RinovaSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shapeRadius = 16.dp,
        onClick = onClick
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
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
