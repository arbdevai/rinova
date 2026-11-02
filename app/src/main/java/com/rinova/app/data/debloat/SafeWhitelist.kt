package com.rinova.app.data.debloat

/**
 * Paket sistem kritis Android yang DILARANG KERAS diubah, dinonaktifkan, atau dicopot.
 * Digunakan sebagai pengaman (safety shield) agar teknisi terhindar dari resiko bootloop.
 */
object SafeWhitelist {
    val CRITICAL_SYSTEM_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.settings",
        "com.android.settings.intelligence",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.providers.settings",
        "com.android.providers.telephony",
        "com.android.providers.media",
        "com.android.providers.media.module",
        "com.android.providers.downloads",
        "com.android.providers.contacts",
        "com.android.keychain",
        "com.android.inputmethod.latin",
        "com.google.android.inputmethod.latin",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.android.wifi.resources",
        "com.android.networkstack",
        "com.android.networkstack.tethering",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller"
    )

    fun isCritical(packageName: String): Boolean {
        return CRITICAL_SYSTEM_PACKAGES.contains(packageName.trim().lowercase())
    }
}
