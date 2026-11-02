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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.rinova.app.ui.components.ScanningProgressSheet
import com.rinova.app.ui.components.SkeletonCard
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
import kotlinx.coroutines.MainScope
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
                MainScope().launch {
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
            .background(SlateBackground)
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
                    Column {
                        Text(
                            text = "Partisi Penyimpanan Internal (/data)",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Inspeksi diskstats & direktori riil sistem",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SdStorage,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
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
                        color = if (percentUsed > 0.9f) StatusCritical else CyanPrimary,
                        trackColor = SlateSurfaceHighlight
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Terpakai: ${FileSizeFormatter.format(storageState.usedBytes)} (${(percentUsed * 100).toInt()}%)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Bebas: ${FileSizeFormatter.format(storageState.freeBytes)}",
                            color = StatusSafe,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Data penyimpanan belum dipindai. Tekan tombol di bawah untuk memulai.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            showScanSheet = true
                            progressVal = 0.1f
                            MainScope().launch {
                                storageRepository.analyzeStorage { step, p ->
                                    currentStep = step
                                    progressVal = p
                                }
                                showScanSheet = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = CyanOnPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (storageState.isAnalyzing) "Memindai..." else "Pindai Mendalam",
                            color = CyanOnPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            MainScope().launch {
                                val res = storageRepository.trimSystemCaches()
                                res.fold(
                                    onSuccess = { actionNotification = "Cache sistem berhasil dipangkas via ADB." },
                                    onFailure = { actionNotification = "Gagal memangkas cache: ${it.localizedMessage}" }
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trim Cache", color = TextPrimary)
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

        // Breakdown Categories
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StorageStatCard(
                title = "Cache Aplikasi",
                sizeText = FileSizeFormatter.format(storageState.appCacheBytes),
                hint = "Data sementara sistem",
                color = CyanPrimary,
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
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        if (storageState.isAnalyzing) {
            repeat(2) { SkeletonListItem() }
        } else if (storageState.orphanedDirectories.isEmpty()) {
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
                        imageVector = Icons.Default.FolderOff,
                        contentDescription = null,
                        tint = StatusSafe,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Tidak ditemukan folder sisa OBB atau data aplikasi yang terhapus di /sdcard/Android.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            storageState.orphanedDirectories.forEach { folder ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.folderName,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = folder.path,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ukuran: ${FileSizeFormatter.format(folder.sizeBytes)} • ${folder.reason}",
                                color = StatusWarning,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { folderToDelete = folder },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusCritical),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus", color = Color.White, fontSize = 12.sp)
                        }
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
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, color = TextSecondary, fontSize = 11.sp)
            Text(text = sizeText, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = hint, color = TextTertiary(color), fontSize = 10.sp)
        }
    }
}

private fun TextTertiary(color: Color): Color = color.copy(alpha = 0.7f)
