package com.rinova.app.data.debloat

import com.rinova.app.core.model.DebloatPackage
import com.rinova.app.core.model.DebloatRisk
import com.rinova.app.core.util.VendorCategory

object UniversalVendorRules {
    fun getPresetRules(category: VendorCategory): List<DebloatPackage> {
        val list = mutableListOf<DebloatPackage>()
        when (category) {
            VendorCategory.XIAOMI -> {
                list.addAll(xiaomiRules)
                list.addAll(universalAdwareBloat)
            }
            VendorCategory.SAMSUNG -> {
                list.addAll(samsungRules)
                list.addAll(universalAdwareBloat)
            }
            VendorCategory.OPPO_REALME -> {
                list.addAll(oppoRealmeRules)
                list.addAll(universalAdwareBloat)
            }
            VendorCategory.VIVO -> {
                list.addAll(vivoRules)
                list.addAll(universalAdwareBloat)
            }
            VendorCategory.TRANSSION -> {
                list.addAll(transsionRules)
                list.addAll(universalAdwareBloat)
            }
            VendorCategory.GOOGLE -> {
                list.addAll(googleRules)
            }
            VendorCategory.MOTOROLA -> {
                list.addAll(motorolaRules)
                list.addAll(universalAdwareBloat)
            }
            VendorCategory.UNIVERSAL -> {
                list.addAll(universalAdwareBloat)
            }
        }
        return list
    }

    private val xiaomiRules = listOf(
        DebloatPackage("com.miui.msa.global", "MIUI Ad Services (MSA)", "Xiaomi", DebloatRisk.SAFE, "Penyedia iklan utama di sistem Xiaomi/Redmi"),
        DebloatPackage("com.miui.analytics", "Analytics Daemon", "Xiaomi", DebloatRisk.SAFE, "Layanan pengumpul telemetri & kebiasaan pengguna"),
        DebloatPackage("com.xiaomi.glgm", "Games Center (Mi Game)", "Xiaomi", DebloatRisk.SAFE, "Aplikasi promosi game bawaan"),
        DebloatPackage("com.xiaomi.mipicks", "GetApps", "Xiaomi", DebloatRisk.SAFE, "Toko aplikasi pabrikan yang sering mengirim notifikasi iklan"),
        DebloatPackage("com.miui.daemon", "MIUI Daemon", "Xiaomi", DebloatRisk.SAFE, "Layanan latar belakang pelacak aktivitas"),
        DebloatPackage("com.miui.hybrid", "Quick Apps", "Xiaomi", DebloatRisk.SAFE, "Platform aplikasi instan penyedia iklan"),
        DebloatPackage("com.miui.hybrid.accessory", "Quick Apps Accessory", "Xiaomi", DebloatRisk.SAFE, "Komponen pendukung Quick Apps"),
        DebloatPackage("com.mi.globalbrowser", "Mi Browser", "Xiaomi", DebloatRisk.ADVANCED, "Peramban bawaan dengan feed berita dan iklan"),
        DebloatPackage("com.miui.videoplayer", "Mi Video", "Xiaomi", DebloatRisk.ADVANCED, "Pemutar video yang menyuntikkan video rekomendasi online"),
        DebloatPackage("com.miui.player", "Mi Music", "Xiaomi", DebloatRisk.ADVANCED, "Aplikasi musik dengan promosi streaming")
    )

    private val samsungRules = listOf(
        DebloatPackage("com.facebook.katana", "Facebook App", "Samsung", DebloatRisk.SAFE, "Aplikasi Facebook pre-installed"),
        DebloatPackage("com.facebook.system", "Facebook App Installer", "Samsung", DebloatRisk.SAFE, "Layanan latar belakang pemasang Facebook"),
        DebloatPackage("com.facebook.appmanager", "Facebook App Manager", "Samsung", DebloatRisk.SAFE, "Pengelola pembaruan Facebook"),
        DebloatPackage("com.facebook.services", "Facebook Services", "Samsung", DebloatRisk.SAFE, "Layanan sinkronisasi Facebook tersembunyi"),
        DebloatPackage("com.samsung.android.bixby.agent", "Bixby Voice Agent", "Samsung", DebloatRisk.ADVANCED, "Asisten suara Bixby memakan RAM dan baterai"),
        DebloatPackage("com.samsung.android.bixby.service", "Bixby Service", "Samsung", DebloatRisk.ADVANCED, "Layanan inti Bixby"),
        DebloatPackage("com.samsung.android.app.spage", "Bixby Home / Samsung Free", "Samsung", DebloatRisk.SAFE, "Halaman berita dan iklan paling kiri di homescreen"),
        DebloatPackage("com.samsung.android.ipsgeofence", "Samsung Visit In", "Samsung", DebloatRisk.SAFE, "Pelacak lokasi toko untuk iklan promosi ritel"),
        DebloatPackage("com.samsung.android.game.gamehome", "Game Launcher", "Samsung", DebloatRisk.ADVANCED, "Pusat game bawaan dengan promosi berkala")
    )

