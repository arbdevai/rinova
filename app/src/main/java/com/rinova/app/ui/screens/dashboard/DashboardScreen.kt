package com.rinova.app.ui.screens.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinova.app.core.model.AdbConnectionState
import com.rinova.app.core.model.DeviceInfo
import com.rinova.app.data.repository.AdbRepository
import com.rinova.app.data.repository.StorageRepository
import com.rinova.app.ui.components.AlertType
import com.rinova.app.ui.components.CustomActionDialog
import com.rinova.app.ui.theme.CyanOnPrimary
import com.rinova.app.ui.theme.CyanPrimary
import com.rinova.app.ui.theme.SlateBackground
import com.rinova.app.ui.theme.SlateSurface
import com.rinova.app.ui.theme.SlateSurfaceHighlight
import com.rinova.app.ui.theme.SlateSurfaceVariant
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusSafe
import com.rinova.app.ui.theme.StatusWarning
import com.rinova.app.ui.theme.TextPrimary
import com.rinova.app.ui.theme.TextSecondary
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    connectionState: AdbConnectionState,
    adbRepository: AdbRepository,
    storageRepository: StorageRepository,
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
                .background(SlateBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "Perangkat Belum Terhubung",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sambungkan ke HP target melalui menu 'Sambungan' terlebih dahulu.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    val device = connectionState.device

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device Identification Banner
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            modifier = Modifier.fillMaxWidth()
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
                    Column {
                        Text(
                            text = "${device.brand} ${device.model}",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${device.manufacturer} • Codename: ${device.deviceCodename.ifEmpty { "Generic" }}",
                            color = CyanPrimary,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusSafe.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ONLINE",
                            color = StatusSafe,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatPill(
                        label = "OS Version",
                        value = "Android ${device.androidVersion} (SDK ${device.sdkInt})",
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        label = "Custom UI",
                        value = device.customRomBrand,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val batText = if (device.batteryLevel >= 0) "${device.batteryLevel}%${if (device.isCharging) " (Charging)" else ""}" else "N/A"
                    StatPill(
                        label = "Baterai",
                        value = batText,
                        modifier = Modifier.weight(1f)
                    )
                    val ramText = if (device.totalRamMb > 0) "${device.freeRamMb} / ${device.totalRamMb} MB" else "N/A"
                    StatPill(
                        label = "RAM Tersedia",
                        value = ramText,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (actionFeedback != null) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = actionFeedback ?: "",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // Quick Technician Tools
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
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
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            dialogTitle = "Reboot Target"
                            dialogMessage = "Mulai ulang HP target ke sistem normal?"
                            dialogCommand = "reboot"
                            dialogAlertType = AlertType.WARNING
                            onConfirmAction = {
                                MainScope().launch {
                                    adbRepository.rebootTarget()
                                    actionFeedback = "Perintah reboot normal terkirim."
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reboot", color = TextPrimary, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            dialogTitle = "Reboot ke Recovery"
                            dialogMessage = "Masuk ke mode Recovery HP target untuk wipe cache atau sideload firmware?"
                            dialogCommand = "reboot recovery"
                            dialogAlertType = AlertType.CRITICAL
                            onConfirmAction = {
                                MainScope().launch {
                                    adbRepository.rebootTarget("recovery")
                                    actionFeedback = "Perintah reboot recovery terkirim."
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Recovery", color = StatusWarning, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            dialogTitle = "Reboot ke Safe Mode"
                            dialogMessage = "Masuk ke Safe Mode target? Seluruh aplikasi pihak ketiga dan malware akan dinonaktifkan sementara."
                            dialogCommand = "setprop persist.sys.safemode 1 && reboot"
                            dialogAlertType = AlertType.CRITICAL
                            onConfirmAction = {
                                MainScope().launch {
                                    adbRepository.executeCommand("setprop persist.sys.safemode 1")
                                    adbRepository.rebootTarget()
                                    actionFeedback = "Target dimatikan untuk masuk ke Safe Mode."
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Safe Mode", color = CyanPrimary, fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = {
                        dialogTitle = "Paksa Bersihkan Cache Sistem"
                        dialogMessage = "Jalankan 'pm trim-caches' untuk menghapus seluruh file cache sementara tanpa menghapus data login penting pengguna?"
                        dialogCommand = "pm trim-caches 9999999999999"
                        dialogAlertType = AlertType.SUCCESS
                        onConfirmAction = {
                            MainScope().launch {
                                val res = storageRepository.trimSystemCaches()
                                res.fold(
                                    onSuccess = { actionFeedback = "Cache sistem berhasil dipangkas." },
                                    onFailure = { actionFeedback = "Gagal memangkas cache: ${it.localizedMessage}" }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = CyanOnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trim Cache Sistem Instan",
                        color = CyanOnPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Feature Shortcuts
        Text(
            text = "Modul Diagnostik",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        FeatureRowCard(
            title = "Deep Storage & Ghost Cache Buster",
            subtitle = "Pindai sisa folder OBB aplikasi terhapus dan crash dump di /sdcard",
            icon = Icons.Default.CleaningServices,
            accentColor = CyanPrimary,
            onClick = onNavigateToStorage
        )

        FeatureRowCard(
            title = "Virus Iklan & Rogue Overlay Hunter",
            subtitle = "Tangkap iklan pop-up aktif dan audit aplikasi tanpa ikon launcher",
            icon = Icons.Default.Shield,
            accentColor = StatusCritical,
            onClick = onNavigateToAdware
        )

        FeatureRowCard(
            title = "Universal Vendor Debloater",
            subtitle = "Nonaktifkan aman paket bloatware Xiaomi, Samsung, Oppo, Vivo, Infinix",
            icon = Icons.Default.Build,
            accentColor = StatusWarning,
            onClick = onNavigateToDebloat
        )

        FeatureRowCard(
            title = "Gemini AI Diagnostic Engine",
            subtitle = "Audit sistem berbasis bukti empiris dan rekomendasi eksekusi terkontrol",
            icon = Icons.Default.Memory,
            accentColor = StatusSafe,
            onClick = onNavigateToAi
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, color = TextSecondary, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun FeatureRowCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
