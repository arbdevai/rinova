package com.rinova.app.core.util

import java.util.Locale

enum class VendorCategory(val displayName: String) {
    XIAOMI("Xiaomi / POCO / Redmi"),
    SAMSUNG("Samsung Galaxy"),
    OPPO_REALME("Oppo / Realme / OnePlus"),
    VIVO("Vivo / iQOO"),
    TRANSSION("Infinix / Tecno / Itel"),
    GOOGLE("Google Pixel"),
    MOTOROLA("Motorola"),
    UNIVERSAL("Universal Android AOSP")
}

object VendorProfileDetector {
    fun detect(manufacturer: String, brand: String, customRomTag: String = ""): VendorCategory {
        val combined = "$manufacturer $brand $customRomTag".lowercase(Locale.ROOT)
        return when {
            combined.contains("xiaomi") || combined.contains("redmi") || combined.contains("poco") || combined.contains("miui") || combined.contains("hyperos") -> VendorCategory.XIAOMI
            combined.contains("samsung") -> VendorCategory.SAMSUNG
            combined.contains("oppo") || combined.contains("realme") || combined.contains("oneplus") || combined.contains("coloros") || combined.contains("oxygen") -> VendorCategory.OPPO_REALME
            combined.contains("vivo") || combined.contains("iqoo") || combined.contains("funtouch") || combined.contains("origin") -> VendorCategory.VIVO
            combined.contains("infinix") || combined.contains("tecno") || combined.contains("itel") || combined.contains("transsion") || combined.contains("xos") || combined.contains("hios") -> VendorCategory.TRANSSION
            combined.contains("google") || combined.contains("pixel") -> VendorCategory.GOOGLE
            combined.contains("motorola") || combined.contains("moto") -> VendorCategory.MOTOROLA
            else -> VendorCategory.UNIVERSAL
        }
    }
}
