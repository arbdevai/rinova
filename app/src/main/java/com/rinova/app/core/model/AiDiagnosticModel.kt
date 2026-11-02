package com.rinova.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AiActionRisk {
    @SerialName("SAFE_ACTION")
    SAFE_ACTION,

    @SerialName("OPTIONAL_TWEAK")
    OPTIONAL_TWEAK,

    @SerialName("CRITICAL_KEEP")
    CRITICAL_KEEP
}

@Serializable
data class AiRecommendation(
    val id: String = "",
    val targetName: String = "",
    val riskLevel: AiActionRisk = AiActionRisk.SAFE_ACTION,
    val empiricalEvidence: String = "",
    val proposedActionTitle: String = "",
    val adbCommand: String = "",
    val isExecutable: Boolean = true
)

@Serializable
data class AiOptimization(
    val title: String = "",
    val rationale: String = "",
    val adbCommand: String = "",
    val estimatedImpact: String = ""
)

@Serializable
data class AiDiagnosticReport(
    val deviceSummary: String = "",
    val overallHealthScore: Int = 100, // 0 - 100
    val empiricalObservations: List<String> = emptyList(),
    val recommendations: List<AiRecommendation> = emptyList(),
    val optimizations: List<AiOptimization> = emptyList(),
    val criticalWarnings: List<String> = emptyList(),
    val generatedAtEpochMs: Long = 0L,
    val modelUsed: String = ""
)

@Serializable
data class GeminiModelInfo(
    val id: String, // e.g. "gemini-2.0-flash"
    val displayName: String = "",
    val description: String = "",
    val inputTokenLimit: Long = 0L,
    val outputTokenLimit: Long = 0L,
    val isRecommended: Boolean = false
)

@Serializable
data class GeminiTestResult(
    val isHealthy: Boolean,
    val modelName: String,
    val latencyMs: Long,
    val inputTokenLimit: Long,
    val outputTokenLimit: Long,
    val statusMessage: String,
    val quotaStatus: String = "Normal"
)
