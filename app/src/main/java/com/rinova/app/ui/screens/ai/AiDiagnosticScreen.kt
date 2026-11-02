package com.rinova.app.ui.screens.ai

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.rinova.app.data.repository.AiDiagnosticRepository
import com.rinova.app.data.repository.SettingsRepository
import com.rinova.app.ui.components.AlertType
import com.rinova.app.ui.components.CustomActionDialog
import com.rinova.app.ui.components.RinovaModelPickerSheet
import com.rinova.app.ui.components.RinovaSurfaceCard
import com.rinova.app.ui.components.RinovaTactileButton
import com.rinova.app.ui.components.RinovaTextField
import com.rinova.app.ui.components.ScanningProgressSheet
import com.rinova.app.ui.components.SkeletonCard
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer
import com.rinova.app.ui.theme.StatusCritical
import com.rinova.app.ui.theme.StatusSafe
import com.rinova.app.ui.theme.StatusWarning
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    var showModelPickerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    if (showModelPickerSheet) {
        RinovaModelPickerSheet(
            sheetState = sheetState,
            models = availableModels,
            selectedModelId = selectedModel,
            onModelSelected = { model ->
                selectedModel = model.id
                coroutineScope.launch {
                    settingsRepository.saveGeminiModel(model.id)
                }
            },
            onDismiss = { showModelPickerSheet = false }
        )
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
            .background(MaterialTheme.colorScheme.background)
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

        // Gemini API Key & Dynamic Model Config (Bespoke Material 3)
        RinovaSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            shapeRadius = 18.dp
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Konfigurasi Gemini API",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusSafe.copy(alpha = 0.15f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Dinamis 100%",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusSafe,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // API Key Field with Show/Hide
                RinovaTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = "Kunci API Gemini (AI Studio)",
                    placeholder = "AIzaSy...",
                    leadingIcon = Icons.Default.Key,
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Active Model Tile (Searchable Bottom Sheet Trigger)
                RinovaSurfaceCard(
                    shapeRadius = 14.dp,
                    borderWidth = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val key = apiKeyInput.trim()
                        if (key.isEmpty()) {
                            actionNotification = "Masukkan API Key terlebih dahulu untuk melihat model."
                            return@RinovaSurfaceCard
                        }
                        if (availableModels.isEmpty()) {
                            isFetchingModels = true
                            coroutineScope.launch {
                                settingsRepository.saveGeminiApiKey(key)
                                val res = aiRepository.fetchAvailableModels(key)
                                isFetchingModels = false
                                res.fold(
                                    onSuccess = {
                                        showModelPickerSheet = true
                                    },
                                    onFailure = {
                                        actionNotification = "Gagal memuat model: ${it.localizedMessage}"
                                    }
                                )
                            }
                        } else {
                            showModelPickerSheet = true
                        }
                    }
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
                                text = "Model Gemini Aktif",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedModel,
                                style = MaterialTheme.typography.titleMedium,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (availableModels.isNotEmpty()) "${availableModels.size} model aktif terdeteksi • Ketuk untuk ganti" else "Ketuk untuk mendeteksi seluruh model dari API",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                        RinovaTactileButton(
                            text = if (isFetchingModels) "Memuat..." else "Pilih Model",
                            onClick = {
                                val key = apiKeyInput.trim()
                                if (key.isEmpty()) {
                                    actionNotification = "Masukkan API Key terlebih dahulu."
                                    return@RinovaTactileButton
                                }
                                if (availableModels.isEmpty()) {
                                    isFetchingModels = true
                                    coroutineScope.launch {
                                        settingsRepository.saveGeminiApiKey(key)
                                        val res = aiRepository.fetchAvailableModels(key)
                                        isFetchingModels = false
                                        res.fold(
                                            onSuccess = { showModelPickerSheet = true },
                                            onFailure = { actionNotification = it.localizedMessage }
                                        )
                                    }
                                } else {
                                    showModelPickerSheet = true
                                }
                            },
                            isSecondary = true,
                            height = 36.dp
                        )
                    }
                }

                // Action Row: Uji Limit & Latency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RinovaTactileButton(
                        text = if (isTestingModel) "Menguji..." else "Tes Limit & Key",
                        icon = Icons.Default.Speed,
                        onClick = {
                            val key = apiKeyInput.trim()
                            if (key.isEmpty()) {
                                actionNotification = "Masukkan API Key terlebih dahulu."
                                return@RinovaTactileButton
                            }
                            isTestingModel = true
                            coroutineScope.launch {
                                settingsRepository.saveGeminiApiKey(key)
                                settingsRepository.saveGeminiModel(selectedModel.trim())
                                val res = aiRepository.testModelAndKey(key, selectedModel.trim())
                                isTestingModel = false
                                res.fold(
                                    onSuccess = {
                                        actionNotification = if (it.isHealthy) "Model ${it.modelName} siap! Latency ${it.latencyMs}ms." else "Kendala: ${it.statusMessage}"
                                    },
                                    onFailure = { actionNotification = it.localizedMessage }
                                )
                            }
                        },
                        isSecondary = true,
                        modifier = Modifier.weight(1f),
                        height = 42.dp
                    )

                    RinovaTactileButton(
                        text = if (isFetchingModels) "Memuat..." else "Sinkron Model",
                        icon = Icons.Default.Refresh,
                        onClick = {
                            val key = apiKeyInput.trim()
                            if (key.isEmpty()) {
                                actionNotification = "Masukkan API Key terlebih dahulu."
                                return@RinovaTactileButton
                            }
                            isFetchingModels = true
                            coroutineScope.launch {
                                settingsRepository.saveGeminiApiKey(key)
                                val res = aiRepository.fetchAvailableModels(key)
                                isFetchingModels = false
                                res.fold(
                                    onSuccess = {
                                        actionNotification = "Berhasil memuat ${it.size} model resmi Google AI."
                                        showModelPickerSheet = true
                                    },
                                    onFailure = { actionNotification = it.localizedMessage }
                                )
                            }
                        },
                        isSecondary = true,
                        modifier = Modifier.weight(1f),
                        height = 42.dp
                    )
                }

                // Primary Run AI Audit Button
                RinovaTactileButton(
                    text = if (isAnalyzing) "Menganalisis Sistem..." else "Audit AI Berbasis Data Empiris",
                    icon = Icons.Default.AutoAwesome,
                    onClick = {
                        focusManager.clearFocus()
                        val key = apiKeyInput.trim()
                        val model = selectedModel.trim()
                        if (key.isEmpty()) {
                            actionNotification = "Masukkan API Key terlebih dahulu."
                            return@RinovaTactileButton
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
                    modifier = Modifier.fillMaxWidth(),
                    height = 50.dp
                )
            }
        }

        // Model & Quota Test Result Card
        if (testResult != null) {
            val tr = testResult!!
            val bannerColor = if (tr.isHealthy) StatusSafe else StatusCritical
            RinovaSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 14.dp
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
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall,
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
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = tr.statusMessage,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Status Quota: ${tr.quotaStatus}",
                            color = if (tr.quotaStatus.contains("Habis") || tr.quotaStatus.contains("Error")) StatusCritical else StatusSafe,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (tr.inputTokenLimit > 0L) {
                            Text(
                                text = "Token Limit: ${tr.inputTokenLimit / 1000}k in / ${tr.outputTokenLimit / 1000}k out",
                                color = Primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
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

        // Report Section
        if (isAnalyzing) {
            SkeletonCard()
            repeat(2) { SkeletonCard() }
        } else if (report != null) {
            val r = report!!

            // Health Score & Summary
            RinovaSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 16.dp
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
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            if (r.modelUsed.isNotEmpty()) {
                                Text(
                                    text = "Dianalisis oleh: ${r.modelUsed}",
                                    color = Primary,
                                    style = MaterialTheme.typography.labelSmall
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
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = r.deviceSummary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }

            // Empirical Observations
            if (r.empiricalObservations.isNotEmpty()) {
                Text(
                    text = "Temuan Fakta Empiris di Sistem",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                RinovaSurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 14.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        r.empiricalObservations.forEach { obs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("•", color = Primary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = obs,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Recommendations (Structured Action Cards)
            if (r.recommendations.isNotEmpty()) {
                Text(
                    text = "Rekomendasi Tindakan Terkontrol (${r.recommendations.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
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

                    RinovaSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = rec.proposedActionTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
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
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = rec.empiricalEvidence,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 16.sp
                            )

                            if (rec.adbCommand.isNotEmpty()) {
                                RinovaSurfaceCard(
                                    shapeRadius = 8.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = rec.adbCommand,
                                        color = Primary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                RinovaTactileButton(
                                    text = if (rec.riskLevel == AiActionRisk.CRITICAL_KEEP) "Tinjau Larangan" else "Tinjau & Eksekusi",
                                    onClick = { pendingRecommendation = rec },
                                    isSecondary = rec.riskLevel != AiActionRisk.SAFE_ACTION,
                                    height = 36.dp
                                )
                            }
                        }
                    }
                }
            }

            // System Optimizations
            if (r.optimizations.isNotEmpty()) {
                Text(
                    text = "Saran Optimalisasi Sistem",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                r.optimizations.forEach { opt ->
                    RinovaSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = opt.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${opt.rationale} • Dampak: ${opt.estimatedImpact}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                RinovaTactileButton(
                                    text = "Terapkan",
                                    onClick = { pendingOptimization = opt },
                                    icon = Icons.Default.PlayArrow,
                                    height = 36.dp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            RinovaSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                shapeRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Belum Ada Hasil Audit",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pastikan HP target terhubung dan API Key telah disimpan, lalu tekan 'Audit AI Berbasis Data Empiris' untuk menganalisis seluruh data telemetri, storage, dan aplikasi berisiko.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
