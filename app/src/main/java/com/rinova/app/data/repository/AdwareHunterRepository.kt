package com.rinova.app.data.repository

import com.rinova.app.core.adb.AdbConnectionManager
import com.rinova.app.core.model.RogueAppItem
import com.rinova.app.core.model.ThreatSeverity
import com.rinova.app.data.debloat.SafeWhitelist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AdwareHunterRepository(
    private val adbManager: AdbConnectionManager
) {
    private val _scannedThreats = MutableStateFlow<List<RogueAppItem>>(emptyList())
    val scannedThreats: StateFlow<List<RogueAppItem>> = _scannedThreats.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _currentScanningStep = MutableStateFlow("")
    val currentScanningStep: StateFlow<String> = _currentScanningStep.asStateFlow()

    private val _scanLogs = MutableStateFlow<List<String>>(emptyList())
    val scanLogs: StateFlow<List<String>> = _scanLogs.asStateFlow()

    private fun log(line: String) {
        _scanLogs.value = _scanLogs.value + line
    }

    suspend fun captureTopActiveWindow(): Result<String> = withContext(Dispatchers.IO) {
        val resp = adbManager.execute("dumpsys window | grep -E \"mCurrentFocus|mFocusedApp\"")
        val out = resp.getOrNull()?.output.orEmpty()
        val pkg = extractPackageFromWindowOutput(out)
        if (pkg.isNotEmpty()) {
            Result.success(pkg)
        } else {
            // Fallback ke dumpsys activity
            val actResp = adbManager.execute("dumpsys activity activities | grep -E \"mResumedActivity\"")
            val actOut = actResp.getOrNull()?.output.orEmpty()
            val fallbackPkg = extractPackageFromWindowOutput(actOut)
            if (fallbackPkg.isNotEmpty()) {
                Result.success(fallbackPkg)
            } else {
                Result.failure(NoSuchElementException("Tidak ada jendela aplikasi yang terdeteksi aktif di layar."))
            }
        }
    }

    suspend fun scanSuspiciousApps(): Result<List<RogueAppItem>> = withContext(Dispatchers.IO) {
        _isScanning.value = true
        _scanLogs.value = emptyList()
        _scanProgress.value = 0.05f
        _currentScanningStep.value = "Mengumpulkan daftar aplikasi pihak ketiga..."

        log("> adb shell pm list packages -3")
        try {
            // 1. Ambil seluruh paket 3rd party dan bersihkan secara ketat
            val listResp = adbManager.execute("pm list packages -3", 10000)
            val rawOutput = listResp.getOrNull()?.output.orEmpty()
            val packageRegex = Regex("^[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+$")
            val thirdPartyPackages = rawOutput.lines()
                .map { it.substringAfter("package:").trim() }
                .filter { it.isNotEmpty() && packageRegex.matches(it) }
                .distinct()

            log("[OK] Terdeteksi ${thirdPartyPackages.size} aplikasi pihak ketiga terpasang di target")

            if (thirdPartyPackages.isEmpty()) {
                _scanProgress.value = 1.0f
                _currentScanningStep.value = "Tidak ada aplikasi pihak ketiga tambahan yang terdeteksi."
                _scannedThreats.value = emptyList()
                log("[DONE] Tidak ada aplikasi pihak ketiga yang perlu dianalisis.")
                return@withContext Result.success(emptyList())
            }

            // 2. Ambil paket yang memiliki Launcher Icon (Batched)
            _scanProgress.value = 0.25f
            _currentScanningStep.value = "Memeriksa ikon launcher dan aktivitas utama..."
            log("> adb shell cmd package query-intent-activities -a android.intent.action.MAIN -c android.intent.category.LAUNCHER")
            val launcherResp = adbManager.execute("cmd package query-intent-activities -a android.intent.action.MAIN -c android.intent.category.LAUNCHER", 10000)
            val launcherOutput = launcherResp.getOrNull()?.output.orEmpty()
            val launcherPackages = mutableSetOf<String>()
            for (line in launcherOutput.lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("packageName=")) {
                    val pkg = trimmed.substringAfter("packageName=").trim()
                    if (packageRegex.matches(pkg)) {
                        launcherPackages.add(pkg)
                    }
                }
            }
            log("[OK] ${launcherPackages.size} aplikasi memiliki ikon launcher visual")

            // 3. Ambil paket dengan izin Overlay (SYSTEM_ALERT_WINDOW) secara instan (Batched - Anti-Hang!)
            _scanProgress.value = 0.50f
            _currentScanningStep.value = "Menganalisis izin overlay jendela (SYSTEM_ALERT_WINDOW)..."
            log("> adb shell cmd appops query-op SYSTEM_ALERT_WINDOW allow")
            val overlayResp = adbManager.execute("cmd appops query-op SYSTEM_ALERT_WINDOW allow", 8000)
            val overlayOutput = overlayResp.getOrNull()?.output.orEmpty()
            val overlayPackages = mutableSetOf<String>()
            for (line in overlayOutput.lineSequence()) {
                val clean = line.substringAfter("package:").trim()
                val pkg = clean.substringBefore(" ").substringBefore(":").trim()
                if (packageRegex.matches(pkg)) {
                    overlayPackages.add(pkg)
                }
            }
            log("[OK] ${overlayPackages.size} aplikasi memiliki izin overlay jendela aktif")

            // 4. Ambil paket dengan boot receiver (BOOT_COMPLETED) secara instan (Batched)
            _scanProgress.value = 0.70f
            _currentScanningStep.value = "Menganalisis startup otomatis (BOOT_COMPLETED)..."
            log("> adb shell cmd package query-receivers -a android.intent.action.BOOT_COMPLETED")
            val bootResp = adbManager.execute("cmd package query-receivers -a android.intent.action.BOOT_COMPLETED", 8000)
            val bootOutput = bootResp.getOrNull()?.output.orEmpty()
            val bootPackages = mutableSetOf<String>()
            for (line in bootOutput.lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("packageName=")) {
                    val pkg = trimmed.substringAfter("packageName=").trim()
                    if (packageRegex.matches(pkg)) {
                        bootPackages.add(pkg)
                    }
                }
            }
            log("[OK] ${bootPackages.size} aplikasi terdaftar berjalan saat boot perangkat")

            // 5. Ambil layanan aksesibilitas aktif
            _scanProgress.value = 0.85f
            _currentScanningStep.value = "Memeriksa izin aksesibilitas aktif sistem..."
            log("> adb shell settings get secure enabled_accessibility_services")
            val accessResp = adbManager.execute("settings get secure enabled_accessibility_services", 5000)
            val accessibilityServices = accessResp.getOrNull()?.output.orEmpty()

            // 6. Evaluasi silang instan & aman
            _scanProgress.value = 0.90f
            _currentScanningStep.value = "Melakukan audit silang keamanan..."
            val threats = mutableListOf<RogueAppItem>()

            for (pkg in thirdPartyPackages) {
                if (SafeWhitelist.isCritical(pkg)) continue

                val hasLauncher = launcherPackages.contains(pkg)
                val hasOverlay = overlayPackages.contains(pkg)
                val hasBoot = bootPackages.contains(pkg)
                val hasAccessibility = accessibilityServices.contains(pkg)

                // Hitung severity berdasarkan bukti nyata empiris
                val reasons = mutableListOf<String>()
                var severity = ThreatSeverity.SAFE

                if (!hasLauncher) {
                    reasons.add("Tidak memiliki ikon launcher (Aplikasi Tersembunyi)")
                    severity = ThreatSeverity.HIGH
                }
                if (hasOverlay) {
                    reasons.add("Memiliki izin Overlay jendela (SYSTEM_ALERT_WINDOW)")
                    if (severity == ThreatSeverity.HIGH) severity = ThreatSeverity.CRITICAL else severity = ThreatSeverity.MEDIUM
                }
                if (hasAccessibility) {
                    reasons.add("Menjalankan Layanan Aksesibilitas di latar belakang")
                    severity = ThreatSeverity.CRITICAL
                }
                if (hasBoot && !hasLauncher) {
                    reasons.add("Berjalan otomatis saat booting tanpa antarmuka launcher")
                    severity = ThreatSeverity.CRITICAL
                }

                if (severity != ThreatSeverity.SAFE) {
                    log("[THREAT] $pkg ($severity): ${reasons.joinToString("; ")}")
                    threats.add(
                        RogueAppItem(
                            packageName = pkg,
                            appName = pkg.substringAfterLast("."),
                            isSystemApp = false,
                            hasLauncherIcon = hasLauncher,
                            hasOverlayPermission = hasOverlay,
                            hasBootCompletedPermission = hasBoot,
                            hasAccessibilityService = hasAccessibility,
                            severity = severity,
                            evidenceReason = reasons.joinToString("; ")
                        )
                    )
                } else {
                    log("[AUDIT] $pkg ... AMAN")
                }
            }

            _scanProgress.value = 1.0f
            _currentScanningStep.value = "Pemindaian selesai. Menemukan ${threats.size} potensi anomali/ancaman."
            log("[DONE] Audit keamanan selesai: ${threats.size} ancaman ditemukan.")
            _scannedThreats.value = threats
            Result.success(threats)
        } catch (e: Exception) {
            val errMsg = "Gagal memindai: ${e.localizedMessage}"
            log("[ERR] $errMsg")
            _currentScanningStep.value = errMsg
            Result.failure(e)
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun freezeApp(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        if (SafeWhitelist.isCritical(packageName)) {
            return@withContext Result.failure(IllegalStateException("Paket ini dilindungi whitelist sistem"))
        }
        adbManager.execute("am force-stop $packageName")
        adbManager.execute("pm disable-user --user 0 $packageName").map { it.allOutput }
    }

    suspend fun uninstallApp(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        if (SafeWhitelist.isCritical(packageName)) {
            return@withContext Result.failure(IllegalStateException("Paket ini dilindungi whitelist sistem"))
        }
        adbManager.execute("pm uninstall --user 0 $packageName").map { it.allOutput }
    }

    private fun extractPackageFromWindowOutput(output: String): String {
        val regex = Regex("""([a-zA-Z0-9_]+(?:\.[a-zA-Z0-9_]+)+)/""")
        val match = regex.find(output)
        return match?.groupValues?.get(1)?.trim().orEmpty()
    }
}
