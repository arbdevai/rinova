package com.rinova.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinova.app.core.update.UpdateState
import com.rinova.app.core.util.FileSizeFormatter
import com.rinova.app.ui.theme.OutlineVariant
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusSafe
import java.io.File

/**
 * IN-APP UPDATE MODAL SHEET (Standard Industri Skill Android Compose)
 * - Auto-popup saat rilis terdeteksi
 * - Tampilan changelog riil dari GitHub Release (bebas hardcode)
 * - OkHttp Streaming progress bar dengan persentase presisi
 * - Aksi pasang APK satu sentuhan dengan deep verification
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RinovaUpdateDialog(
    updateState: UpdateState,
    currentVersion: String,
    onDownload: (url: String, version: String, size: Long) -> Unit,
    onInstall: (apkFile: File) -> Unit,
    onCheckAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14171E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF384050))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Icon + Title + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Pembaruan Sistem Rinova",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF3F4F6),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Versi Saat Ini: v$currentVersion",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF9CA3AF)
                    )
                }
            }

            when (updateState) {
                is UpdateState.Checking -> {
                    RinovaSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 14.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Menghubungi server GitHub Release untuk memeriksa versi terbaru...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }

                is UpdateState.UpdateAvailable -> {
                    // Version & Size Meta Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Primary.copy(alpha = 0.15f))
                                .border(0.8.dp, Primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Tersedia: ${updateState.newVersion}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (updateState.fileSizeBytes > 0L) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E222B))
                                    .border(0.8.dp, OutlineVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Ukuran Paket: ${FileSizeFormatter.format(updateState.fileSizeBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF9CA3AF),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Release Notes / Changelog Box
                    Text(
                        text = "Catatan Rilis & Perubahan:",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFF3F4F6),
                        fontWeight = FontWeight.SemiBold
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0C0E12))
                            .border(0.8.dp, OutlineVariant, RoundedCornerShape(12.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        val notes = updateState.releaseNotes.ifBlank {
                            "Peningkatan performa, pembaruan keamanan, dan optimasi arsitektur Rinova."
                        }
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD1D5DB),
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    // Action Button
                    RinovaTactileButton(
                        text = "Unduh & Perbarui Sekarang",
                        icon = Icons.Default.Download,
                        onClick = {
                            onDownload(
                                updateState.downloadUrl,
                                updateState.newVersion,
                                updateState.fileSizeBytes
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 50.dp
                    )
                }

                is UpdateState.Downloading -> {
                    RinovaSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 14.dp
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
                                Text(
                                    text = "Mengunduh Paket Pembaruan...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFF3F4F6),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${updateState.progressPercent}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            LinearProgressIndicator(
                                progress = { updateState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Primary,
                                trackColor = Color(0xFF282D38)
                            )

                            Text(
                                text = "Menulis ke isolasi berkas sementara .part. Verifikasi 4 lapis akan dijalankan otomatis saat selesai.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }

                is UpdateState.ReadyToInstall -> {
                    RinovaSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 14.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(StatusSafe.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusSafe,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Paket ${updateState.version} Siap Dipasang",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFFF3F4F6),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Integritas berkas & signature DEX terverifikasi 100%.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusSafe
                                    )
                                }
                            }

                            RinovaTactileButton(
                                text = "Pasang Pembaruan Sekarang",
                                onClick = { onInstall(updateState.apkFile) },
                                modifier = Modifier.fillMaxWidth(),
                                height = 50.dp
                            )
                        }
                    }
                }

                is UpdateState.Error -> {
                    RinovaSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 14.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = StatusCritical,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Kendala Pembaruan",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StatusCritical,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = updateState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                            RinovaTactileButton(
                                text = "Coba Periksa Lagi",
                                onClick = onCheckAgain,
                                isSecondary = true,
                                height = 40.dp
                            )
                        }
                    }
                }

                is UpdateState.Idle -> {
                    RinovaSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 14.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusSafe,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Aplikasi Anda Sudah Versi Terbaru",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFFF3F4F6),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Versi v$currentVersion adalah rilis resmi paling mutakhir di repositori.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
