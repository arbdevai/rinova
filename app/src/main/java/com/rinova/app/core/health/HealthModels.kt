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
    val timestamp: Long = System.currentTimeMillis(),
    val initialScore: Int = 100,
    val targetOptimalScore: Int = 100,
    val initialCacheEstimate: String = "0 B",
    val initialAdwareCount: Int = 0,
    val initialRamStatus: String = "Normal",
    val targetRamStatus: String = "Optimal"
) {
    val currentScore: Int
        get() {
            val unresolved = issues.filterNot { it.isResolved }
            val penalties = unresolved.sumOf {
                when (it.severity) {
                    HealthSeverity.CRITICAL -> 25
                    HealthSeverity.WARNING -> 12
                    HealthSeverity.INFO -> 5
                    HealthSeverity.SAFE -> 0
                }
            }
            return (100 - penalties).coerceIn(10, 100)
        }
}
