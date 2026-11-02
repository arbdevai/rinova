package com.rinova.app.ui.screens.storage

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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.rinova.app.core.model.OrphanedDirectory
import com.rinova.app.core.util.FileSizeFormatter
import com.rinova.app.data.repository.StorageRepository
import com.rinova.app.ui.components.AlertType
import com.rinova.app.ui.components.CustomActionDialog
import com.rinova.app.ui.components.RinovaSurfaceCard
import com.rinova.app.ui.components.RinovaTactileButton
import com.rinova.app.ui.components.ScanningProgressSheet
import com.rinova.app.ui.components.SkeletonListItem
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusSafe
import com.rinova.app.ui.theme.StatusWarning
import kotlinx.coroutines.launch

@Composable
fun StorageScreen(
    storageRepository: StorageRepository,
    modifier: Modifier = Modifier
) {
    val storageState by storageRepository.storageState.collectAsStateWithLifecycle()
    var progressVal by remember { mutableFloatStateOf(0f) }
    var currentStep by remember { mutableStateOf("") }
    var showScanSheet by remember { mutableStateOf(false) }

    var folderToDelete by remember { mutableStateOf<OrphanedDirectory?>(null) }
    var actionNotification by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    if (folderToDelete != null) {
        val target = folderToDelete!!
        CustomActionDialog(
            title = "Hapus Folder Yatim OBB / Data",
            message = "Hapus direktori '${target.folderName}' (${FileSizeFormatter.format(target.sizeBytes)})? Aplikasi pemilik sudah tidak terpasang di HP target.",
            commandPreview = "rm -rf \"${target.path}\"",
            alertType = AlertType.CRITICAL,
            confirmButtonText = "Hapus Permanen",
            onConfirm = {
                val p = target.path
                folderToDelete = null
                coroutineScope.launch {
                    val res = storageRepository.deleteOrphanedFolder(p)
                    res.fold(
                        onSuccess = {
                            actionNotification = "Folder sisa berhasil dibersihkan dari penyimpanan."
                            storageRepository.analyzeStorage()
                        },
                        onFailure = { actionNotification = "Gagal menghapus: ${it.localizedMessage}" }
                    )
                }
            },
            onDismiss = { folderToDelete = null }
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
        // Scanning Progress Alert Sheet
        if (showScanSheet || storageState.isAnalyzing) {
            ScanningProgressSheet(
                title = "Memindai Penyimpanan Mendalam",
                currentStepText = if (currentStep.isNotEmpty()) currentStep else storageState.currentStepText,
                progress = progressVal,
                onCancel = { showScanSheet = false }
            )
        }

        // Storage Overview Card
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
                    Column {
                        Text(
                            text = "Partisi Penyimpanan Internal (/data)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Inspeksi diskstats & direktori riil sistem",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SdStorage,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (storageState.totalBytes > 0L) {
                    val percentUsed = (storageState.usedBytes.toFloat() / storageState.totalBytes.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { percentUsed },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (percentUsed > 0.9f) StatusCritical else Primary,
                        trackColor = Color(0xFF282D38)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Terpakai: ${FileSizeFormatter.format(storageState.usedBytes)} (${(percentUsed * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Bebas: ${FileSizeFormatter.format(storageState.freeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusSafe,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Data penyimpanan belum dipindai. Tekan tombol di bawah untuk memulai.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RinovaTactileButton(
                        text = if (storageState.isAnalyzing) "Memindai..." else "Pindai Mendalam",
                        onClick = {
                            showScanSheet = true
                            progressVal = 0.1f
                            coroutineScope.launch {
                                storageRepository.analyzeStorage { step, p ->
                                    currentStep = step
                                    progressVal = p
                                }
                                showScanSheet = false
                            }
                        },
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.weight(1f)
                    )

                    RinovaTactileButton(
                        text = "Trim Cache",
                        onClick = {
                            coroutineScope.launch {
                                val res = storageRepository.trimSystemCaches()
                                res.fold(
                                    onSuccess = { actionNotification = "Cache sistem berhasil dipangkas via ADB." },
                                    onFailure = { actionNotification = "Gagal memangkas cache: ${it.localizedMessage}" }
                                )
                            }
                        },
                        icon = Icons.Default.CleaningServices,
                        isSecondary = true,
                        modifier = Modifier.weight(1f)
                    )
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

        // Breakdown Categories
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StorageStatCard(
                title = "Cache Aplikasi",
                sizeText = FileSizeFormatter.format(storageState.appCacheBytes),
                hint = "Data sementara sistem",
                color = Primary,
                modifier = Modifier.weight(1f)
            )
            StorageStatCard(
                title = "Sisa OBB Yatim",
                sizeText = FileSizeFormatter.format(storageState.orphanedObbBytes),
                hint = "Folder tanpa pemilik",
                color = if (storageState.orphanedObbBytes > 0) StatusCritical else StatusSafe,
                modifier = Modifier.weight(1f)
            )
            StorageStatCard(
                title = "Crash Dump",
                sizeText = FileSizeFormatter.format(storageState.crashDumpsBytes),
                hint = "Tombstones & ANR",
                color = StatusWarning,
                modifier = Modifier.weight(1f)
            )
        }

        // Orphaned Directories Section
        Text(
            text = "Daftar Folder Yatim Terdeteksi (${storageState.orphanedDirectories.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        if (storageState.isAnalyzing) {
            repeat(2) { SkeletonListItem() }
        } else if (storageState.orphanedDirectories.isEmpty()) {
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
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = null,
                            tint = StatusSafe,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Tidak ditemukan folder sisa OBB atau data aplikasi yang terhapus di /sdcard/Android.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            storageState.orphanedDirectories.forEach { folder ->
                RinovaSurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.folderName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = folder.path,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ukuran: ${FileSizeFormatter.format(folder.sizeBytes)} • ${folder.reason}",
                                color = StatusWarning,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        RinovaTactileButton(
                            text = "Hapus",
                            onClick = { folderToDelete = folder },
                            icon = Icons.Default.DeleteOutline,
                            height = 36.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageStatCard(
    title: String,
    sizeText: String,
    hint: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    RinovaSurfaceCard(
        modifier = modifier,
        shapeRadius = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = sizeText,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}
