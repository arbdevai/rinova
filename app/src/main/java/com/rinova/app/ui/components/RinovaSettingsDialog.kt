package com.rinova.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rinova.app.core.ai.GeminiApiClient
import com.rinova.app.core.model.GeminiModelInfo
import com.rinova.app.data.repository.SettingsRepository
import com.rinova.app.ui.theme.OutlineVariant
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer
import com.rinova.app.ui.theme.StatusSafe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun RinovaSettingsDialog(
    settingsRepository: SettingsRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val geminiClient = remember { GeminiApiClient() }

    var apiKey by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("gemini-2.0-flash") }
    var isProMode by remember { mutableStateOf(false) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var isSavedToast by remember { mutableStateOf(false) }

    var fetchedModels by remember { mutableStateOf<List<GeminiModelInfo>>(emptyList()) }
    var isLoadingModels by remember { mutableStateOf(false) }
    var modelLoadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        apiKey = settingsRepository.geminiApiKey.first()
        selectedModel = settingsRepository.geminiModel.first()
        isProMode = settingsRepository.isProMode.first()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF14171D)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Pengaturan Rinova",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "BYOK & Konfigurasi Model AI",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Section 1: Gemini API Key (BYOK)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Kunci Gemini API (Bring Your Own Key)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Nol kunci master di dalam APK. Kunci Anda tersimpan aman di sandbox privat hp dan kebal dekompilasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: AIzaSy...", color = Color(0xFF6B7280), fontSize = 13.sp) },
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = OutlineVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Link get free API key
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Belum punya kunci? Buat gratis di Google AI Studio",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontSize = 11.sp
                        )
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Section 2: Pilihan Model AI Gemini (Bukan Hardcode!)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Model Gemini Aktif",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Tombol Muat Model dari API Key
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E222B))
                                .border(0.8.dp, OutlineVariant, RoundedCornerShape(6.dp))
                                .clickable(enabled = apiKey.isNotBlank() && !isLoadingModels) {
                                    coroutineScope.launch {
                                        isLoadingModels = true
                                        modelLoadError = null
                                        val res = geminiClient.fetchAvailableModels(apiKey)
                                        res.fold(
                                            onSuccess = { models ->
                                                fetchedModels = models
                                                isLoadingModels = false
                                            },
                                            onFailure = { err ->
                                                modelLoadError = err.localizedMessage
                                                isLoadingModels = false
                                            }
                                        )
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isLoadingModels) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = Primary,
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = if (isLoadingModels) "Memuat..." else "Sinkron dari API Key",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Text(
                        text = "Model terpilih saat ini: $selectedModel",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )

                    // Quick Selection Chips Rekomendasi
                    val quickModels = listOf(
                        Pair("gemini-2.0-flash", "Cepat & Cerdas (Rekomendasi)"),
                        Pair("gemini-2.5-flash", "Generasi Baru"),
                        Pair("gemini-1.5-flash", "Hemat Kuota"),
                        Pair("gemini-1.5-pro", "Penalaran Rumit")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickModels.forEach { (mId, mDesc) ->
                            val isSelected = selectedModel == mId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryContainer.copy(alpha = 0.5f) else Color(0xFF1B1E26))
                                    .border(
                                        1.dp,
                                        if (isSelected) Primary else OutlineVariant,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedModel = mId }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = mId,
                                        color = if (isSelected) Primary else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = mDesc,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 10.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Tampilkan daftar model hasil sinkronisasi API Key jika ada
                    if (fetchedModels.isNotEmpty()) {
                        Text(
                            text = "Model Lain yang Didukung Akun Anda (${fetchedModels.size} model):",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            fetchedModels.take(8).forEach { m ->
                                val isSelected = selectedModel == m.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryContainer.copy(alpha = 0.3f) else Color(0xFF101217))
                                        .border(
                                            0.8.dp,
                                            if (isSelected) Primary else Color(0xFF232733),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedModel = m.id }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = m.id,
                                        color = if (isSelected) Primary else Color(0xFFD1D5DB),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (modelLoadError != null) {
                        Text(
                            text = "Gagal memuat model: $modelLoadError",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp
                        )
                    }
                }

                // Section 3: Mode Pro / Teknisi Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B1E26))
                        .border(0.8.dp, OutlineVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mode Pro (Teknisi)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isProMode) "Menampilkan preview perintah shell dan log telemetri" else "Mode Pintar: Bahasa ramah awam tanpa jargon terminal",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isProMode,
                        onCheckedChange = { isProMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Primary,
                            checkedTrackColor = PrimaryContainer
                        )
                    )
                }

                // Save Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            settingsRepository.saveGeminiApiKey(apiKey)
                            settingsRepository.saveGeminiModel(selectedModel)
                            settingsRepository.setProMode(isProMode)
                            isSavedToast = true
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Simpan Pengaturan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
