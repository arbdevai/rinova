package com.rinova.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class OrphanedDirectory(
    val path: String,
    val sizeBytes: Long,
    val folderName: String,
    val reason: String = "Aplikasi pemilik sudah tidak terpasang di sistem"
)

@Serializable
data class StorageBreakdown(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val systemAppBytes: Long = 0L,
    val userDataBytes: Long = 0L,
    val appCacheBytes: Long = 0L,
    val orphanedObbBytes: Long = 0L,
    val crashDumpsBytes: Long = 0L,
    val hiddenMediaCacheBytes: Long = 0L,
    val orphanedDirectories: List<OrphanedDirectory> = emptyList(),
    val isAnalyzing: Boolean = false,
    val currentStepText: String = "",
    val lastScanTimestamp: Long = 0L
)
