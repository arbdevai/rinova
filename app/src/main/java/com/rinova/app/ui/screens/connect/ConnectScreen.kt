package com.rinova.app.ui.screens.connect

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinova.app.core.model.AdbConnectionState
import com.rinova.app.core.util.QrCodeGenerator
import com.rinova.app.data.repository.AdbRepository
import com.rinova.app.data.repository.SettingsRepository
import com.rinova.app.ui.theme.Outline
import com.rinova.app.ui.theme.OutlineVariant
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusSafe
import com.rinova.app.ui.theme.StatusWarning
import com.rinova.app.ui.theme.SurfaceContainerHigh
import com.rinova.app.ui.theme.SurfaceContainerLow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ConnectScreen(
    adbRepository: AdbRepository,
    settingsRepository: SettingsRepository,
    connectionState: AdbConnectionState,
    onConnectSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tab Mode: 0 = Langsung, 1 = Kode 6-Digit, 2 = QR Code
    var selectedTab by remember { mutableIntStateOf(0) }

    var ipInput by remember { mutableStateOf("192.168.1.") }
    var portInput by remember { mutableStateOf("5555") }

    // Pairing fields (Android 11-14+)
    var pairingPortInput by remember { mutableStateOf("") }
    var pairingCodeInput by remember { mutableStateOf("") }

    // QR Pairing state
    var qrServiceName by remember { mutableStateOf("rinova-${(1000..9999).random()}") }
    var qrPassword by remember { mutableStateOf("${(100000..999999).random()}") }
    var qrBitmap by remember {
        mutableStateOf<Bitmap?>(
            QrCodeGenerator.createQrCodeBitmap(
                QrCodeGenerator.generateAdbPairingPayload(qrServiceName, qrPassword),
                512,
                Color.Black,
                Color.White
            )
        )
    }

    var isSubmitting by remember { mutableStateOf(false) }
    var statusFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val savedIp = settingsRepository.lastTargetIp.first()
        val savedPort = settingsRepository.lastTargetPort.first()
        if (savedIp.isNotEmpty()) ipInput = savedIp
        if (savedPort.isNotEmpty()) portInput = savedPort
    }

    fun regenerateQrCode() {
        val newService = "rinova-${(1000..9999).random()}"
        val newPass = "${(100000..999999).random()}"
        qrServiceName = newService
        qrPassword = newPass
        val payload = QrCodeGenerator.generateAdbPairingPayload(newService, newPass)
        qrBitmap = QrCodeGenerator.createQrCodeBitmap(payload, 512, Color.Black, Color.White)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        Card(
            shape = RoundedCornerShape(16.dp),
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
                val (color, title, subtitle) = when (connectionState) {
                    is AdbConnectionState.Connected -> Triple(
                        StatusSafe,
                        "Terhubung: ${connectionState.device.model}",
                        "IP: ${connectionState.host}:${connectionState.port} • Android ${connectionState.device.androidVersion}"
                    )
                    is AdbConnectionState.Connecting, is AdbConnectionState.Authorizing -> Triple(
                        StatusWarning,
                        "Menghubungkan ke $ipInput:$portInput...",
                        "Menunggu otorisasi RSA dari target"
                    )
                    is AdbConnectionState.Error -> Triple(
                        StatusCritical,
                        "Gagal Terhubung",
                        connectionState.message
                    )
                    is AdbConnectionState.Disconnected -> Triple(
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        "Belum Terhubung",
                        "Pilih metode sambungan di bawah untuk mulai"
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (connectionState is AdbConnectionState.Connected) Icons.Default.CheckCircle else Icons.Default.Devices,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
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
            }
        }

        // Segmented Mode Switcher (Google Material 3 Container)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceContainerLow)
                .border(1.dp, OutlineVariant, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                Triple(0, "Langsung", Icons.Default.Devices),
                Triple(1, "Kode 6-Digit", Icons.Default.VpnKey),
                Triple(2, "QR Code", Icons.Default.QrCodeScanner)
            )

            tabs.forEach { (index, title, icon) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) PrimaryContainer else Color.Transparent)
                        .clickable {
                            selectedTab = index
                            errorMessage = null
                            statusFeedbackMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Form Card per Mode
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 0: KONEKSI LANGSUNG
                        Text(
                            text = "Koneksi Langsung (Sudah Pernah Di-Pair)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Gunakan mode ini jika ponsel sudah pernah dipasangkan sebelumnya atau port ADB default (5555).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = { Text("Alamat IP HP Target") },
                            placeholder = { Text("Contoh: 192.168.1.14") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = m3OutlinedColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text("Port ADB Wireless Utama") },
                            placeholder = { Text("Contoh: 39485 atau 5555") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = m3OutlinedColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    1 -> {
                        // TAB 1: PAIRING KODE 6-DIGIT (ANDROID 11-14+)
                        Text(
                            text = "Pairing Kode 6-Digit (Android 11 - 14+)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Di HP target: Masuk ke Pengaturan > Opsi Pengembang > Debugging Nirkabel > 'Sandingkan perangkat dengan kode pemasangan'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = { Text("Alamat IP HP Target") },
                            placeholder = { Text("Contoh: 192.168.1.14") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = m3OutlinedColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = pairingPortInput,
                                onValueChange = { pairingPortInput = it },
                                label = { Text("Port Pairing") },
                                placeholder = { Text("Misal: 37393") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = m3OutlinedColors(),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = pairingCodeInput,
                                onValueChange = { if (it.length <= 6) pairingCodeInput = it },
                                label = { Text("Kode 6 Digit") },
                                placeholder = { Text("Misal: 102118") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = m3OutlinedColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text("Port Koneksi ADB Utama (Di luar dialog pairing)") },
                            placeholder = { Text("Misal: 39485") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = m3OutlinedColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    2 -> {
                        // TAB 2: PAIRING QR CODE (ANDROID 11-14+)
                        Text(
                            text = "Pairing QR Code Nirkabel (AOSP Standard)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Di HP target: Buka menu Debugging Nirkabel > 'Sandingkan perangkat dengan kode QR' lalu arahkan kamera target ke QR Code ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        // QR Code High-Contrast Frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceContainerHigh)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (qrBitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(208.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = qrBitmap!!.asImageBitmap(),
                                            contentDescription = "QR Code ADB Wireless",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Service: $qrServiceName",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Primary,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "PIN Pairing: $qrPassword",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { regenerateQrCode() },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Acak QR", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = { Text("Alamat IP HP Target") },
                            placeholder = { Text("Contoh: 192.168.1.14") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = m3OutlinedColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text("Port Koneksi ADB Target") },
                            placeholder = { Text("Misal: 39485") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = m3OutlinedColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (statusFeedbackMessage != null) {
                    Text(
                        text = statusFeedbackMessage ?: "",
                        color = StatusSafe,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = StatusCritical,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (connectionState is AdbConnectionState.Connected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    adbRepository.disconnect()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Putuskan", color = StatusCritical, style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onConnectSuccess,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Buka Dashboard", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val cleanIp = ipInput.trim()
                            val cleanConnectPort = portInput.trim().toIntOrNull()

                            if (cleanIp.isEmpty() || cleanConnectPort == null) {
                                errorMessage = "Alamat IP atau Port Koneksi tidak valid"
                                return@Button
                            }

                            errorMessage = null
                            statusFeedbackMessage = null
                            isSubmitting = true

                            coroutineScope.launch {
                                settingsRepository.saveLastConnection(cleanIp, portInput.trim())

                                if (selectedTab == 1) {
                                    // Eksekusi Pairing Kode 6 Digit
                                    val pairPort = pairingPortInput.trim().toIntOrNull()
                                    val pairCode = pairingCodeInput.trim()

                                    if (pairPort == null || pairCode.length != 6) {
                                        isSubmitting = false
                                        errorMessage = "Port pairing dan kode 6 digit harus diisi lengkap sesuai dialog di HP target."
                                        return@launch
                                    }

                                    statusFeedbackMessage = "Melakukan pairing dengan target $cleanIp:$pairPort..."
                                    val pairResult = adbRepository.pair(cleanIp, pairPort, pairCode)

                                    if (pairResult.isFailure) {
                                        isSubmitting = false
                                        errorMessage = "Pairing gagal: ${pairResult.exceptionOrNull()?.localizedMessage}"
                                        return@launch
                                    }

                                    statusFeedbackMessage = "Pairing berhasil! Menyambungkan ke port utama $cleanConnectPort..."
                                }

                                // Hubungkan ke Port Koneksi Utama
                                val res = adbRepository.connect(cleanIp, cleanConnectPort)
                                isSubmitting = false

                                res.fold(
                                    onSuccess = {
                                        statusFeedbackMessage = "Berhasil terhubung ke ${it.model}!"
                                        onConnectSuccess()
                                    },
                                    onFailure = {
                                        errorMessage = it.localizedMessage
                                    }
                                )
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        val buttonText = when {
                            isSubmitting -> "Sedang Memproses..."
                            selectedTab == 1 -> "Pair & Hubungkan Otomatis"
                            selectedTab == 2 -> "Hubungkan Setelah Scan QR"
                            else -> "Hubungkan ADB Wireless"
                        }
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Panduan Lapangan Teknisi Card (Google M3 Accordion Style)
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Panduan Lapangan Teknisi (Android 11 - 14+):",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val steps = listOf(
                    "Pastikan HP teknisi dan HP target berada di satu Wi-Fi atau Hotspot lokal yang sama (tanpa Client Isolation).",
                    "Di HP target: Masuk ke Pengaturan > Tentang Ponsel > Tekan 'Nomor Versi / Build Number' 7x hingga Opsi Pengembang aktif.",
                    "Masuk ke Opsi Pengembang > Aktifkan 'Debugging USB' dan 'Debugging Nirkabel'.",
                    "Untuk HP Android 11-14+ yang baru pertama kali disambungkan, pilih tab 'Kode 6-Digit' atau 'QR Code'.",
                    "Perhatikan: Port Pairing di dalam popup berbeda dengan Port Koneksi utama di halaman Debugging Nirkabel."
                )

                steps.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${idx + 1}.",
                            color = Primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = step,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun m3OutlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary
)
