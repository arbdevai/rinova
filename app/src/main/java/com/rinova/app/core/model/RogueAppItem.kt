package com.rinova.app.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThreatSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    SAFE
}

@Serializable
data class RogueAppItem(
    val packageName: String,
    val appName: String = "",
    val isSystemApp: Boolean = false,
    val hasLauncherIcon: Boolean = true,
    val hasOverlayPermission: Boolean = false,
    val hasBootCompletedPermission: Boolean = false,
    val hasAccessibilityService: Boolean = false,
    val isCurrentlyActiveOnTop: Boolean = false,
    val detectedSdkIndicators: List<String> = emptyList(),
    val severity: ThreatSeverity = ThreatSeverity.LOW,
    val evidenceReason: String = ""
)
