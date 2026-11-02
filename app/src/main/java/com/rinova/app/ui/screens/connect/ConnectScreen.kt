package com.rinova.app.ui.screens.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinova.app.core.model.AdbConnectionState
import com.rinova.app.data.repository.AdbRepository
import com.rinova.app.data.repository.SettingsRepository
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
import kotlinx.coroutines.flow.first

@Composable
fun ConnectScreen(
    adbRepository: AdbRepository,
    settingsRepository: SettingsRepository,
    connectionState: AdbConnectionState,
    onConnectSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ipInput by remember { mutableStateOf("192.168.1.") }
    var portInput by remember { mutableStateOf("5555") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        val savedIp = settingsRepository.lastTargetIp.first()
        val savedPort = settingsRepository.lastTargetPort.first()
        if (savedIp.isNotEmpty()) ipInput = savedIp
        if (savedPort.isNotEmpty()) portInput = savedPort
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val (color, title, subtitle) = when (connectionState) {
                    is AdbConnectionState.Connected -> Triple(
                        StatusSafe,
                        "Terhubung ke ${connectionState.device.model}",
                        "IP: ${connectionState.host}:${connectionState.port} • Android ${connectionState.device.androidVersion}"
                    )
                    is AdbConnectionState.Connecting, is AdbConnectionState.Authorizing -> Triple(
                        StatusWarning,
                        "Menghubungkan ke $ipInput:$portInput...",
                        "Menunggu respon otorisasi RSA dari target"
                    )
                    is AdbConnectionState.Error -> Triple(
                        StatusCritical,
                        "Gagal Terhubung",
                        connectionState.message
                    )
                    is AdbConnectionState.Disconnected -> Triple(
                        TextSecondary,
                        "Belum Terhubung",
                        "Masukkan IP & Port Debugging Nirkabel HP target"
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (connectionState is AdbConnectionState.Connected) Icons.Default.CheckCircle else Icons.Default.Wifi,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
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

        // Form Input Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Konfigurasi Sambungan Nirkabel",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    label = { Text("Alamat IP HP Target") },
                    placeholder = { Text("Contoh: 192.168.1.15") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = SlateSurfaceHighlight,
                        focusedLabelColor = CyanPrimary,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyanPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it },
                    label = { Text("Port ADB Wireless") },
                    placeholder = { Text("5555 atau port acak (misal 38471)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = SlateSurfaceHighlight,
                        focusedLabelColor = CyanPrimary,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyanPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = StatusCritical,
                        fontSize = 12.sp
                    )
                }

                if (connectionState is AdbConnectionState.Connected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                kotlinx.coroutines.MainScope().run {
                                    adbRepository.disconnect()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Putuskan Sambungan", color = StatusCritical)
                        }
                        Button(
                            onClick = onConnectSuccess,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Buka Dashboard", color = CyanOnPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val cleanIp = ipInput.trim()
                            val cleanPort = portInput.trim().toIntOrNull()
                            if (cleanIp.isEmpty() || cleanPort == null) {
                                errorMessage = "IP atau Port tidak valid"
                                return@Button
                            }
                            errorMessage = null
                            isSubmitting = true
                            kotlinx.coroutines.MainScope().run {
                                settingsRepository.saveLastConnection(cleanIp, portInput.trim())
                                val res = adbRepository.connect(cleanIp, cleanPort)
                                isSubmitting = false
                                res.fold(
                                    onSuccess = { onConnectSuccess() },
                                    onFailure = { errorMessage = it.localizedMessage }
                                )
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isSubmitting) "Menyambungkan..." else "Hubungkan ADB Wireless",
                            color = CyanOnPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Petunjuk Lapangan Teknisi Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Panduan Teknisi (HP Pasien / Target):",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val steps = listOf(
                    "Hubungkan HP teknisi dan HP target ke satu jaringan Wi-Fi atau Hotspot yang sama.",
                    "Di HP target: Masuk ke Pengaturan -> Tentang Ponsel -> Tekan 'Nomor Versi / Build Number' 7x.",
                    "Masuk ke Opsi Pengembang (Developer Options) -> Aktifkan 'Debugging USB' dan 'Debugging Nirkabel' (Wireless Debugging).",
                    "Buka menu Debugging Nirkabel, perhatikan Alamat IP & Port yang tertera (misal: 192.168.1.50:38491).",
                    "Ketikkan ke kolom di atas lalu tekan 'Hubungkan ADB Wireless'. Centang 'Selalu izinkan dari komputer ini' pada popup target."
                )

                steps.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${idx + 1}.",
                            color = CyanPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = step,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
