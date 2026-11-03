package com.rinova.app.core.health

enum class HealthCategory {
    STORAGE,
    ADWARE,
    PERFORMANCE,
    SECURITY
}

enum class HealthSeverity {
    CRITICAL,
    WARNING,
    INFO,
    SAFE
}

data class HealthIssue(
    val id: String,
    val category: HealthCategory,
    val title: String,
    val humanDescription: String,
    val technicalCommand: String,
    val severity: HealthSeverity,
    val potentialSavings: String = "",
    val isResolved: Boolean = false
)

data class HealthScanResult(
    val totalIssuesFound: Int,
    val safeItemsCount: Int,
    val issues: List<HealthIssue>,
    val timestamp: Long = System.currentTimeMillis()
)
