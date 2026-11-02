package com.rinova.app.ui.screens.adware

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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinova.app.core.model.RogueAppItem
import com.rinova.app.core.model.ThreatSeverity
import com.rinova.app.data.repository.AdwareHunterRepository
import com.rinova.app.ui.components.AlertType
import com.rinova.app.ui.components.CustomActionDialog
import com.rinova.app.ui.components.RinovaSurfaceCard
import com.rinova.app.ui.components.RinovaTactileButton
import com.rinova.app.ui.components.ScanningProgressSheet
import com.rinova.app.ui.components.SkeletonListItem
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusSafe
import com.rinova.app.ui.theme.StatusWarning
import kotlinx.coroutines.launch

@Composable
fun AdwareHunterScreen(
    adwareHunterRepository: AdwareHunterRepository,
    modifier: Modifier = Modifier
) {
    val threats by adwareHunterRepository.scannedThreats.collectAsStateWithLifecycle()
    val isScanning by adwareHunterRepository.isScanning.collectAsStateWithLifecycle()
    val currentStep by adwareHunterRepository.currentScanningStep.collectAsStateWithLifecycle()

    var capturedWindowPkg by remember { mutableStateOf<String?>(null) }
    var actionNotification by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var selectedActionTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // Pair(packageName, isUninstall)

    if (selectedActionTarget != null) {
        val (pkg, isUninstall) = selectedActionTarget!!
        val actionTitle = if (isUninstall) "Paksa Copot Aplikasi (Uninstall)" else "Bekukan Aplikasi (Freeze)"
        val actionMessage = if (isUninstall) {
            "Apakah Anda yakin ingin mencopot paket '$pkg' dari perangkat target?"
        } else {
            "Apakah Anda yakin ingin menonaktifkan '$pkg'? Aplikasi ini akan dihentikan dan disembunyikan dari sistem."
        }
        val command = if (isUninstall) "pm uninstall --user 0 $pkg" else "pm disable-user --user 0 $pkg"

        CustomActionDialog(
            title = actionTitle,
            message = actionMessage,
            commandPreview = command,
            alertType = AlertType.CRITICAL,
            confirmButtonText = if (isUninstall) "Copot Sekarang" else "Bekukan",
            onConfirm = {
                selectedActionTarget = null
                coroutineScope.launch {
                    val res = if (isUninstall) {
                        adwareHunterRepository.uninstallApp(pkg)
                    } else {
                        adwareHunterRepository.freezeApp(pkg)
                    }
                    res.fold(
                        onSuccess = {
                            actionNotification = "Tindakan terhadap $pkg berhasil."
                            adwareHunterRepository.scanSuspiciousApps()
                        },
                        onFailure = { actionNotification = "Gagal memproses: ${it.localizedMessage}" }
                    )
                }
            },
            onDismiss = { selectedActionTarget = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isScanning) {
            ScanningProgressSheet(
                title = "Memindai Izin & Malware Tersembunyi",
                currentStepText = currentStep,
                progress = -1f,
                onCancel = {}
            )
        }

        // Active Ad Sniffer Banner
        RinovaSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            shapeRadius = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sniffer Jendela Aktif (Active Ad Sniffer)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Saat iklan pop-up tiba-tiba muncul di HP pasien, tekan tombol ini detik itu juga untuk menangkap pelakunya.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(StatusCritical.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = null,
                            tint = StatusCritical,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                RinovaTactileButton(
                    text = "Tangkap Pelaku Iklan Sekarang",
                    onClick = {
                        coroutineScope.launch {
                            val res = adwareHunterRepository.captureTopActiveWindow()
                            res.fold(
                                onSuccess = {
                                    capturedWindowPkg = it
                                    actionNotification = "Jendela aktif berhasil ditangkap: $it"
                                },
                                onFailure = {
                                    actionNotification = "Tidak ada jendela aktif yang terdeteksi."
                                }
                            )
                        }
                    },
                    icon = Icons.Default.CenterFocusStrong,
                    modifier = Modifier.fillMaxWidth()
                )

                if (capturedWindowPkg != null) {
                    RinovaSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 14.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Pelaku Layar Teratas Terdeteksi:",
                                color = StatusWarning,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = capturedWindowPkg ?: "",
                                color = Primary,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RinovaTactileButton(
                                    text = "Bekukan",
                                    onClick = { selectedActionTarget = Pair(capturedWindowPkg!!, false) },
                                    isSecondary = true,
                                    height = 38.dp,
                                    modifier = Modifier.weight(1f)
                                )
                                RinovaTactileButton(
                                    text = "Copot Paksa",
                                    onClick = { selectedActionTarget = Pair(capturedWindowPkg!!, true) },
                                    height = 38.dp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (actionNotification != null) {
            RinovaSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 14.dp
            ) {
                Text(
                    text = actionNotification ?: "",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // Suspicious Apps Header & Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Audit Aplikasi Berisiko & Overlay",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            RinovaTactileButton(
                text = if (isScanning) "Memindai..." else "Pindai Semua",
                onClick = {
                    coroutineScope.launch {
                        adwareHunterRepository.scanSuspiciousApps()
                    }
                },
                icon = Icons.Default.Refresh,
                enabled = !isScanning,
                height = 36.dp
            )
        }

        // Threat List
        if (isScanning) {
            repeat(3) { SkeletonListItem() }
        } else if (threats.isEmpty()) {
            RinovaSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(StatusSafe.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = StatusSafe,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Belum ada aplikasi mencurigakan atau overlay nakal yang terdeteksi. Jalankan 'Pindai Semua' untuk audit menyeluruh.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            threats.forEach { item ->
                ThreatItemCard(
                    item = item,
                    onFreeze = { selectedActionTarget = Pair(item.packageName, false) },
                    onUninstall = { selectedActionTarget = Pair(item.packageName, true) }
                )
            }
        }
    }
}

@Composable
private fun ThreatItemCard(
    item: RogueAppItem,
    onFreeze: () -> Unit,
    onUninstall: () -> Unit
) {
    val severityColor = when (item.severity) {
        ThreatSeverity.CRITICAL -> StatusCritical
        ThreatSeverity.HIGH -> StatusWarning
        ThreatSeverity.MEDIUM -> Color(0xFFFFCA28)
        ThreatSeverity.LOW -> StatusSafe
        ThreatSeverity.SAFE -> StatusSafe
    }

    RinovaSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shapeRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appName.ifEmpty { item.packageName.substringAfterLast(".") },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(severityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.severity.name,
                        color = severityColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Evidence reason
            if (item.evidenceReason.isNotEmpty()) {
                Text(
                    text = "Bukti Sistem: ${item.evidenceReason}",
                    color = StatusWarning,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                RinovaTactileButton(
                    text = "Bekukan",
                    onClick = onFreeze,
                    icon = Icons.Default.Block,
                    isSecondary = true,
                    height = 36.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                RinovaTactileButton(
                    text = "Copot",
                    onClick = onUninstall,
                    icon = Icons.Default.Delete,
                    height = 36.dp
                )
            }
        }
    }
}
