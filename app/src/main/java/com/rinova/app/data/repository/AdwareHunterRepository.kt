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
        _scanProgress.value = 0.05f
        _currentScanningStep.value = "Mengumpulkan daftar aplikasi pihak ketiga..."
        try {
            // 1. Ambil seluruh paket 3rd party dan bersihkan secara ketat
            val listResp = adbManager.execute("pm list packages -3", 10000)
            val rawOutput = listResp.getOrNull()?.output.orEmpty()
            val packageRegex = Regex("^[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+$")
            val thirdPartyPackages = rawOutput.lines()
                .map { it.substringAfter("package:").trim() }
                .filter { it.isNotEmpty() && packageRegex.matches(it) }
                .distinct()

            // 2. Ambil paket yang memiliki Launcher Icon
            _scanProgress.value = 0.15f
            _currentScanningStep.value = "Memeriksa ikon launcher dan aktivitas utama..."
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

            // 3. Ambil layanan aksesibilitas aktif
            _scanProgress.value = 0.25f
            _currentScanningStep.value = "Memeriksa izin aksesibilitas aktif sistem..."
            val accessResp = adbManager.execute("settings get secure enabled_accessibility_services", 5000)
            val accessibilityServices = accessResp.getOrNull()?.output.orEmpty()

            val threats = mutableListOf<RogueAppItem>()
            val totalApps = thirdPartyPackages.size

            if (totalApps == 0) {
                _scanProgress.value = 1.0f
                _currentScanningStep.value = "Tidak ada aplikasi pihak ketiga tambahan yang terdeteksi."
                _scannedThreats.value = emptyList()
                return@withContext Result.success(emptyList())
            }

            for ((index, pkg) in thirdPartyPackages.withIndex()) {
                if (SafeWhitelist.isCritical(pkg)) continue

                val currentProgress = 0.25f + (((index + 1).toFloat() / totalApps) * 0.70f)
                _scanProgress.value = currentProgress
                _currentScanningStep.value = "Memeriksa [$pkg] (${index + 1}/$totalApps)\nAnalisis izin overlay jendela (SYSTEM_ALERT_WINDOW)..."

                val hasLauncher = launcherPackages.contains(pkg)

                // Cek SYSTEM_ALERT_WINDOW via appops dengan timeout aman
                val appOpsResp = adbManager.execute("cmd appops get $pkg SYSTEM_ALERT_WINDOW", 4000)
                val appOpsOut = appOpsResp.getOrNull()?.output.orEmpty()
                val hasOverlay = appOpsOut.contains("allow", ignoreCase = true)

                // Cek RECEIVE_BOOT_COMPLETED dengan timeout aman
                val dumpPkgResp = adbManager.execute("dumpsys package $pkg | grep -E \"android.permission.RECEIVE_BOOT_COMPLETED\"", 4000)
                val hasBoot = dumpPkgResp.getOrNull()?.output?.contains("RECEIVE_BOOT_COMPLETED") == true

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
                }
            }

            _scanProgress.value = 1.0f
            _currentScanningStep.value = "Pemindaian selesai. Menemukan ${threats.size} potensi anomali/ancaman."
            _scannedThreats.value = threats
            Result.success(threats)
        } catch (e: Exception) {
            _currentScanningStep.value = "Gagal memindai: ${e.localizedMessage}"
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
