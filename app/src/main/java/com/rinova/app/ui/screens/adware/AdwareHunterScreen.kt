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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
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
import com.rinova.app.ui.components.ScanningProgressSheet
import com.rinova.app.ui.components.SkeletonListItem
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
            .background(SlateBackground)
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
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sniffer Jendela Aktif (Active Ad Sniffer)",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Saat iklan pop-up tiba-tiba muncul di HP pasien, tekan tombol ini detik itu juga untuk menangkap pelakunya.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(StatusCritical.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = null,
                            tint = StatusCritical,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Button(
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
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tangkap Pelaku Iklan Sekarang",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (capturedWindowPkg != null) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurfaceHighlight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Pelaku Layar Teratas Terdeteksi:",
                                color = StatusWarning,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = capturedWindowPkg ?: "",
                                color = CyanPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { selectedActionTarget = Pair(capturedWindowPkg!!, false) },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Bekukan", color = StatusWarning, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { selectedActionTarget = Pair(capturedWindowPkg!!, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Copot Paksa", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (actionNotification != null) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = actionNotification ?: "",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
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
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = {
                    coroutineScope.launch {
                        adwareHunterRepository.scanSuspiciousApps()
                    }
                },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isScanning) "Memindai..." else "Pindai Semua",
                    color = CyanOnPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Threat List
        if (isScanning) {
            repeat(3) { SkeletonListItem() }
        } else if (threats.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = StatusSafe,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Belum ada aplikasi mencurigakan atau overlay nakal yang terdeteksi. Jalankan 'Pindai Semua' untuk audit menyeluruh.",
                        color = TextSecondary,
                        fontSize = 13.sp,
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

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appName.ifEmpty { item.packageName.substringAfterLast(".") },
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.packageName,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(severityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.severity.name,
                        color = severityColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Evidence reason
            if (item.evidenceReason.isNotEmpty()) {
                Text(
                    text = "Bukti Sistem: ${item.evidenceReason}",
                    color = StatusWarning,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onFreeze,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = StatusWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bekukan", color = StatusWarning, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onUninstall,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copot", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}
