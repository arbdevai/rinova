package com.rinova.app.data.repository

import com.rinova.app.core.adb.AdbConnectionManager
import com.rinova.app.core.ai.GeminiApiClient
import com.rinova.app.core.ai.GeminiPromptBuilder
import com.rinova.app.core.model.AdbConnectionState
import com.rinova.app.core.model.AiDiagnosticReport
import com.rinova.app.core.model.AiRecommendation
import com.rinova.app.core.model.GeminiModelInfo
import com.rinova.app.core.model.GeminiTestResult
import com.rinova.app.core.model.AiChatAction
import com.rinova.app.core.model.ChatMessage
import com.rinova.app.core.model.DeviceInfo
import com.rinova.app.core.model.MessageSender
import com.rinova.app.core.util.FileSizeFormatter
import com.rinova.app.data.debloat.SafeWhitelist
import java.io.IOException
import kotlinx.serialization.json.Json
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

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isSendingChat = MutableStateFlow(false)
    val isSendingChat: StateFlow<Boolean> = _isSendingChat.asStateFlow()

    fun initChatSessionIfNeeded(device: DeviceInfo?) {
        if (_chatMessages.value.isEmpty()) {
            val resolvedDevice = device ?: (adbManager.connectionState.value as? AdbConnectionState.Connected)?.device
            val devName = if (resolvedDevice != null) "${resolvedDevice.brand} ${resolvedDevice.model}" else "HP Android Pasien"
            val androidVer = resolvedDevice?.androidVersion ?: "14"
            val welcomeText = "Halo! Saya Dr. Rinova, asisten pintar optimasi Android Anda.\n\nSaya telah terhubung ke perangkat target $devName (Android $androidVer). Apa kendala atau keluhan yang sedang dialami? (Misalnya: HP lambat/lemot saat main game, memori penuh, baterai boros, atau ingin membasmi iklan & bloatware tersembunyi?)"
            _chatMessages.value = listOf(
                ChatMessage(
                    sender = MessageSender.AI,
                    text = welcomeText
                )
            )
        }
    }

    suspend fun sendChatMessage(
        apiKey: String,
        userText: String,
        modelName: String = "gemini-2.0-flash"
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        val cleanUserText = userText.trim()
        if (cleanUserText.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Pesan tidak boleh kosong"))
        }

        val connState = adbManager.connectionState.value
        val device = if (connState is AdbConnectionState.Connected) connState.device else null

        // 1. Tambahkan pesan user ke antrean chat
        val userMsg = ChatMessage(sender = MessageSender.USER, text = cleanUserText)
        _chatMessages.value = _chatMessages.value + userMsg

        _isSendingChat.value = true

        try {
            // 2. Kumpulkan telemetri riil dari perangkat target
            val realTelemetry = buildRealDeviceTelemetry(device)

            // 3. Susun System Instruction Dr. Rinova yang kaya konteks empiris lapangan
            val systemInstruction = buildString {
                append("Anda adalah Dr. Rinova, konsultan dokter HP AI dan asisten optimasi Android otonom yang ramah, santai, empatik, berbasis data riil lapangan, dan anti-jawaban template kosong.\n")
                append("Anda terhubung langsung ke perangkat target melalui ADB Wireless dan memiliki akses ke alat pencarian web Google Search secara live untuk meriset paket/aplikasi asing.\n\n")
                append(realTelemetry)
                append("\n\nATURAN RESPON & TINDAKAN:\n")
                append("1. Berbicaralah dalam Bahasa Indonesia yang ramah, menenangkan, tidak kaku, dan mudah dimengerti (seperti teknisi ahli yang menjelaskan ke sahabat sendiri).\n")
                append("2. RUJUK DATA RIIL DI ATAS! Sebutkan tipe HP spesifik (${device?.brand ?: "Android"} ${device?.model ?: ""}), sisa penyimpanan riil, aplikasi pihak ketiga yang benar-benar terpasang di HP tersebut, dan RAM aktual jika relevan dengan pertanyaan user.\n")
                append("3. JIKA ADA APLIKASI ATAU PAKET ASING yang ditanyakan atau ditemukan mencurigakan (misalnya CapCut com.lemon.lvoverseas, ReVanced app.morphe.android.youtube, game Roblox, dsb), gunakan pengetahuan web Google Search untuk menjelaskan fungsi aplikasi tersebut secara akurat.\n")
                append("4. JIKA MENYARANKAN TINDAKAN PERBAIKAN (misal bersihkan cache sistem, matikan RAM Plus, debloat adware sponsor, atau optimasi), WAJIB sertakan kartu aksi terstruktur di akhir respons Anda dalam blok markdown ```action ... ``` persis seperti ini:\n")
                append("```action\n")
                append("{\n")
                append("  \"title\": \"Judul Singkat Tindakan\",\n")
                append("  \"reason\": \"Alasan jelas & menenangkan\",\n")
                append("  \"command\": \"perintah adb valid\",\n")
                append("  \"isSafe\": true\n")
                append("}\n")
                append("```\n")
                append("Rinova akan otomatis mengubah blok ```action``` tersebut menjadi tombol aksi 1-klik di layar pengguna!")
            }

            // 4. Susun histori percakapan
            val history = _chatMessages.value.map { msg ->
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                Pair(role, msg.text)
            }

            // 5. Panggil Gemini dengan Google Search Grounding aktif
            val responseResult = geminiClient.chat(
                apiKey = apiKey,
                systemInstructionText = systemInstruction,
                messages = history,
                modelName = modelName,
                enableSearchGrounding = true
            )
            if (responseResult.isFailure) {
                return@withContext Result.failure(responseResult.exceptionOrNull() ?: IOException("Gagal memanggil Gemini"))
            }

            val rawResponse = responseResult.getOrThrow()

            // 5. Ekstrak blok ```action ... ``` menjadi AiChatAction
            val actionRegex = Regex("""```action\s*(\{.*?\})\s*```""", RegexOption.DOT_MATCHES_ALL)
            val extractedActions = mutableListOf<AiChatAction>()

            actionRegex.findAll(rawResponse).forEach { match ->
                val jsonStr = match.groupValues[1]
                try {
                    val root = Json { ignoreUnknownKeys = true; isLenient = true }.parseToJsonElement(jsonStr)
                    val obj = root as? kotlinx.serialization.json.JsonObject
                    val title = obj?.get("title")?.toString()?.replace("\"", "").orEmpty()
                    val reason = obj?.get("reason")?.toString()?.replace("\"", "").orEmpty()
                    val command = obj?.get("command")?.toString()?.replace("\"", "").orEmpty()
                    val isSafe = obj?.get("isSafe")?.toString()?.toBooleanStrictOrNull() ?: true

                    if (title.isNotEmpty() && command.isNotEmpty()) {
                        extractedActions.add(
                            AiChatAction(
                                title = title,
                                reason = reason,
                                shellCommand = command,
                                isSafe = isSafe
                            )
                        )
                    }
                } catch (_: Exception) {}
            }

            // Bersihkan teks dari blok ```action``` agar bubble chat tetap rapi
            val cleanBubbleText = rawResponse.replace(actionRegex, "").trim()

            val aiMsg = ChatMessage(
                sender = MessageSender.AI,
                text = cleanBubbleText,
                actions = extractedActions
            )

            _chatMessages.value = _chatMessages.value + aiMsg
            Result.success(aiMsg)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isSendingChat.value = false
        }
    }

    suspend fun executeChatAction(action: AiChatAction): Result<String> = withContext(Dispatchers.IO) {
        val cmd = action.shellCommand.trim()
        if (cmd.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Perintah tidak valid"))

        val execRes = adbManager.execute(cmd)
        execRes.fold(
            onSuccess = { res ->
                // Perbarui status action menjadi isExecuted = true di dalam list pesan
                val updatedMessages = _chatMessages.value.map { msg ->
                    val updatedActions = msg.actions.map { a ->
                        if (a.actionId == action.actionId) a.copy(isExecuted = true) else a
                    }
                    msg.copy(actions = updatedActions)
                }
                _chatMessages.value = updatedMessages
                Result.success(res.allOutput.ifEmpty { "Berhasil dijalankan." })
            },
            onFailure = { err ->
                Result.failure(err)
            }
        )
    }

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

    private suspend fun buildRealDeviceTelemetry(device: DeviceInfo?): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.appendLine("=== TELEMETRI REAL-TIME PERANGKAT TARGET ===")
        if (device != null) {
            sb.appendLine("1. IDENTITAS PERANGKAT:")
            sb.appendLine("   - Merk & Model: ${device.brand} ${device.model} (${device.manufacturer})")
            sb.appendLine("   - Versi OS: Android ${device.androidVersion} (API SDK ${device.sdkInt}, Patch ${device.securityPatch})")
            sb.appendLine("   - CPU ABI: ${device.cpuAbi}")
            sb.appendLine("   - Baterai: ${device.batteryLevel}% (${if (device.isCharging) "Sedang Diisi Daya" else "Menggunakan Baterai"})")
            sb.appendLine("   - Memori RAM: ${device.freeRamMb} MB bebas dari total ${device.totalRamMb} MB")
            if (device.customRomBrand.isNotEmpty()) {
                sb.appendLine("   - Varian Vendor / ROM: ${device.customRomBrand}")
            }
        } else {
            sb.appendLine("1. IDENTITAS PERANGKAT: Belum terhubung ke ADB.")
        }

        // Storage Breakdown
        val storage = storageRepo.storageState.value
        if (storage.totalBytes > 0) {
            sb.appendLine("\n2. PENYIMPANAN FISIK (/data):")
            sb.appendLine("   - Total Kapasitas: ${FileSizeFormatter.format(storage.totalBytes)}")
            sb.appendLine("   - Terpakai: ${FileSizeFormatter.format(storage.usedBytes)}")
            sb.appendLine("   - Ruang Bebas: ${FileSizeFormatter.format(storage.freeBytes)}")
            sb.appendLine("   - Akumulasi App Cache: ${FileSizeFormatter.format(storage.appCacheBytes)}")
            if (storage.orphanedDirectories.isNotEmpty()) {
                sb.appendLine("   - Folder Sisa (Orphaned) (${storage.orphanedDirectories.size} folder): ${storage.orphanedDirectories.joinToString { "${it.folderName} (${FileSizeFormatter.format(it.sizeBytes)})" }}")
            }
        }

        // 3rd Party Installed Packages
        try {
            val pkgResp = adbManager.execute("pm list packages -3", 4000)
            val pkgs = pkgResp.getOrNull()?.output?.lines()
                ?.map { it.substringAfter("package:").trim() }
                ?.filter { it.isNotEmpty() && !it.contains(" ") }
                ?: emptyList()
            if (pkgs.isNotEmpty()) {
                sb.appendLine("\n3. APLIKASI PIHAK KETIGA YANG TERPASANG (${pkgs.size} aplikasi):")
                sb.appendLine("   " + pkgs.take(30).joinToString(", "))
            }
        } catch (_: Exception) {}

        // Threats detected by Adware Hunter
        val threats = adwareRepo.scannedThreats.value
        if (threats.isNotEmpty()) {
            sb.appendLine("\n4. TEMUAN ADWARE & ANOMALI KEAMANAN:")
            threats.forEach { t ->
                sb.appendLine("   - [${t.severity}] ${t.packageName} -> ${t.evidenceReason}")
            }
        }

        sb.toString()
    }
}
