package com.rinova.app.data.repository

import com.rinova.app.core.adb.AdbConnectionManager
import com.rinova.app.core.model.OrphanedDirectory
import com.rinova.app.core.model.StorageBreakdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class StorageRepository(
    private val adbManager: AdbConnectionManager
) {
    private val _storageState = MutableStateFlow(StorageBreakdown())
    val storageState: StateFlow<StorageBreakdown> = _storageState.asStateFlow()

    suspend fun analyzeStorage(
        onProgressUpdate: (step: String, percent: Float) -> Unit = { _, _ -> }
    ): Result<StorageBreakdown> = withContext(Dispatchers.IO) {
        try {
            _storageState.value = _storageState.value.copy(
                isAnalyzing = true,
                scanProgress = 0.08f,
                currentStepText = "Membaca kapasitas dan partisi penyimpanan..."
            )
            onProgressUpdate("Membaca kapasitas dan partisi penyimpanan...", 0.08f)

            // 1. Baca df -k /data
            val dfResp = adbManager.execute("df -k /data", 5000)
            var totalBytes = 0L
            var usedBytes = 0L
            var freeBytes = 0L

            dfResp.getOrNull()?.output?.let { output ->
                val lines = output.trim().lines()
                if (lines.size >= 2) {
                    val tokens = lines[1].split("\\s+".toRegex()).filter { it.isNotEmpty() }
                    if (tokens.size >= 4) {
                        val totalKb = tokens[1].toLongOrNull() ?: 0L
                        val usedKb = tokens[2].toLongOrNull() ?: 0L
                        val freeKb = tokens[3].toLongOrNull() ?: 0L
                        totalBytes = totalKb * 1024L
                        usedBytes = usedKb * 1024L
                        freeBytes = freeKb * 1024L
                    }
                }
            }

            // 2. Analisis App Cache via dumpsys diskstats
            _storageState.value = _storageState.value.copy(
                scanProgress = 0.25f,
                currentStepText = "Menganalisis statistik partisi data & cache aplikasi..."
            )
            onProgressUpdate("Menganalisis statistik partisi data & cache aplikasi...", 0.25f)

            var appCacheBytes = 0L
            val diskStatsResp = adbManager.execute("dumpsys diskstats", 5000)
            diskStatsResp.getOrNull()?.output?.let { out ->
                for (line in out.lineSequence()) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("App Cache:")) {
                        val kb = trimmed.substringAfter("App Cache:").trim().substringBefore("K").trim().toLongOrNull() ?: 0L
                        appCacheBytes = kb * 1024L
                    }
                }
            }

            // 3. Scan folder di /sdcard/Android/data dan obb
            _storageState.value = _storageState.value.copy(
                scanProgress = 0.40f,
                currentStepText = "Mencocokkan daftar aplikasi terpasang di sistem..."
            )
            onProgressUpdate("Mencocokkan daftar aplikasi terpasang di sistem...", 0.40f)

            val installedPackages = mutableSetOf<String>()
            val pkgListResp = adbManager.execute("pm list packages", 10000)
            pkgListResp.getOrNull()?.output?.let { out ->
                for (line in out.lineSequence()) {
                    val pkg = line.substringAfter("package:").trim()
                    if (pkg.isNotEmpty()) installedPackages.add(pkg)
                }
            }

            val orphanedList = mutableListOf<OrphanedDirectory>()
            var orphanedTotalBytes = 0L

            suspend fun inspectFolders(basePath: String, startProg: Float, endProg: Float) {
                val listResp = adbManager.execute("ls -1 $basePath", 5000)
                val rawFolders = listResp.getOrNull()?.output?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                val candidateFolders = rawFolders.filter { folder ->
                    folder.contains(".") &&
                    !folder.startsWith("android") &&
                    !folder.startsWith("com.android") &&
                    !folder.startsWith("com.google.android") &&
                    !folder.startsWith("com.samsung") &&
                    !folder.startsWith("com.sec") &&
                    !folder.startsWith("com.miui") &&
                    !folder.startsWith("com.xiaomi") &&
                    !installedPackages.contains(folder)
                }

                if (candidateFolders.isEmpty()) {
                    val step = "Memeriksa $basePath: Tidak ada folder yatim."
                    _storageState.value = _storageState.value.copy(currentStepText = step, scanProgress = endProg)
                    onProgressUpdate(step, endProg)
                    return
                }

                for ((idx, folder) in candidateFolders.withIndex()) {
                    val frac = (idx + 1).toFloat() / candidateFolders.size
                    val prog = startProg + (frac * (endProg - startProg))
                    val step = "Memeriksa folder yatim: $folder (${idx + 1}/${candidateFolders.size})..."
                    _storageState.value = _storageState.value.copy(currentStepText = step, scanProgress = prog)
                    onProgressUpdate(step, prog)

                    val fullPath = "$basePath/$folder"
                    // Timeout 3 detik agar tidak macet di Scoped Storage
                    val duResp = adbManager.execute("du -sk \"$fullPath\" 2>/dev/null", 3000)
                    val duOut = duResp.getOrNull()?.output?.trim().orEmpty()
                    val kb = duOut.split("\\s+".toRegex()).firstOrNull()?.toLongOrNull() ?: 0L
                    if (kb > 1024L) { // Hanya catat jika > 1 MB
                        val bytes = kb * 1024L
                        orphanedTotalBytes += bytes
                        orphanedList.add(
                            OrphanedDirectory(
                                path = fullPath,
                                sizeBytes = bytes,
                                folderName = folder,
                                reason = "Folder sisa dari aplikasi yang sudah di-uninstall"
                            )
                        )
                    }
                }
            }

            inspectFolders("/sdcard/Android/data", 0.45f, 0.65f)
            inspectFolders("/sdcard/Android/obb", 0.65f, 0.80f)

            // 4. Periksa crash logs / tombstones / anr
            _storageState.value = _storageState.value.copy(
                scanProgress = 0.85f,
                currentStepText = "Memindai file crash dump sistem (tombstones & ANR)..."
            )
            onProgressUpdate("Memindai file crash dump sistem (tombstones & ANR)...", 0.85f)

            var crashDumpsBytes = 0L
            val tombResp = adbManager.execute("du -sk /data/tombstones 2>/dev/null", 3000)
            tombResp.getOrNull()?.output?.let { out ->
                val kb = out.trim().split("\\s+".toRegex()).firstOrNull()?.toLongOrNull() ?: 0L
                crashDumpsBytes += (kb * 1024L)
            }
            val anrResp = adbManager.execute("du -sk /data/anr 2>/dev/null", 3000)
            anrResp.getOrNull()?.output?.let { out ->
                val kb = out.trim().split("\\s+".toRegex()).firstOrNull()?.toLongOrNull() ?: 0L
                crashDumpsBytes += (kb * 1024L)
            }

            onProgressUpdate("Analisis penyimpanan selesai.", 1.0f)
            val resultBreakdown = StorageBreakdown(
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                freeBytes = freeBytes,
                appCacheBytes = appCacheBytes,
                orphanedObbBytes = orphanedTotalBytes,
                crashDumpsBytes = crashDumpsBytes,
                orphanedDirectories = orphanedList,
                isAnalyzing = false,
                scanProgress = 1.0f,
                currentStepText = "Analisis selesai.",
                lastScanTimestamp = System.currentTimeMillis()
            )

            _storageState.value = resultBreakdown
            Result.success(resultBreakdown)
        } catch (e: Exception) {
            _storageState.value = _storageState.value.copy(
                isAnalyzing = false,
                scanProgress = 0f,
                currentStepText = "Gagal memindai: ${e.localizedMessage}"
            )
            Result.failure(e)
        }
    }

    suspend fun trimSystemCaches(): Result<String> = withContext(Dispatchers.IO) {
        // Eksekusi pembersihan cache sistem aman
        adbManager.execute("pm trim-caches 9999999999999").map { it.allOutput }
    }

    suspend fun deleteOrphanedFolder(folderPath: String): Result<String> = withContext(Dispatchers.IO) {
        if (!folderPath.startsWith("/sdcard/Android/data/") && !folderPath.startsWith("/sdcard/Android/obb/")) {
            return@withContext Result.failure(IllegalArgumentException("Jalur direktori tidak diizinkan demi keselamatan data sistem."))
        }
        adbManager.execute("rm -rf \"$folderPath\"").map { it.allOutput }
    }
}
