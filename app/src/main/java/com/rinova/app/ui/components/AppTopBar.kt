package com.rinova.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinova.app.core.model.AdbConnectionState
import com.rinova.app.core.update.UpdateState
import com.rinova.app.ui.theme.OutlineVariant
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusSafe
import com.rinova.app.ui.theme.StatusWarning

import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal

/**
 * BESPOKE FLOATING TOP STATUS DECK (Anti-Kuno, Anti-Datar)
 * - Terapkan statusBarsPadding() anti punch-hole & jam sistem
 * - Floating Island dengan gradien matte arang dan highlight tepi atas
 * - Quick Action Pill in-app update langsung dari header
 * - Tombol Konsol Log Telemetri Real-Time
 * - Live connection telemetry radar
 * - Mode Pro / Smart Toggle & Settings BYOK Shortcut
 */
@Composable
fun AppTopBar(
    connectionState: AdbConnectionState,
    updateState: UpdateState = UpdateState.Idle,
    isProMode: Boolean = false,
    onToggleProMode: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onUpdateClick: (() -> Unit)? = null,
    onCheckUpdateManual: (() -> Unit)? = null,
    onLogClick: (() -> Unit)? = null,
    onStatusClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState is AdbConnectionState.Connected
    val isUpdateAvailable = updateState is UpdateState.UpdateAvailable || updateState is UpdateState.ReadyToInstall

    // Animasi pendaran radar dot untuk koneksi & update
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Floating Island Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1B1E26), Color(0xFF13151B))
                    )
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.35f),
                            Color(0xFF384050),
                            OutlineVariant
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sisi Kiri: Emblem Presisi & Identitas Rinova
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF3D1B00), Color(0xFF1C0D00))
                                )
                            )
                            .border(0.8.dp, Primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "RINOVA",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFF3F4F6),
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Primary.copy(alpha = 0.15f))
                                    .border(0.5.dp, Primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Primary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val subText = when (connectionState) {
                            is AdbConnectionState.Connected -> "${connectionState.device.brand} ${connectionState.device.model}"
                            is AdbConnectionState.Connecting, is AdbConnectionState.Authorizing -> "Menghubungkan target..."
                            else -> "Technician Toolkit"
                        }
                        Text(
                            text = subText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                // Sisi Kanan: Action Badges (Live Radar + In-App Update Trigger)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Update Pill (Muncul secara dinamis saat rilis baru terdeteksi)
                    AnimatedVisibility(
                        visible = isUpdateAvailable,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val updateLabel = when (updateState) {
                            is UpdateState.ReadyToInstall -> "Pasang ${updateState.version}"
                            is UpdateState.UpdateAvailable -> "Update ${updateState.newVersion}"
                            else -> "Pembaruan"
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFFF7A00), Color(0xFFFF5200))
                                    )
                                )
                                .border(0.8.dp, Color(0xFFFFD8B3).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .clickable { onUpdateClick?.invoke() }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = updateLabel,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Connection Live Telemetry Capsule
                    val (statusColor, statusText, statusIcon) = when (connectionState) {
                        is AdbConnectionState.Connected -> Triple(
                            StatusSafe,
                            "ONLINE",
                            Icons.Default.Wifi
                        )
                        is AdbConnectionState.Connecting, is AdbConnectionState.Authorizing -> Triple(
                            StatusWarning,
                            "PAIRING",
                            Icons.Default.Refresh
                        )
                        is AdbConnectionState.Error -> Triple(
                            StatusCritical,
                            "ERROR",
                            Icons.Default.WifiOff
                        )
                        is AdbConnectionState.Disconnected -> Triple(
                            Color(0xFF9CA3AF),
                            "STANDBY",
                            Icons.Default.WifiOff
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF181B22))
                            .border(0.8.dp, OutlineVariant, RoundedCornerShape(14.dp))
                            .then(
                                if (onStatusClick != null) Modifier.clickable(onClick = onStatusClick) else Modifier
                            )
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .scale(if (isConnected) pulseScale else 1f)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Mode Pro / Smart Capsule Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isProMode) PrimaryContainer else Color(0xFF1E293B))
                            .border(0.8.dp, if (isProMode) Primary else OutlineVariant, RoundedCornerShape(14.dp))
                            .then(if (onToggleProMode != null) Modifier.clickable(onClick = onToggleProMode) else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isProMode) "PRO" else "SMART",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isProMode) Primary else Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    // Tombol Pengaturan BYOK
                    if (onSettingsClick != null) {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Tombol Menu Cek Update / Info
                    if (!isUpdateAvailable) {
                        IconButton(
                            onClick = { onCheckUpdateManual?.invoke() },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Cek Pembaruan",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Tombol Konsol Log Telemetri
                    IconButton(
                        onClick = { onLogClick?.invoke() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Buka Konsol Log",
                            tint = Primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
