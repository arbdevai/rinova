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
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.rinova.app.ui.components.RinovaSurfaceCard
import com.rinova.app.ui.components.RinovaTactileButton
import com.rinova.app.ui.components.RinovaTextField
import com.rinova.app.ui.theme.Outline
import com.rinova.app.ui.theme.OutlineVariant
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusSafe
import com.rinova.app.ui.theme.StatusWarning
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ConnectScreen(
    adbRepository: AdbRepository,
    settingsRepository: SettingsRepository,
    connectionState: AdbConnectionState,
    onConnectSuccess: () -> Unit,
    onOpenLog: (() -> Unit)? = null,
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
        RinovaSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            shapeRadius = 18.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
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
                        Color(0xFF9CA3AF),
                        "Belum Terhubung",
                        "Pilih metode sambungan di bawah untuk mulai"
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f))
                        .border(1.dp, color.copy(alpha = 0.4f), CircleShape),
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
            }
        }

        // Segmented Mode Switcher (Sleek Industrial Tab Strip)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF14171E))
                .border(0.8.dp, OutlineVariant, RoundedCornerShape(16.dp))
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PrimaryContainer else Color.Transparent)
                        .clickable {
                            selectedTab = index
                            errorMessage = null
                            statusFeedbackMessage = null
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Primary else Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color(0xFFFFFFFF) else Color(0xFF9CA3AF),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Form Card per Mode
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
                when (selectedTab) {
                    0 -> {
                        // TAB 0: KONEKSI LANGSUNG
                        Text(
                            text = "Koneksi Langsung (Sudah Pernah Di-Pair)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gunakan mode ini jika ponsel sudah pernah dipasangkan sebelumnya atau port ADB default (5555).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        RinovaTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = "Alamat IP HP Target",
                            placeholder = "Contoh: 192.168.1.14",
                            leadingIcon = Icons.Default.Wifi,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        RinovaTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = "Port ADB Wireless Utama",
                            placeholder = "Contoh: 39485 atau 5555",
                            leadingIcon = Icons.Default.Router,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    1 -> {
                        // TAB 1: PAIRING KODE 6-DIGIT (ANDROID 11-14+)
                        Text(
                            text = "Pairing Kode 6-Digit (Android 11 - 14+)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Di HP target: Masuk ke Pengaturan > Opsi Pengembang > Debugging Nirkabel > 'Sandingkan perangkat dengan kode pemasangan'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        RinovaTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = "Alamat IP HP Target",
                            placeholder = "Contoh: 192.168.1.14",
                            leadingIcon = Icons.Default.Wifi,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RinovaTextField(
                                value = pairingPortInput,
                                onValueChange = { pairingPortInput = it },
                                label = "Port Pairing",
                                placeholder = "Misal: 37393",
                                leadingIcon = Icons.Default.Tag,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )

                            RinovaTextField(
                                value = pairingCodeInput,
                                onValueChange = { if (it.length <= 6) pairingCodeInput = it },
                                label = "Kode 6 Digit",
                                placeholder = "Misal: 102118",
                                leadingIcon = Icons.Default.Pin,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        RinovaTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = "Port Koneksi ADB Utama",
                            placeholder = "Port di luar dialog pairing (misal: 39485)",
                            leadingIcon = Icons.Default.Router,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    2 -> {
                        // TAB 2: PAIRING QR CODE (ANDROID 11-14+)
                        Text(
                            text = "Pairing QR Code Nirkabel (AOSP Standard)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Di HP target: Buka menu Debugging Nirkabel > 'Sandingkan perangkat dengan kode QR' lalu arahkan kamera target ke QR Code ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        // QR Code High-Contrast Frame with precision viewport
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0F1116))
                                .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (qrBitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(210.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White)
                                            .border(2.dp, Primary, RoundedCornerShape(14.dp))
                                            .padding(12.dp),
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

                                    RinovaTactileButton(
                                        text = "Acak QR",
                                        onClick = { regenerateQrCode() },
                                        icon = Icons.Default.Refresh,
                                        isSecondary = true,
                                        height = 36.dp
                                    )
                                }
                            }
                        }

                        RinovaTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = "Alamat IP HP Target",
                            placeholder = "Contoh: 192.168.1.14",
                            leadingIcon = Icons.Default.Wifi,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        RinovaTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = "Port Koneksi ADB Target",
                            placeholder = "Misal: 39485",
                            leadingIcon = Icons.Default.Router,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = errorMessage ?: "",
                            color = StatusCritical,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (onOpenLog != null) {
                            Text(
                                text = "▶ Buka Konsol Log Telemetri untuk rincian error & stack trace",
                                color = Primary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onOpenLog() }
                            )
                        }
                    }
                }

                if (connectionState is AdbConnectionState.Connected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RinovaTactileButton(
                            text = "Putuskan",
                            onClick = {
                                coroutineScope.launch {
                                    adbRepository.disconnect()
                                }
                            },
                            isSecondary = true,
                            modifier = Modifier.weight(1f)
                        )
                        RinovaTactileButton(
                            text = "Buka Dashboard",
                            onClick = onConnectSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    val buttonText = when {
                        isSubmitting -> "Sedang Memproses..."
                        selectedTab == 1 -> "Pair & Hubungkan Otomatis"
                        selectedTab == 2 -> "Hubungkan Setelah Scan QR"
                        else -> "Hubungkan ADB Wireless"
                    }

                    RinovaTactileButton(
                        text = buttonText,
                        onClick = {
                            focusManager.clearFocus()
                            val cleanIp = ipInput.trim()
                            val cleanConnectPort = portInput.trim().toIntOrNull()

                            if (cleanIp.isEmpty() || cleanConnectPort == null) {
                                errorMessage = "Alamat IP atau Port Koneksi tidak valid"
                                return@RinovaTactileButton
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Panduan Lapangan Teknisi Card
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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
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
