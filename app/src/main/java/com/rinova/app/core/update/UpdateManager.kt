package com.rinova.app.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.rinova.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubAsset(
    val name: String = "",
    val browser_download_url: String = "",
    val size: Long = 0L,
    val content_type: String = ""
)

@Serializable
data class GitHubRelease(
    val tag_name: String = "",
    val name: String = "",
    val body: String = "",
    val assets: List<GitHubAsset> = emptyList()
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpdateAvailable(
        val newVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val fileSizeBytes: Long
    ) : UpdateState
    data class Downloading(val progressPercent: Int) : UpdateState
    data class ReadyToInstall(val apkFile: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

class UpdateManager(
    private val context: Context,
    private val repoOwner: String = "arbdevai",
    private val repoName: String = "rinova"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    suspend fun checkForUpdates(): Result<UpdateState> = withContext(Dispatchers.IO) {
        _updateState.value = UpdateState.Checking
        val url = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/vnd.github.v3+json")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val rawBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val err = "Gagal memeriksa pembaruan (HTTP ${response.code})"
                _updateState.value = UpdateState.Error(err)
                return@withContext Result.failure(IOException(err))
            }

            val release = json.decodeFromString(GitHubRelease.serializer(), rawBody)
            val remoteVersion = release.tag_name.removePrefix("v").trim()
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()

            if (isNewerVersion(remoteVersion, currentVersion)) {
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                    ?: release.assets.firstOrNull()

                if (apkAsset != null) {
                    val state = UpdateState.UpdateAvailable(
                        newVersion = release.tag_name,
                        releaseNotes = release.body,
                        downloadUrl = apkAsset.browser_download_url,
                        fileSizeBytes = apkAsset.size
                    )
                    _updateState.value = state
                    Result.success(state)
                } else {
                    _updateState.value = UpdateState.Idle
                    Result.success(UpdateState.Idle)
                }
            } else {
                _updateState.value = UpdateState.Idle
                Result.success(UpdateState.Idle)
            }
        } catch (e: Exception) {
            val err = "Koneksi ke server pembaruan terputus: ${e.localizedMessage}"
            _updateState.value = UpdateState.Error(err)
            Result.failure(e)
        }
    }

    suspend fun downloadUpdate(
        downloadUrl: String,
        version: String,
        expectedBytes: Long = 0L
    ): Result<File> = withContext(Dispatchers.IO) {
        val downloadDir = context.getExternalFilesDir("updates") ?: context.filesDir
        cleanupOldApks(downloadDir)

        val tempPartFile = File(downloadDir, "app-$version.apk.part")
        val finalApkFile = File(downloadDir, "app-$version.apk")

        if (tempPartFile.exists()) tempPartFile.delete()

        _updateState.value = UpdateState.Downloading(0)

        val request = Request.Builder()
            .url(downloadUrl)
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                tempPartFile.delete()
                val err = "Gagal mengunduh APK (HTTP ${response.code})"
                _updateState.value = UpdateState.Error(err)
                return@withContext Result.failure(IOException(err))
            }

            val responseBody = response.body ?: throw IOException("Respons unduhan kosong")
            val totalBytes = responseBody.contentLength().let { if (it > 0) it else expectedBytes }
            val inputStream = responseBody.byteStream()
            val outputStream = FileOutputStream(tempPartFile)

            val buffer = ByteArray(64 * 1024)
            var downloadedBytes = 0L
            var read: Int

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                downloadedBytes += read
                if (totalBytes > 0L) {
                    val progress = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 99)
                    _updateState.value = UpdateState.Downloading(progress)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Verifikasi 4 Lapis sebelum rename
            if (isApkValid(context, tempPartFile, totalBytes)) {
                if (finalApkFile.exists()) finalApkFile.delete()
                val renamed = tempPartFile.renameTo(finalApkFile)
                if (renamed) {
                    _updateState.value = UpdateState.ReadyToInstall(finalApkFile)
                    Result.success(finalApkFile)
                } else {
                    tempPartFile.delete()
                    val err = "Gagal memfinalisasi file instalasi."
                    _updateState.value = UpdateState.Error(err)
                    Result.failure(IOException(err))
                }
            } else {
                tempPartFile.delete()
                val err = "Verifikasi integritas paket APK gagal (File tidak lengkap atau korup)."
                _updateState.value = UpdateState.Error(err)
                Result.failure(IOException(err))
            }
        } catch (e: Exception) {
            if (tempPartFile.exists()) tempPartFile.delete()
            val err = "Proses unduh terputus: ${e.localizedMessage}"
            _updateState.value = UpdateState.Error(err)
            Result.failure(e)
        }
    }

    fun installApk(apkFile: File) {
        if (!apkFile.exists() || apkFile.length() < 1_000_000) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(settingsIntent)
            return
        }

        val authority = "${context.packageName}.provider"
        val contentUri = FileProvider.getUriForFile(context, authority, apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(installIntent)
    }

    private fun isApkValid(context: Context, apkFile: File, expectedBytes: Long = 0L): Boolean {
        if (!apkFile.exists() || !apkFile.isFile || apkFile.length() < 1_000_000) return false
        if (expectedBytes > 0L && Math.abs(apkFile.length() - expectedBytes) > 4096L) return false

        return try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_ACTIVITIES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES or PackageManager.GET_ACTIVITIES
            }
            val pkg = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
            pkg.packageName == context.packageName
        } catch (_: Exception) {
            false
        }
    }

    private fun cleanupOldApks(directory: File) {
        try {
            directory.listFiles()?.forEach { file ->
                if (file.name.endsWith(".apk") || file.name.endsWith(".part")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val rParts = remote.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val lParts = local.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }

        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
