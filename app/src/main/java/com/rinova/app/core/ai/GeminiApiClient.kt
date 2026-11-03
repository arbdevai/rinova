package com.rinova.app.core.ai

import com.rinova.app.core.model.AiDiagnosticReport
import com.rinova.app.core.model.GeminiModelInfo
import com.rinova.app.core.model.GeminiTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
private data class GeminiPart(val text: String)

@Serializable
private data class GeminiContent(val parts: List<GeminiPart>)

@Serializable
private data class GeminiGenerationConfig(
    val temperature: Float = 0.2f,
    val responseMimeType: String = "application/json"
)

@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

@Serializable
private data class GeminiSystemInstruction(val parts: List<GeminiPart>)

@Serializable
private data class GeminiChatContent(val role: String, val parts: List<GeminiPart>)

@Serializable
private data class GeminiChatRequest(
    val contents: List<GeminiChatContent>,
    val systemInstruction: GeminiSystemInstruction? = null,
    val generationConfig: GeminiChatGenerationConfig = GeminiChatGenerationConfig()
)

@Serializable
private data class GeminiChatGenerationConfig(
    val temperature: Float = 0.7f
)

@Serializable
private data class GeminiCandidate(val content: GeminiContent? = null)

@Serializable
private data class GeminiResponse(val candidates: List<GeminiCandidate>? = null)

@Serializable
private data class GeminiRemoteModel(
    val name: String = "",
    val displayName: String = "",
    val description: String = "",
    val inputTokenLimit: Long = 0L,
    val outputTokenLimit: Long = 0L,
    val supportedGenerationMethods: List<String> = emptyList()
)

@Serializable
private data class GeminiListModelsResponse(
    val models: List<GeminiRemoteModel> = emptyList()
)

class GeminiApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun fetchAvailableModels(apiKey: String): Result<List<GeminiModelInfo>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Kunci API Gemini belum dimasukkan"))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$cleanKey"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val rawBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(parseHttpError(response.code, rawBody))
            }

            val listResponse = json.decodeFromString(GeminiListModelsResponse.serializer(), rawBody)
            val filtered = listResponse.models
                .filter { it.supportedGenerationMethods.contains("generateContent") }
                .map { m ->
                    val cleanId = m.name.removePrefix("models/")
                    GeminiModelInfo(
                        id = cleanId,
                        displayName = m.displayName.ifEmpty { cleanId },
                        description = m.description,
                        inputTokenLimit = m.inputTokenLimit,
                        outputTokenLimit = m.outputTokenLimit,
                        isRecommended = cleanId.contains("flash", ignoreCase = true)
                    )
                }
                .sortedByDescending { it.id.contains("2.5") || it.id.contains("2.0") }

            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testModelAndKey(apiKey: String, modelName: String): Result<GeminiTestResult> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanModel = modelName.trim().removePrefix("models/")
        if (cleanKey.isEmpty() || cleanModel.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Kunci API atau Nama Model tidak boleh kosong."))
        }

        val startTime = System.currentTimeMillis()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel?key=$cleanKey"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val latencyMs = System.currentTimeMillis() - startTime
            val rawBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val err = parseHttpError(response.code, rawBody)
                return@withContext Result.success(
                    GeminiTestResult(
                        isHealthy = false,
                        modelName = cleanModel,
                        latencyMs = latencyMs,
                        inputTokenLimit = 0L,
                        outputTokenLimit = 0L,
                        statusMessage = err.message ?: "Gagal memverifikasi model",
                        quotaStatus = if (response.code == 429) "Quota Habis / Terlampaui" else "Error (${response.code})"
                    )
                )
            }

            val modelMeta = json.decodeFromString(GeminiRemoteModel.serializer(), rawBody)
            Result.success(
                GeminiTestResult(
                    isHealthy = true,
                    modelName = cleanModel,
                    latencyMs = latencyMs,
                    inputTokenLimit = modelMeta.inputTokenLimit,
                    outputTokenLimit = modelMeta.outputTokenLimit,
                    statusMessage = "Koneksi aktif dan model siap digunakan (Latency ${latencyMs}ms).",
                    quotaStatus = "Normal (Siap Pakai)"
                )
            )
        } catch (e: Exception) {
            val latencyMs = System.currentTimeMillis() - startTime
            Result.success(
                GeminiTestResult(
                    isHealthy = false,
                    modelName = cleanModel,
                    latencyMs = latencyMs,
                    inputTokenLimit = 0L,
                    outputTokenLimit = 0L,
                    statusMessage = "Koneksi gagal: ${e.localizedMessage}",
                    quotaStatus = "Offline"
                )
            )
        }
    }

    suspend fun analyze(
        apiKey: String,
        prompt: String,
        modelName: String = "gemini-2.0-flash"
    ): Result<AiDiagnosticReport> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanModel = modelName.trim().removePrefix("models/")
        if (cleanKey.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Kunci API Gemini belum dimasukkan. Silakan atur di menu Pengaturan."))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$cleanKey"
        val requestBodyObject = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(prompt))
                )
            )
        )
        val jsonPayload = json.encodeToString(GeminiRequest.serializer(), requestBodyObject)
        val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Accept", "application/json")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val rawBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(parseHttpError(response.code, rawBody))
            }

            val parsedResponse = json.decodeFromString(GeminiResponse.serializer(), rawBody)
            val jsonText = parsedResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(IOException("Respons Gemini tidak mengandung data konten yang valid."))

            val cleanJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val report = json.decodeFromString(AiDiagnosticReport.serializer(), cleanJson)
            Result.success(report.copy(
                generatedAtEpochMs = System.currentTimeMillis(),
                modelUsed = cleanModel
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chat(
        apiKey: String,
        systemInstructionText: String,
        messages: List<Pair<String, String>>,
        modelName: String = "gemini-2.0-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanModel = modelName.trim().removePrefix("models/")
        if (cleanKey.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Kunci API Gemini belum dimasukkan. Silakan atur di menu Pengaturan."))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$cleanKey"
        val chatContents = messages.map { (role, text) ->
            GeminiChatContent(
                role = if (role == "user") "user" else "model",
                parts = listOf(GeminiPart(text))
            )
        }

        val req = GeminiChatRequest(
            contents = chatContents,
            systemInstruction = if (systemInstructionText.isNotEmpty()) {
                GeminiSystemInstruction(parts = listOf(GeminiPart(systemInstructionText)))
            } else null
        )

        val jsonPayload = json.encodeToString(GeminiChatRequest.serializer(), req)
        val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Accept", "application/json")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val rawBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(parseHttpError(response.code, rawBody))
            }

            val parsedResponse = json.decodeFromString(GeminiResponse.serializer(), rawBody)
            val text = parsedResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(IOException("Respons Gemini kosong."))

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseHttpError(code: Int, rawBody: String): Exception {
        if (rawBody.trimStart().startsWith("<") || rawBody.contains("<html", ignoreCase = true)) {
            return IOException("Server Gemini sedang dalam pemeliharaan (Maintenance). Silakan coba beberapa saat lagi.")
        }
        return when (code) {
            429 -> IOException("Batas Quota / Rate Limit Terlampaui (HTTP 429). Model ini sedang mencapai limit panggilan gratis Google AI Studio. Silakan tunggu beberapa saat atau pilih model Flash alternatif.")
            404 -> IOException("Model tidak ditemukan atau sudah usang (HTTP 404). Silakan periksa daftar model aktif dengan menekan tombol 'Muat Model'.")
            400 -> IOException("Permintaan tidak valid (HTTP 400). Periksa format request atau model yang dipilih.")
            403 -> IOException("Akses ditolak (HTTP 403 Forbidden). Kunci API tidak valid atau tidak memiliki izin akses ke model ini.")
            else -> IOException("Gagal menghubungi Gemini API (HTTP $code): $rawBody")
        }
    }
}
