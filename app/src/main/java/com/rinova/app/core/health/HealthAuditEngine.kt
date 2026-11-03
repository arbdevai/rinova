package com.rinova.app.core.health

import com.rinova.app.core.model.DeviceInfo
import com.rinova.app.data.repository.AdbRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HealthAuditEngine(
    private val adbRepository: AdbRepository
) {

    suspend fun runSmartHealthAudit(
        device: DeviceInfo,
        onLog: (String) -> Unit = {}
    ): HealthScanResult = withContext(Dispatchers.IO) {
        val issues = mutableListOf<HealthIssue>()
        var safeCount = 0

        val brandLower = device.brand.lowercase()
        val manufacturerLower = device.manufacturer.lowercase()
        val isXiaomi = brandLower.contains("xiaomi") || brandLower.contains("redmi") || brandLower.contains("poco")
        val isSamsung = brandLower.contains("samsung") || manufacturerLower.contains("samsung")

        onLog("> Rinova Health Diagnostics Engine diinisialisasi...")
        onLog("[INFO] Target: ${device.brand} ${device.model} (Android ${device.androidVersion}, SDK ${device.sdkInt})")

        // 1. Audit Penyimpanan (Storage Cache & Partition Health)
        onLog("> adb shell df -k /data")
        val dfResult = adbRepository.executeCommand("df -k /data").getOrNull()?.output.orEmpty()
        var dataUsagePercent = 0
        dfResult.lines().forEach { line ->
            val parts = line.trim().split(Regex("""\s+"""))
            if (parts.size >= 5 && parts.last().contains("/data")) {
                val pStr = parts[parts.size - 2].replace("%", "")
                dataUsagePercent = pStr.toIntOrNull() ?: 0
            }
        }
        onLog("[OK] Partisi /data terpakai: $dataUsagePercent%")

        if (dataUsagePercent > 85) {
            issues.add(
                HealthIssue(
                    id = "storage_partition_full",
                    category = HealthCategory.STORAGE,
                    title = "Kapasitas Partisi /data Mendekati Penuh ($dataUsagePercent%)",
                    humanDescription = "Memori internal sistem hampir penuh. Menyebabkan penurunan kecepatan baca/tulis aplikasi dan risiko bootloop jika ruang habis total.",
                    technicalCommand = "pm trim-caches 9999999999999",
                    severity = HealthSeverity.CRITICAL,
                    potentialSavings = "~1 - 3 GB"
                )
            )
        } else {
            safeCount++
        }

        // Cache Sistem & Thumbnail Gallery
        if (isXiaomi) {
            issues.add(
                HealthIssue(
                    id = "xiaomi_gallery_cache",
                    category = HealthCategory.STORAGE,
                    title = "File Thumbnail Galeri Xiaomi Menumpuk",
                    humanDescription = "Aplikasi Galeri MIUI/HyperOS membuat salinan thumbnail tersembunyi berukuran gigabyte yang tidak dihapus otomatis. 100% aman dibersihkan tanpa menghapus foto asli.",
                    technicalCommand = "rm -rf /storage/emulated/0/Android/data/com.miui.gallery/files/ThumbnailFile/* /storage/emulated/0/Android/data/com.miui.gallery/files/gallery_disk_cache/*",
                    severity = HealthSeverity.WARNING,
                    potentialSavings = "~1.5 GB"
                )
            )
        } else {
            issues.add(
                HealthIssue(
                    id = "general_system_cache",
                    category = HealthCategory.STORAGE,
                    title = "Pangkas Cache Sementara Aplikasi",
                    humanDescription = "Membersihkan berkas sementara sistem dan cache sisa aplikasi. Data login dan dokumen pribadi tetap aman dan tidak terhapus.",
                    technicalCommand = "pm trim-caches 9999999999999",
                    severity = HealthSeverity.INFO,
                    potentialSavings = "~800 MB"
                )
            )
        }

        // 2. Audit Adware & Bloatware Pabrikan
        onLog("> adb shell pm list packages")
        val installedPackagesRaw = adbRepository.executeCommand("pm list packages").getOrNull()?.output.orEmpty()
        val installedPackages = installedPackagesRaw.lines()
            .map { it.substringAfter("package:").trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        onLog("[OK] Terbaca ${installedPackages.size} total paket di sistem")

        if (isSamsung) {
            if (installedPackages.contains("com.aura.oobe.samsung.gl")) {
                issues.add(
                    HealthIssue(
                        id = "adware_iron_source_aura",
                        category = HealthCategory.ADWARE,
                        title = "Adware Sponsor IronSource Aura Terdeteksi",
                        humanDescription = "Aplikasi tersembunyi yang otomatis memasang game/aplikasi sponsor tanpa izin dan mengonsumsi ~90MB RAM di latar belakang.",
                        technicalCommand = "pm uninstall -k --user 0 com.aura.oobe.samsung.gl",
                        severity = HealthSeverity.CRITICAL,
                        potentialSavings = "~90 MB RAM"
                    )
                )
            } else {
                safeCount++
            }

            if (installedPackages.contains("com.samsung.android.dynamiclock")) {
                issues.add(
                    HealthIssue(
                        id = "adware_glance_lockscreen",
                        category = HealthCategory.ADWARE,
                        title = "Wallpaper Promosi Glance Lockscreen Aktif",
                        humanDescription = "Layanan berita dan iklan pada layar kunci yang mengunduh gambar di latar belakang dan menguras kuota internet.",
                        technicalCommand = "pm uninstall -k --user 0 com.samsung.android.dynamiclock",
                        severity = HealthSeverity.WARNING,
                        potentialSavings = "Hemat Kuota & Baterai"
                    )
                )
            } else {
                safeCount++
            }

            if (installedPackages.contains("com.samsung.android.game.gos")) {
                issues.add(
                    HealthIssue(
                        id = "perf_samsung_gos",
                        category = HealthCategory.PERFORMANCE,
                        title = "Pembatas Performa Game (Samsung GOS) Aktif",
                        humanDescription = "Layanan GOS sering kali menurunkan FPS dan membatasi kinerja chipset secara berlebihan saat bermain game atau menjalankan aplikasi berat.",
                        technicalCommand = "pm disable-user --user 0 com.samsung.android.game.gos",
                        severity = HealthSeverity.INFO,
                        potentialSavings = "Stabilitas FPS Game"
                    )
                )
            }
        }

        if (isXiaomi) {
            if (installedPackages.contains("com.miui.msa.global")) {
                issues.add(
                    HealthIssue(
                        id = "adware_miui_msa",
                        category = HealthCategory.ADWARE,
                        title = "Layanan Iklan Sistem MIUI (MSA) Berjalan",
                        humanDescription = "Penyedia iklan utama pada aplikasi File Manager, Tema, dan Keamanan Xiaomi. Menghilangkan iklan popup tanpa merusak fungsi sistem.",
                        technicalCommand = "pm uninstall -k --user 0 com.miui.msa.global",
                        severity = HealthSeverity.CRITICAL,
                        potentialSavings = "~60 MB RAM"
                    )
                )
            } else {
                safeCount++
            }

            if (installedPackages.contains("com.xiaomi.joyose")) {
                issues.add(
                    HealthIssue(
                        id = "perf_xiaomi_joyose",
                        category = HealthCategory.PERFORMANCE,
                        title = "Joyose Throttler Terdeteksi",
                        humanDescription = "Layanan pengumpul analitik yang membatasi refresh rate layar dan throttling performa CPU saat suhu ponsel sedikit hangat.",
                        technicalCommand = "pm disable-user --user 0 com.xiaomi.joyose",
                        severity = HealthSeverity.WARNING,
                        potentialSavings = "Performa Maksimal"
                    )
                )
            }
        }

        // 3. Audit RAM Plus / Memory Extension
        if (isSamsung) {
            val ramPlusVal = adbRepository.executeCommand("settings get global ram_expand_size").getOrNull()?.output.orEmpty().trim()
            if (ramPlusVal.isNotEmpty() && ramPlusVal != "0" && ramPlusVal != "null") {
                issues.add(
                    HealthIssue(
                        id = "perf_samsung_ram_plus",
                        category = HealthCategory.PERFORMANCE,
                        title = "RAM Plus Aktif ($ramPlusVal MB Virtual)",
                        humanDescription = "RAM virtual menulis memori ke chip flash eMMC/UFS. Menyebabkan lag/stuttering saat membuka aplikasi dan mempercepat keausan chip penyimpanan.",
                        technicalCommand = "settings put global ram_expand_size 0",
                        severity = HealthSeverity.WARNING,
                        potentialSavings = "Respon HP Lebih Cepat"
                    )
                )
            } else {
                safeCount++
            }
        }

        // 4. Audit Keamanan Kernel & Aksesibilitas
        val selinuxOut = adbRepository.executeCommand("getenforce").getOrNull()?.output.orEmpty().trim()
        if (selinuxOut.equals("Enforcing", ignoreCase = true)) {
            safeCount++
        } else if (selinuxOut.equals("Permissive", ignoreCase = true)) {
            issues.add(
                HealthIssue(
                    id = "sec_selinux_permissive",
                    category = HealthCategory.SECURITY,
                    title = "Pertahanan Kernel SELinux Permissive",
                    humanDescription = "Kernel berada dalam mode permissive. Batasan keamanan Android ditiadakan, perangkat rentan terhadap modifikasi aplikasi liar.",
                    technicalCommand = "setenforce 1",
                    severity = HealthSeverity.CRITICAL,
                    potentialSavings = "Keamanan Kernel Aktif"
                )
            )
        }

        val accessibilityOut = adbRepository.executeCommand("settings get secure enabled_accessibility_services").getOrNull()?.output.orEmpty().trim()
        if (accessibilityOut.isNotEmpty() && accessibilityOut != "null") {
            val services = accessibilityOut.split(":")
            val suspicious = services.filter { !it.contains("google") && !it.contains("android") && !it.contains("samsung") && !it.contains("miui") }
            if (suspicious.isNotEmpty()) {
                issues.add(
                    HealthIssue(
                        id = "sec_accessibility_audit",
                        category = HealthCategory.SECURITY,
                        title = "Layanan Aksesibilitas Mencurigakan Aktif",
                        humanDescription = "Ditemukan aplikasi non-sistem yang memiliki hak akses membaca layar dan input sentuhan. Berpotensi sebagai penyadap data atau malware.",
                        technicalCommand = "settings put secure enabled_accessibility_services ''",
                        severity = HealthSeverity.CRITICAL,
                        potentialSavings = "Privasi Layar Terlindungi"
                    )
                )
            } else {
                safeCount++
            }
        } else {
            safeCount++
        }

        val initialAdware = issues.count { it.category == HealthCategory.ADWARE }
        val initialCache = if (isXiaomi) "~1.5 GB" else "~800 MB"
        val ramIssue = issues.find { it.category == HealthCategory.PERFORMANCE }
        val initialRam = if (ramIssue != null) "Terbebani (${ramIssue.title})" else "Stabil"

        onLog("[DONE] Audit kesehatan selesai. Ditemukan ${issues.size} rekomendasi optimasi.")

        val res = HealthScanResult(
            totalIssuesFound = issues.size,
            safeItemsCount = safeCount,
            issues = issues,
            initialCacheEstimate = initialCache,
            initialAdwareCount = initialAdware,
            initialRamStatus = initialRam,
            targetRamStatus = "Optimal & Responsif"
        )
        res.copy(initialScore = res.currentScore)
    }

    suspend fun executeFix(issue: HealthIssue): Result<String> = withContext(Dispatchers.IO) {
        val cmd = issue.technicalCommand.trim()
        if (cmd.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Perintah tidak valid"))
        adbRepository.executeCommand(cmd).map { it.allOutput }
    }
}
