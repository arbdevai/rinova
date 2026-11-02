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
                currentStepText = "Membaca tabel partisi sistem..."
            )
            onProgressUpdate("Membaca tabel partisi sistem...", 0.1f)

            // 1. Baca df -k /data
            val dfResp = adbManager.execute("df -k /data")
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
                currentStepText = "Memindai statistik cache aplikasi..."
            )
            onProgressUpdate("Memindai statistik cache aplikasi...", 0.3f)

            var appCacheBytes = 0L
            val diskStatsResp = adbManager.execute("dumpsys diskstats")
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
                currentStepText = "Memeriksa sisa OBB dan folder aplikasi terhapus..."
            )
            onProgressUpdate("Memeriksa sisa OBB dan folder aplikasi terhapus...", 0.5f)

            val installedPackages = mutableSetOf<String>()
            val pkgListResp = adbManager.execute("pm list packages")
            pkgListResp.getOrNull()?.output?.let { out ->
                for (line in out.lineSequence()) {
                    val pkg = line.removePrefix("package:").trim()
                    if (pkg.isNotEmpty()) installedPackages.add(pkg)
                }
            }

            val orphanedList = mutableListOf<OrphanedDirectory>()
            var orphanedTotalBytes = 0L

            suspend fun inspectFolders(basePath: String) {
                val listResp = adbManager.execute("ls -1 $basePath")
                val folders = listResp.getOrNull()?.output?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                for (folder in folders) {
                    if (folder.contains(".") && !installedPackages.contains(folder)) {
                        // Aplikasi ini tidak terpasang! Cek ukurannya
                        val fullPath = "$basePath/$folder"
                        val duResp = adbManager.execute("du -sk $fullPath")
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
            }

            inspectFolders("/sdcard/Android/data")
            inspectFolders("/sdcard/Android/obb")

            // 4. Periksa crash logs / tombstones / anr
            _storageState.value = _storageState.value.copy(
                currentStepText = "Memindai file crash dump sistem..."
            )
            onProgressUpdate("Memindai file crash dump sistem...", 0.8f)

            var crashDumpsBytes = 0L
            val tombResp = adbManager.execute("du -sk /data/tombstones 2>/dev/null")
            tombResp.getOrNull()?.output?.let { out ->
                val kb = out.trim().split("\\s+".toRegex()).firstOrNull()?.toLongOrNull() ?: 0L
                crashDumpsBytes += (kb * 1024L)
            }
            val anrResp = adbManager.execute("du -sk /data/anr 2>/dev/null")
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
                currentStepText = "Analisis selesai.",
                lastScanTimestamp = System.currentTimeMillis()
            )

            _storageState.value = resultBreakdown
            Result.success(resultBreakdown)
        } catch (e: Exception) {
            _storageState.value = _storageState.value.copy(
                isAnalyzing = false,
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
