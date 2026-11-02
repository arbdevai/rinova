package com.rinova.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val manufacturer: String = "Unknown",
    val brand: String = "Generic",
    val model: String = "Android Device",
    val deviceCodename: String = "",
    val androidVersion: String = "11",
    val sdkInt: Int = 30,
    val securityPatch: String = "-",
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val customRomBrand: String = "AOSP",
    val cpuAbi: String = "arm64-v8a",
    val totalRamMb: Long = 0L,
    val freeRamMb: Long = 0L
)