    private val oppoRealmeRules = listOf(
        DebloatPackage("com.heytap.market", "App Market", "Oppo/Realme", DebloatRisk.SAFE, "Toko aplikasi HeyTap penyedia notifikasi rekomendasi"),
        DebloatPackage("com.heytap.browser", "HeyTap Browser", "Oppo/Realme", DebloatRisk.ADVANCED, "Browser bawaan dengan feed berita promosi"),
        DebloatPackage("com.heytap.mcs", "HeyTap Cloud Services", "Oppo/Realme", DebloatRisk.ADVANCED, "Layanan sinkronisasi cloud akun HeyTap"),
        DebloatPackage("com.oppo.market", "OPPO App Market", "Oppo/Realme", DebloatRisk.SAFE, "Toko aplikasi OEM"),
        DebloatPackage("com.coloros.gamespace", "Game Space Ads", "Oppo/Realme", DebloatRisk.ADVANCED, "Optimalisasi game dengan integrasi iklan partner")
    )

    private val vivoRules = listOf(
        DebloatPackage("com.vivo.appstore", "V-Appstore", "Vivo", DebloatRisk.SAFE, "Toko aplikasi Vivo dengan notifikasi promosi"),
        DebloatPackage("com.vivo.browser", "Vivo Browser", "Vivo", DebloatRisk.ADVANCED, "Peramban bawaan dengan feed berita online"),
        DebloatPackage("com.bbk.theme", "Vivo iTheme", "Vivo", DebloatRisk.ADVANCED, "Toko tema dengan katalog iklan berbayar"),
        DebloatPackage("com.vivo.tips", "Vivo Tips", "Vivo", DebloatRisk.SAFE, "Panduan penggunaan yang memicu pop-up")
    )

    private val transsionRules = listOf(
        DebloatPackage("com.transsion.phonemaster", "Phone Master Adware", "Transsion", DebloatRisk.SAFE, "Pembersih bawaan Infinix/Tecno yang memunculkan iklan lockscreen"),
        DebloatPackage("com.transsion.palmsore", "Palm Store", "Transsion", DebloatRisk.SAFE, "Toko aplikasi Transsion pengirim notifikasi iklan massal"),
        DebloatPackage("com.ahagame.center", "Aha Games", "Transsion", DebloatRisk.SAFE, "Pusat game promosi bawaan"),
        DebloatPackage("com.transsion.carlcare", "Carlcare Hub", "Transsion", DebloatRisk.ADVANCED, "Pusat layanan purna jual dengan banner iklan")
    )

    private val googleRules = listOf(
        DebloatPackage("com.google.android.apps.tachyon", "Google Meet / Duo", "Google", DebloatRisk.ADVANCED, "Aplikasi panggilan video bawaan"),
        DebloatPackage("com.google.android.videos", "Google TV", "Google", DebloatRisk.ADVANCED, "Aplikasi rental film Google"),
        DebloatPackage("com.google.android.apps.youtube.music", "YouTube Music", "Google", DebloatRisk.ADVANCED, "Layanan musik bawaan pre-installed")
    )

    private val motorolaRules = listOf(
        DebloatPackage("com.motorola.ccc.notification", "Motorola Notifications", "Motorola", DebloatRisk.SAFE, "Notifikasi survei dan promosi"),
        DebloatPackage("com.motorola.genie", "Moto Help / Tips", "Motorola", DebloatRisk.ADVANCED, "Aplikasi panduan perangkat bawaan")
    )

    private val universalAdwareBloat = listOf(
        DebloatPackage("com.opera.browser", "Opera Pre-installed", "Partner", DebloatRisk.SAFE, "Peramban pihak ketiga yang sering disuntikkan OEM"),
        DebloatPackage("com.aura.oobe.samsung", "IronSource Aura Out-Of-Box", "Ad-Network", DebloatRisk.SAFE, "Penyedia pemasangan otomatis APK bersponsor"),
        DebloatPackage("com.ironsource.aura.unbranded", "Aura Discovery", "Ad-Network", DebloatRisk.SAFE, "Penyedia iklan dan pengunduh background tak dikenal")
    )
}
