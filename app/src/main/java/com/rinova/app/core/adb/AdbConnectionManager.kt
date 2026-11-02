package com.rinova.app.core.adb

import android.content.Context
import android.os.Build
import com.rinova.app.core.log.RinovaLogManager
import com.rinova.app.core.model.AdbConnectionState
import com.rinova.app.core.model.DeviceInfo
import com.rinova.app.core.util.VendorProfileDetector
import dadb.AdbShellResponse
import dadb.Dadb
import io.github.muntashirakon.adb.PairingConnectionCtx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class AdbConnectionManager(private val context: Context) {

    private val mutex = Mutex()
    private var activeDadb: Dadb? = null

    private val _connectionState = MutableStateFlow<AdbConnectionState>(AdbConnectionState.Disconnected)
    val connectionState: StateFlow<AdbConnectionState> = _connectionState.asStateFlow()

    suspend fun connect(host: String, port: Int): Result<DeviceInfo> = withContext(Dispatchers.IO) {
        mutex.withLock {
            RinovaLogManager.i("CONN", "Mencoba menghubungkan ADB ke $host:$port...")
            try {
                _connectionState.value = AdbConnectionState.Connecting(host, port)

                // 1. Validasi socket reachability
                val socketTest = Socket()
                try {
                    RinovaLogManager.d("CONN", "Uji socket reachability ke $host:$port (Timeout 4s)...")
                    socketTest.connect(InetSocketAddress(host, port), 4000)
                    socketTest.close()
                    RinovaLogManager.d("CONN", "Socket $host:$port terbuka dan dapat dijangkau.")
                } catch (e: Exception) {
                    val err = "Gagal menjangkau $host:$port: ${e.localizedMessage}. Pastikan HP target dan HP teknisi dalam satu WiFi / Hotspot dan Wireless Debugging aktif."
                    RinovaLogManager.e("CONN", err, e)
                    _connectionState.value = AdbConnectionState.Error(err)
                    return@withContext Result.failure(IOException(err, e))
                }

                _connectionState.value = AdbConnectionState.Authorizing(host, port)
                RinovaLogManager.i("CONN", "Menjalankan handshake otorisasi ADB ke $host:$port...")

                // 2. Muat kredensial keypair Rinova
                val keyBundle = RinovaAdbKeyStore.getOrGenerateKeyBundle(context)

                // 3. Inisialisasi Dadb handshake dengan keypair tersimpan
                activeDadb?.close()
                val dadb = Dadb.create(host, port, keyPair = keyBundle.dadbKeyPair)
                activeDadb = dadb

                // 4. Ambil data identitas perangkat target
                RinovaLogManager.d("CONN", "Mengambil identitas getprop dan hardware dari target...")
                val deviceInfo = fetchDeviceInfoInternal(dadb)

                _connectionState.value = AdbConnectionState.Connected(host, port, deviceInfo)
                RinovaLogManager.i(
                    "CONN",
                    "BERHASIL TERHUBUNG: ${deviceInfo.brand} ${deviceInfo.model} (Android ${deviceInfo.androidVersion}, SDK ${deviceInfo.sdkInt}) di $host:$port"
                )
                Result.success(deviceInfo)
            } catch (e: Exception) {
                activeDadb?.close()
                activeDadb = null
                val err = "Gagal otorisasi ADB ke $host:$port: ${e.localizedMessage ?: "Koneksi ditolak"}. Jika baru pertama kali, lakukan 'Pairing Nirkabel' terlebih dahulu atau periksa popup konfirmasi di HP target."
                RinovaLogManager.e("CONN", err, e)
                _connectionState.value = AdbConnectionState.Error(err)
                Result.failure(IOException(err, e))
            }
        }
    }

    suspend fun pair(host: String, port: Int, pairingCode: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanCode = pairingCode.trim()
        RinovaLogManager.i("PAIR", "Memulai proses pairing nirkabel ke $host:$port (Kode: $cleanCode)...")

        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            val err = "Kode pairing harus berupa 6 digit angka numerik."
            RinovaLogManager.w("PAIR", err)
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        // 1. Validasi jangkauan socket port pairing sementara
        val socketTest = Socket()
        try {
            RinovaLogManager.d("PAIR", "Uji socket port pairing $host:$port...")
            socketTest.connect(InetSocketAddress(host, port), 4000)
            socketTest.close()
            RinovaLogManager.i("PAIR", "Port pairing $host:$port merespons socket dengan baik.")
        } catch (e: Exception) {
            val err = "Gagal menjangkau port pairing $host:$port: ${e.localizedMessage}. Pastikan dialog 'Pasangkan dengan kode pairing' di HP target masih terbuka di layar!"
            RinovaLogManager.e("PAIR", err, e)
            return@withContext Result.failure(IOException(err, e))
        }

        // 2. Muat kredensial keypair & sertifikat Rinova
        val keyBundle = try {
            RinovaAdbKeyStore.getOrGenerateKeyBundle(context)
        } catch (e: Exception) {
            val err = "Gagal memuat kredensial kriptografi Rinova: ${e.localizedMessage}"
            RinovaLogManager.e("PAIR", err, e)
            return@withContext Result.failure(IOException(err, e))
        }

        // 3. TIER 1: Embedded In-App Engine (TLS 1.3 + SPAKE2 Protocol - 100% No Root)
        try {
            RinovaLogManager.i("PAIR", "Mengeksekusi pairing via Embedded Engine (TLS 1.3 / SPAKE2 - No Root)...")
            val deviceName = "Rinova Technician [${Build.MANUFACTURER} ${Build.MODEL}]"
            val pairingClient = PairingConnectionCtx(
                host,
                port,
                cleanCode.toByteArray(Charsets.UTF_8),
                keyBundle.javaKeyPair.private,
                keyBundle.certificate,
                deviceName
            )
            pairingClient.use { client ->
                client.start()
            }
            val successMsg = "Pairing nirkabel BERHASIL via Embedded Engine (TLS 1.3 / SPAKE2). Kunci Rinova berhasil didaftarkan di target!"
            RinovaLogManager.i("PAIR", successMsg)
            return@withContext Result.success(successMsg)
        } catch (e: Exception) {
            RinovaLogManager.w(
                "PAIR",
                "Embedded engine memberikan catatan (${e.javaClass.simpleName}: ${e.message}). Memeriksa opsi fallback..."
            )
        }

        // 4. TIER 2: Fallback ke Biner CLI Lokal jika tersedia
        val candidates = listOf(
            File(context.applicationInfo.nativeLibraryDir, "libadb.so").absolutePath,
            "/data/data/com.termux/files/usr/bin/adb",
            "adb",
            "/system/bin/adb",
            "/data/local/tmp/adb"
        )

        var adbPath: String? = null
        for (c in candidates) {
            try {
                val f = File(c)
                if (c == "adb" || (f.exists() && f.canExecute())) {
                    val p = ProcessBuilder(listOf(c, "version"))
                        .apply {
                            environment()["HOME"] = context.filesDir.absolutePath
                            environment()["TMPDIR"] = context.cacheDir.absolutePath
                        }
                        .redirectErrorStream(true)
                        .start()
                    val versionOut = p.inputStream.bufferedReader().use { it.readText() }.trim()
                    if (p.waitFor() == 0) {
                        adbPath = c
                        RinovaLogManager.i("PAIR", "Biner ADB fallback ditemukan di: $c ($versionOut)")
                        break
                    }
                }
            } catch (_: Exception) {}
        }

        if (adbPath != null) {
            try {
                RinovaLogManager.i("PAIR", "Mengeksekusi CLI fallback: $adbPath pair $host:$port $cleanCode")
                val process = ProcessBuilder(listOf(adbPath, "pair", "$host:$port", cleanCode))
                    .apply {
                        directory(context.filesDir)
                        environment()["HOME"] = context.filesDir.absolutePath
                        environment()["TMPDIR"] = context.cacheDir.absolutePath
                    }
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
                val exitCode = process.waitFor()

                if (output.contains("Successfully paired", ignoreCase = true)) {
                    val msg = "SUKSES PAIRING (CLI): $output"
                    RinovaLogManager.i("PAIR", msg)
                    return@withContext Result.success(msg)
                }
            } catch (e: Exception) {
                RinovaLogManager.w("PAIR", "CLI fallback gagal dieksekusi: ${e.message}")
            }
        }

        val finalErr = "Pairing nirkabel belum berhasil. Pastikan kode 6-digit [$cleanCode] cocok dan dialog 'Pasangkan dengan kode pairing' di HP target masih aktif di layar (jangan di-minimize)."
        RinovaLogManager.e("PAIR", finalErr)
        Result.failure(IOException(finalErr))
    }

    suspend fun execute(command: String, timeoutMs: Long = 20000): Result<AdbShellResponse> = withContext(Dispatchers.IO) {
        val dadb = activeDadb ?: return@withContext Result.failure(IllegalStateException("Belum terhubung ke perangkat target"))
        RinovaLogManager.d("SHELL", "> $command")
        try {
            val response = withTimeoutOrNull(timeoutMs) {
                dadb.shell(command)
            } ?: run {
                val timeoutErr = "Perintah ADB '$command' melebihi batas waktu (${timeoutMs}ms)"
                RinovaLogManager.e("SHELL", timeoutErr)
                return@withContext Result.failure(IOException(timeoutErr))
            }

            RinovaLogManager.d("SHELL", "< Exit ${response.exitCode} (Output: ${response.output.length} chars)")
            Result.success(response)
        } catch (e: Exception) {
            RinovaLogManager.e("SHELL", "Gagal eksekusi '$command': ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        mutex.withLock {
            RinovaLogManager.i("CONN", "Memutuskan sesi sambungan ADB.")
            try {
                activeDadb?.close()
            } catch (_: Exception) {}
            activeDadb = null
            _connectionState.value = AdbConnectionState.Disconnected
        }
    }

    private fun fetchDeviceInfoInternal(dadb: Dadb): DeviceInfo {
        fun getProp(name: String): String {
            return try {
                dadb.shell("getprop $name").output.trim()
            } catch (_: Exception) {
                ""
            }
        }

        val brand = getProp("ro.product.brand").ifEmpty { "Android" }
        val model = getProp("ro.product.model").ifEmpty { "Device" }
        val manufacturer = getProp("ro.product.manufacturer").ifEmpty { brand }
        val androidVersion = getProp("ro.build.version.release").ifEmpty { "14" }
        val sdkInt = getProp("ro.build.version.sdk").toIntOrNull() ?: 34
        val cpuAbi = getProp("ro.product.cpu.abi").ifEmpty { "arm64-v8a" }
        val securityPatch = getProp("ro.build.version.security_patch").ifEmpty { "2024-01-01" }
        val deviceCodename = getProp("ro.product.device").ifEmpty { getProp("ro.product.name") }

        // Baterai empiris
        var batteryLevel = -1
        var isCharging = false
        try {
            val batteryOut = dadb.shell("dumpsys battery").output
            batteryOut.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("level:")) {
                    batteryLevel = trimmed.substringAfter("level:").trim().toIntOrNull() ?: -1
                }
                if (trimmed.startsWith("status:")) {
                    val status = trimmed.substringAfter("status:").trim().toIntOrNull() ?: 0
                    isCharging = status == 2 || status == 5
                }
            }
        } catch (_: Exception) {}

        // RAM empiris
        var totalRamMb = 0L
        var freeRamMb = 0L
        try {
            val memOut = dadb.shell("cat /proc/meminfo").output
            memOut.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("MemTotal:")) {
                    totalRamMb = (trimmed.filter { it.isDigit() }.toLongOrNull() ?: 0L) / 1024L
                }
                if (trimmed.startsWith("MemAvailable:")) {
                    freeRamMb = (trimmed.filter { it.isDigit() }.toLongOrNull() ?: 0L) / 1024L
                }
            }
        } catch (_: Exception) {}

        val customRom = VendorProfileDetector.detect(manufacturer, brand).displayName

        return DeviceInfo(
            brand = brand,
            model = model,
            manufacturer = manufacturer,
            androidVersion = androidVersion,
            sdkInt = sdkInt,
            cpuAbi = cpuAbi,
            securityPatch = securityPatch,
            deviceCodename = deviceCodename,
            customRomBrand = customRom,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            totalRamMb = totalRamMb,
            freeRamMb = freeRamMb
        )
    }
}
