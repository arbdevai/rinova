package com.rinova.app.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class DebloatRisk {
    SAFE,       // Aman dinonaktifkan/dihapus (misal: iklan, analytics, game store pabrikan)
    ADVANCED,   // Fitur opsional (misal: Bixby, Mi Cloud, Theme Store, bawaan browser)
    CRITICAL    // Komponen sistem esensial (SystemUI, Telepon, Pengaturan, SMS) - Dilarang disentuh!
}

@Serializable
data class DebloatPackage(
    val packageName: String,
    val appLabel: String,
    val vendorCategory: String, // Xiaomi, Samsung, Oppo, Vivo, Transsion, Google, AOSP
    val risk: DebloatRisk,
    val description: String,
    val isInstalled: Boolean = true,
    val isEnabled: Boolean = true
)
