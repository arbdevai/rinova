package com.rinova.app.core.adb

import com.rinova.app.core.model.AdbConnectionState
import com.rinova.app.core.model.DeviceInfo
import com.rinova.app.core.util.VendorProfileDetector
import dadb.AdbShellResponse
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class AdbConnectionManager {

    private val mutex = Mutex()
    private var activeDadb: Dadb? = null

    private val _connectionState = MutableStateFlow<AdbConnectionState>(AdbConnectionState.Disconnected)
    val connectionState: StateFlow<AdbConnectionState> = _connectionState.asStateFlow()

    suspend fun connect(host: String, port: Int): Result<DeviceInfo> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                _connectionState.value = AdbConnectionState.Connecting(host, port)

                // 1. Validasi socket reachability
                val socketTest = Socket()
                try {
                    socketTest.connect(InetSocketAddress(host, port), 4000)
                    socketTest.close()
                } catch (e: Exception) {
                    val err = "Gagal menjangkau $host:$port. Pastikan HP target dan HP teknisi dalam satu WiFi / Hotspot dan Wireless Debugging aktif."
                    _connectionState.value = AdbConnectionState.Error(err)
                    return@withContext Result.failure(IOException(err, e))
                }

                _connectionState.value = AdbConnectionState.Authorizing(host, port)

                // 2. Inisialisasi Dadb handshake
                activeDadb?.close()
                val dadb = Dadb.create(host, port)
                activeDadb = dadb

                // 3. Ambil data identitas perangkat target
                val deviceInfo = fetchDeviceInfoInternal(dadb)

                _connectionState.value = AdbConnectionState.Connected(host, port, deviceInfo)
                Result.success(deviceInfo)
            } catch (e: Exception) {
                activeDadb?.close()
                activeDadb = null
                val err = "Gagal otorisasi ADB ke $host:$port: ${e.localizedMessage ?: "Koneksi ditolak"}. Periksa popup 'Izinkan Debugging Nirkabel' di HP target."
                _connectionState.value = AdbConnectionState.Error(err)
                Result.failure(IOException(err, e))
            }
        }
    }

    suspend fun pair(host: String, port: Int, pairingCode: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanCode = pairingCode.trim()
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            return@withContext Result.failure(IllegalArgumentException("Kode pairing harus berupa 6 digit angka."))
        }

        // 1. Validasi jangkauan socket port pairing sementara
        val socketTest = Socket()
        try {
            socketTest.connect(InetSocketAddress(host, port), 4000)
            socketTest.close()
        } catch (e: Exception) {
            val err = "Gagal menjangkau $host:$port. Pastikan dialog pairing di HP target masih terbuka dan satu jaringan Wi-Fi."
            return@withContext Result.failure(IOException(err, e))
        }

        // 2. Cari eksekutor binary adb
        val candidates = listOf("adb", "/usr/bin/adb", "/system/bin/adb", "/data/local/tmp/adb")
        var adbPath: String? = null
        for (c in candidates) {
            try {
                val p = ProcessBuilder(listOf(c, "version")).redirectErrorStream(true).start()
                if (p.waitFor() == 0) {
                    adbPath = c
                    break
                }
            } catch (_: Exception) {}
        }

        if (adbPath == null) {
            return@withContext Result.failure(
                IOException("Biner ADB lokal tidak ditemukan di host. Jalankan 'adb pair $host:$port $cleanCode' pada terminal teknisi.")
            )
        }

        try {
            val process = ProcessBuilder(listOf(adbPath, "pair", "$host:$port", cleanCode))
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            val exitCode = process.waitFor()

            if (output.contains("Successfully paired", ignoreCase = true)) {
                Result.success(output)
            } else {
                val errMsg = if (output.isNotEmpty()) output else "Proses pairing gagal (Exit code: $exitCode)."
                Result.failure(IOException(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun execute(command: String, timeoutMs: Long = 20000): Result<AdbShellResponse> = withContext(Dispatchers.IO) {
        val dadb = activeDadb ?: return@withContext Result.failure(IllegalStateException("Belum terhubung ke perangkat target"))
        try {
            val response = withTimeoutOrNull(timeoutMs) {
                dadb.shell(command)
            } ?: return@withContext Result.failure(IOException("Perintah ADB '$command' melebihi batas waktu (Timeout)"))

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                activeDadb?.close()
            } catch (_: Exception) {}
            activeDadb = null
            _connectionState.value = AdbConnectionState.Disconnected
        }
    }

    private fun fetchDeviceInfoInternal(dadb: Dadb): DeviceInfo {
        fun prop(name: String): String {
            return try {
                dadb.shell("getprop $name").output.trim()
            } catch (_: Exception) {
                ""
            }
        }

        val manufacturer = prop("ro.product.manufacturer").ifEmpty { "Android" }
        val brand = prop("ro.product.brand").ifEmpty { manufacturer }
        val model = prop("ro.product.model").ifEmpty { "Perangkat Nirkabel" }
        val codename = prop("ro.product.device")
        val androidVer = prop("ro.build.version.release").ifEmpty { "11" }
        val sdkInt = prop("ro.build.version.sdk").toIntOrNull() ?: 30
        val securityPatch = prop("ro.build.version.security_patch").ifEmpty { "-" }
        val cpuAbi = prop("ro.product.cpu.abi").ifEmpty { "arm64-v8a" }
        val miuiVer = prop("ro.miui.ui.version.name")
        val customRom = if (miuiVer.isNotEmpty()) "MIUI/HyperOS $miuiVer" else VendorProfileDetector.detect(manufacturer, brand).displayName

        // Parse Battery info via dumpsys battery
        var batteryLevel = -1
        var isCharging = false
        try {
            val batteryOut = dadb.shell("dumpsys battery").output
            for (line in batteryOut.lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("level:")) {
                    batteryLevel = trimmed.substringAfter("level:").trim().toIntOrNull() ?: -1
                } else if (trimmed.startsWith("status:")) {
                    val status = trimmed.substringAfter("status:").trim().toIntOrNull() ?: 1
                    isCharging = (status == 2 || status == 5) // 2=Charging, 5=Full
                }
            }
        } catch (_: Exception) {}

        // Parse RAM info
        var totalRamMb = 0L
        var freeRamMb = 0L
        try {
            val memOut = dadb.shell("cat /proc/meminfo").output
            for (line in memOut.lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("MemTotal:")) {
                    val kb = trimmed.substringAfter("MemTotal:").trim().substringBefore(" ").toLongOrNull() ?: 0L
                    totalRamMb = kb / 1024L
                } else if (trimmed.startsWith("MemAvailable:")) {
                    val kb = trimmed.substringAfter("MemAvailable:").trim().substringBefore(" ").toLongOrNull() ?: 0L
                    freeRamMb = kb / 1024L
                }
            }
        } catch (_: Exception) {}

        return DeviceInfo(
            manufacturer = manufacturer,
            brand = brand,
            model = model,
            deviceCodename = codename,
            androidVersion = androidVer,
            sdkInt = sdkInt,
            securityPatch = securityPatch,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            customRomBrand = customRom,
            cpuAbi = cpuAbi,
            totalRamMb = totalRamMb,
            freeRamMb = freeRamMb
        )
    }
}
