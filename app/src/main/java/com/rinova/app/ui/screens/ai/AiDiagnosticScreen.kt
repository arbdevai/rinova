package com.rinova.app.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinova.app.core.model.AiActionRisk
import com.rinova.app.core.model.AiOptimization
import com.rinova.app.core.model.AiRecommendation
import com.rinova.app.core.model.GeminiTestResult
import com.rinova.app.data.repository.AiDiagnosticRepository
import com.rinova.app.data.repository.SettingsRepository
import com.rinova.app.ui.components.AlertType
import com.rinova.app.ui.components.CustomActionDialog
import com.rinova.app.ui.components.ScanningProgressSheet
import com.rinova.app.ui.components.SkeletonCard
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
import kotlinx.coroutines.launch

@Composable
fun AiDiagnosticScreen(
    aiRepository: AiDiagnosticRepository,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val report by aiRepository.diagnosticReport.collectAsStateWithLifecycle()
    val isAnalyzing by aiRepository.isAnalyzing.collectAsStateWithLifecycle()
    val currentStep by aiRepository.analysisStep.collectAsStateWithLifecycle()
    val availableModels by aiRepository.availableModels.collectAsStateWithLifecycle()
    val testResult by aiRepository.testResult.collectAsStateWithLifecycle()

    var apiKeyInput by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("gemini-2.0-flash") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var isTestingModel by remember { mutableStateOf(false) }
    var isFetchingModels by remember { mutableStateOf(false) }
    var actionNotification by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var pendingRecommendation by remember { mutableStateOf<AiRecommendation?>(null) }
    var pendingOptimization by remember { mutableStateOf<AiOptimization?>(null) }

    LaunchedEffect(Unit) {
        val savedKey = settingsRepository.geminiApiKey.first()
        val savedModel = settingsRepository.geminiModel.first()
        if (savedKey.isNotEmpty()) apiKeyInput = savedKey
        if (savedModel.isNotEmpty()) selectedModel = savedModel
    }

    if (pendingRecommendation != null) {
        val rec = pendingRecommendation!!
        val isDanger = rec.riskLevel == AiActionRisk.CRITICAL_KEEP
        CustomActionDialog(
            title = rec.proposedActionTitle,
            message = "${rec.empiricalEvidence}\n\nTarget: ${rec.targetName}",
            commandPreview = rec.adbCommand,
            alertType = if (isDanger) AlertType.CRITICAL else AlertType.WARNING,
            confirmButtonText = if (isDanger) "Batalkan (Berisiko)" else "Eksekusi Rekomendasi",
            onConfirm = {
                val r = rec
                pendingRecommendation = null
                if (!isDanger) {
                    coroutineScope.launch {
                        val res = aiRepository.executeRecommendation(r)
                        res.fold(
                            onSuccess = { actionNotification = "Perintah berhasil dijalankan di HP target: $it" },
                            onFailure = { actionNotification = "Gagal mengeksekusi: ${it.localizedMessage}" }
                        )
                    }
                }
            },
            onDismiss = { pendingRecommendation = null }
        )
    }

    if (pendingOptimization != null) {
        val opt = pendingOptimization!!
        CustomActionDialog(
            title = opt.title,
            message = "${opt.rationale}\n\nEstimasi Dampak: ${opt.estimatedImpact}",
            commandPreview = opt.adbCommand,
            alertType = AlertType.INFO,
            confirmButtonText = "Terapkan Optimasi",
            onConfirm = {
                val cmd = opt.adbCommand
                pendingOptimization = null
                coroutineScope.launch {
                    val res = aiRepository.executeOptimization(cmd)
                    res.fold(
                        onSuccess = { actionNotification = "Optimasi diterapkan: $it" },
                        onFailure = { actionNotification = "Gagal menerapkan: ${it.localizedMessage}" }
                    )
                }
            },
            onDismiss = { pendingOptimization = null }
        )
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
        if (isAnalyzing) {
            ScanningProgressSheet(
                title = "Audit Gemini AI Sedang Berjalan",
                currentStepText = currentStep,
                progress = -1f,
                onCancel = {}
            )
        }

        // Gemini API Key & Dynamic Model Config
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            modifier = Modifier.fillMaxWidth()
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Konfigurasi Gemini API & Model",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Dinamis & Bebas Hardcode",
                        color = StatusSafe,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Kunci API Gemini (AI Studio)") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    },
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

                // Model Name Input
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = { selectedModel = it },
                    label = { Text("Nama Model Gemini Aktif") },
                    placeholder = { Text("gemini-2.0-flash atau gemini-1.5-flash") },
                    singleLine = true,
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

                // Quick presets or loaded models
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fallbackPresets = listOf(
                        "gemini-2.5-flash",
                        "gemini-2.0-flash",
                        "gemini-1.5-flash",
                        "gemini-1.5-pro"
                    )
                    val modelList = if (availableModels.isNotEmpty()) availableModels.map { it.id } else fallbackPresets

                    modelList.take(6).forEach { modelId ->
                        FilterChip(
                            selected = selectedModel == modelId,
                            onClick = { selectedModel = modelId },
                            label = { Text(modelId, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = CyanOnPrimary,
                                containerColor = SlateSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                // Action buttons: Muat Model, Uji Limit, Simpan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val key = apiKeyInput.trim()
                            if (key.isEmpty()) {
                                actionNotification = "Masukkan API Key terlebih dahulu."
                                return@OutlinedButton
                            }
                            isFetchingModels = true
                            coroutineScope.launch {
                                settingsRepository.saveGeminiApiKey(key)
                                val res = aiRepository.fetchAvailableModels(key)
                                isFetchingModels = false
                                res.fold(
                                    onSuccess = { models ->
                                        actionNotification = "Berhasil memuat ${models.size} model aktif dari API."
                                        if (models.isNotEmpty() && !models.any { it.id == selectedModel }) {
                                            selectedModel = models.first().id
                                        }
                                    },
                                    onFailure = { actionNotification = it.localizedMessage }
                                )
                            }
                        },
                        enabled = !isFetchingModels,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFetchingModels) "Memuat..." else "Muat Model", color = TextPrimary, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val key = apiKeyInput.trim()
                            if (key.isEmpty()) {
                                actionNotification = "Masukkan API Key terlebih dahulu."
                                return@OutlinedButton
                            }
                            isTestingModel = true
                            coroutineScope.launch {
                                settingsRepository.saveGeminiApiKey(key)
                                settingsRepository.saveGeminiModel(selectedModel.trim())
                                val res = aiRepository.testModelAndKey(key, selectedModel.trim())
                                isTestingModel = false
                                res.fold(
                                    onSuccess = {
                                        actionNotification = if (it.isHealthy) "Model ${it.modelName} siap digunakan!" else "Model mengalami kendala: ${it.statusMessage}"
                                    },
                                    onFailure = { actionNotification = it.localizedMessage }
                                )
                            }
                        },
                        enabled = !isTestingModel,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = StatusWarning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isTestingModel) "Menguji..." else "Tes Limit & Key", color = StatusWarning, fontSize = 11.sp)
                    }
                }

                // Audit AI Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val key = apiKeyInput.trim()
                        val model = selectedModel.trim()
                        if (key.isEmpty()) {
                            actionNotification = "Masukkan API Key terlebih dahulu."
                            return@Button
                        }
                        coroutineScope.launch {
                            settingsRepository.saveGeminiApiKey(key)
                            settingsRepository.saveGeminiModel(model)
                            val res = aiRepository.runAiAudit(key, model)
                            res.fold(
                                onSuccess = { actionNotification = "Audit selesai menggunakan $model!" },
                                onFailure = { actionNotification = "Gagal audit: ${it.localizedMessage}" }
                            )
                        }
                    },
                    enabled = !isAnalyzing,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyanOnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAnalyzing) "Menganalisis Sistem..." else "Audit AI Berbasis Data Empiris",
                        color = CyanOnPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Model & Quota Test Result Card
        if (testResult != null) {
            val tr = testResult!!
            val bannerColor = if (tr.isHealthy) StatusSafe else StatusCritical
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (tr.isHealthy) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Hasil Uji Model: ${tr.modelName}",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(bannerColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${tr.latencyMs} ms",
                                color = bannerColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = tr.statusMessage,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Status Quota: ${tr.quotaStatus}",
                            color = if (tr.quotaStatus.contains("Habis") || tr.quotaStatus.contains("Error")) StatusCritical else StatusSafe,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (tr.inputTokenLimit > 0L) {
                            Text(
                                text = "Token Limit: ${tr.inputTokenLimit / 1000}k in / ${tr.outputTokenLimit / 1000}k out",
                                color = CyanPrimary,
                                fontSize = 11.sp
                            )
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

        // Report Section
        if (isAnalyzing) {
            SkeletonCard()
            repeat(2) { SkeletonCard() }
        } else if (report != null) {
            val r = report!!

            // Health Score & Summary
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Skor Kesehatan Sistem",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (r.modelUsed.isNotEmpty()) {
                                Text(
                                    text = "Dianalisis oleh: ${r.modelUsed}",
                                    color = CyanPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (r.overallHealthScore >= 80) StatusSafe.copy(alpha = 0.15f) else StatusWarning.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${r.overallHealthScore} / 100",
                                color = if (r.overallHealthScore >= 80) StatusSafe else StatusWarning,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = r.deviceSummary,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Empirical Observations
            if (r.empiricalObservations.isNotEmpty()) {
                Text(
                    text = "Temuan Fakta Empiris di Sistem",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        r.empiricalObservations.forEach { obs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("•", color = CyanPrimary, fontWeight = FontWeight.Bold)
                                Text(obs, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }

            // Recommendations (Structured Action Cards)
            if (r.recommendations.isNotEmpty()) {
                Text(
                    text = "Rekomendasi Tindakan Terkontrol (${r.recommendations.size})",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                r.recommendations.forEach { rec ->
                    val badgeColor = when (rec.riskLevel) {
                        AiActionRisk.SAFE_ACTION -> StatusSafe
                        AiActionRisk.OPTIONAL_TWEAK -> StatusWarning
                        AiActionRisk.CRITICAL_KEEP -> StatusCritical
                    }
                    val badgeText = when (rec.riskLevel) {
                        AiActionRisk.SAFE_ACTION -> "AMAN DIEKSEKUSI"
                        AiActionRisk.OPTIONAL_TWEAK -> "OPSI LANJUTAN"
                        AiActionRisk.CRITICAL_KEEP -> "JANGAN DISENTUH (KRITIS)"
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = rec.proposedActionTitle,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        color = badgeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = rec.empiricalEvidence,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            if (rec.adbCommand.isNotEmpty()) {
                                Card(
                                    shape = RoundedCornerShape(6.dp),
                                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = rec.adbCommand,
                                        color = CyanPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { pendingRecommendation = rec },
                                    colors = ButtonDefaults.buttonColors(containerColor = badgeColor),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (rec.riskLevel == AiActionRisk.CRITICAL_KEEP) "Tinjau Larangan" else "Tinjau & Eksekusi",
                                        color = if (rec.riskLevel == AiActionRisk.CRITICAL_KEEP) Color.White else Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // System Optimizations
            if (r.optimizations.isNotEmpty()) {
                Text(
                    text = "Saran Optimalisasi Sistem",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                r.optimizations.forEach { opt ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = opt.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${opt.rationale} • Dampak: ${opt.estimatedImpact}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { pendingOptimization = opt },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Terapkan", color = CyanPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Belum Ada Hasil Audit",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pastikan HP target terhubung dan API Key telah disimpan, lalu tekan 'Audit AI Berbasis Data Empiris' untuk menganalisis seluruh data telemetri, storage, dan aplikasi berisiko.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
