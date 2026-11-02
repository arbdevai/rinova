package com.rinova.app.data.repository

import com.rinova.app.core.adb.AdbConnectionManager
import com.rinova.app.core.ai.GeminiApiClient
import com.rinova.app.core.ai.GeminiPromptBuilder
import com.rinova.app.core.model.AdbConnectionState
import com.rinova.app.core.model.AiDiagnosticReport
import com.rinova.app.core.model.AiRecommendation
import com.rinova.app.core.model.GeminiModelInfo
import com.rinova.app.core.model.GeminiTestResult
import com.rinova.app.data.debloat.SafeWhitelist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AiDiagnosticRepository(
    private val adbManager: AdbConnectionManager,
    private val storageRepo: StorageRepository,
    private val adwareRepo: AdwareHunterRepository,
    private val geminiClient: GeminiApiClient
) {
    private val _diagnosticReport = MutableStateFlow<AiDiagnosticReport?>(null)
    val diagnosticReport: StateFlow<AiDiagnosticReport?> = _diagnosticReport.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisStep = MutableStateFlow("")
    val analysisStep: StateFlow<String> = _analysisStep.asStateFlow()

    private val _testResult = MutableStateFlow<GeminiTestResult?>(null)
    val testResult: StateFlow<GeminiTestResult?> = _testResult.asStateFlow()

    private val _availableModels = MutableStateFlow<List<GeminiModelInfo>>(emptyList())
    val availableModels: StateFlow<List<GeminiModelInfo>> = _availableModels.asStateFlow()

    suspend fun fetchAvailableModels(apiKey: String): Result<List<GeminiModelInfo>> {
        val res = geminiClient.fetchAvailableModels(apiKey)
        res.onSuccess { _availableModels.value = it }
        return res
    }

    suspend fun testModelAndKey(apiKey: String, modelName: String): Result<GeminiTestResult> {
        val res = geminiClient.testModelAndKey(apiKey, modelName)
        res.onSuccess { _testResult.value = it }
        return res
    }

    suspend fun runAiAudit(apiKey: String, modelName: String = "gemini-2.0-flash"): Result<AiDiagnosticReport> = withContext(Dispatchers.IO) {
        val connState = adbManager.connectionState.value
        if (connState !is AdbConnectionState.Connected) {
            return@withContext Result.failure(IllegalStateException("Hubungkan HP teknisi ke HP target terlebih dahulu."))
        }

        _isAnalyzing.value = true
        _analysisStep.value = "Mengumpulkan telemetri sistem & partisi..."

        try {
            // 1. Dapatkan storage breakdown empiris
            _analysisStep.value = "Mengumpulkan metrik penyimpanan internal..."
            val storageData = storageRepo.storageState.value.let {
                if (it.totalBytes == 0L) {
                    storageRepo.analyzeStorage().getOrThrow()
                } else it
            }

            // 2. Dapatkan data ancaman aplikasi
            _analysisStep.value = "Mengaudit perizinan aplikasi & overlay..."
            val suspiciousApps = adwareRepo.scannedThreats.value.ifEmpty {
                adwareRepo.scanSuspiciousApps().getOrDefault(emptyList())
            }

            // 3. Tangkap jendela teratas
            _analysisStep.value = "Mendeteksi jendela aplikasi aktif..."
            val topWindow = adwareRepo.captureTopActiveWindow().getOrNull()

            // 4. Susun prompt empiris
            _analysisStep.value = "Menyusun audit data untuk analisis Gemini AI..."
            val prompt = GeminiPromptBuilder.buildEmpiricalPrompt(
                device = connState.device,
                storage = storageData,
                suspiciousApps = suspiciousApps,
                topWindowApp = topWindow
            )

            // 5. Eksekusi analisis AI dengan model dinamis
            _analysisStep.value = "Menghubungi Gemini AI ($modelName)..."
            val reportResult = geminiClient.analyze(apiKey, prompt, modelName)

            reportResult.fold(
                onSuccess = { report ->
                    _analysisStep.value = "Audit selesai."
                    _diagnosticReport.value = report
                    Result.success(report)
                },
                onFailure = { err ->
                    _analysisStep.value = "Gagal: ${err.localizedMessage}"
                    Result.failure(err)
                }
            )
        } catch (e: Exception) {
            _analysisStep.value = "Gagal memproses data: ${e.localizedMessage}"
            Result.failure(e)
        } finally {
            _isAnalyzing.value = false
        }
    }

    suspend fun executeRecommendation(recommendation: AiRecommendation): Result<String> = withContext(Dispatchers.IO) {
        val cmd = recommendation.adbCommand.trim()
        if (cmd.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Perintah tidak valid"))
        }

        for (pkg in SafeWhitelist.CRITICAL_SYSTEM_PACKAGES) {
            if (cmd.contains("disable") && cmd.contains(pkg)) {
                return@withContext Result.failure(IllegalStateException("Perintah diblokir: Paket $pkg adalah komponen sistem kritis"))
            }
        }

        adbManager.execute(cmd).map { it.allOutput }
    }

    suspend fun executeOptimization(command: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanCmd = command.trim()
        if (cleanCmd.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Perintah kosong"))
        adbManager.execute(cleanCmd).map { it.allOutput }
    }
}
