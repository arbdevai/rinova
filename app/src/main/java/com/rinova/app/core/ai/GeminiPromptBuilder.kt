package com.rinova.app.core.ai

import com.rinova.app.core.model.DeviceInfo
import com.rinova.app.core.model.RogueAppItem
import com.rinova.app.core.model.StorageBreakdown
import com.rinova.app.core.util.FileSizeFormatter

object GeminiPromptBuilder {

    fun buildEmpiricalPrompt(
        device: DeviceInfo,
        storage: StorageBreakdown,
        suspiciousApps: List<RogueAppItem>,
        topWindowApp: String?
    ): String {
        val orphanedFoldersSummary = if (storage.orphanedDirectories.isEmpty()) {
            "Tidak ditemukan folder yatim di /sdcard/Android/data atau obb."
        } else {
            storage.orphanedDirectories.joinToString("\n") {
                "- Jalur: ${it.path} | Ukuran: ${FileSizeFormatter.format(it.sizeBytes)} | Indikasi: ${it.reason}"
            }
        }

        val suspiciousAppsSummary = if (suspiciousApps.isEmpty()) {
            "Tidak ada aplikasi terdeteksi tanpa launcher atau penyalahgunaan overlay."
        } else {
            suspiciousApps.take(15).joinToString("\n") { app ->
                "- Paket: ${app.packageName} (Label: ${app.appName}) | Ikon Launcher: ${app.hasLauncherIcon} | Overlay: ${app.hasOverlayPermission} | Boot: ${app.hasBootCompletedPermission} | Aktif Teratas: ${app.isCurrentlyActiveOnTop} | Alasan Sistem: ${app.evidenceReason}"
            }
        }

        return """
KAMU ADALAH RINOVA AI DIAGNOSTIC ENGINE UNTUK TEKNISI SMARTPHONE ANDROID.
PRINSIP KERJA MUTLAK:
1. Analisis WAJIB 100% BERBASIS DATA EMPIRIS di bawah. DILARANG KERAS berasumsi atau menebak.
2. DILARANG MEREKOMENDASIKAN HAPUS/NONAKTIFKAN PAKET KRITIS SISTEM (SystemUI, Telepon, SMS, Pengaturan, Layanan Google Play inti, SIM/RIL).
3. Klasifikasikan setiap tindakan dengan ketat:
   - SAFE_ACTION: Pembersihan cache terisolasi, folder yatim terbukti, atau adware nakal terkonfirmasi.
   - OPTIONAL_TWEAK: Optimasi performa sistem aman (skala animasi, doze, trim caches).
   - CRITICAL_KEEP: Paket yang dicurigai namun ternyata sistem vital, JANGAN DISENTUH!
4. Setiap rekomendasi WAJIB menyertakan perintah ADB shell yang presisi, siap dieksekusi oleh teknisi.

DATA TELEMETRI PERANGKAT:
- Pabrikan/Model: ${device.manufacturer} ${device.model} (${device.brand})
- Versi Android / SDK: Android ${device.androidVersion} (SDK ${device.sdkInt})
- Custom ROM / UI: ${device.customRomBrand}
- RAM: Total ${device.totalRamMb} MB / Tersedia ${device.freeRamMb} MB
- Baterai: ${device.batteryLevel}% (Sedang Mengisi: ${device.isCharging})
- Jendela Aktif Teratas (Foreground Sniffer): ${topWindowApp ?: "Tidak terdeteksi / Home"}

DATA PENYIMPANAN RIIL (DUMPSYS DISKSTATS & DU):
- Total Penyimpanan: ${FileSizeFormatter.format(storage.totalBytes)}
- Terpakai: ${FileSizeFormatter.format(storage.usedBytes)} | Bebas: ${FileSizeFormatter.format(storage.freeBytes)}
- Sampah Cache Aplikasi: ${FileSizeFormatter.format(storage.appCacheBytes)}
- Sampah Crash Dump (ANR/Tombstones): ${FileSizeFormatter.format(storage.crashDumpsBytes)}
- Folder Yatim OBB/Data Terdeteksi:
$orphanedFoldersSummary

DAFTAR AUDIT APLIKASI MENCURIGAKAN (OVERLAY / HIDDEN LAUNCHER / ADS):
$suspiciousAppsSummary

FORMAT OUTPUT WAJIB BERUPA JSON SESUAI STRUKTUR INI:
{
  "deviceSummary": "Ringkasan kondisi teknis perangkat berbasis fakta",
  "overallHealthScore": 85,
  "empiricalObservations": [
    "Fakta empiris 1 ditemukan di sistem",
    "Fakta empiris 2 ditemukan di sistem"
  ],
  "recommendations": [
    {
      "id": "rec_1",
      "targetName": "Nama target (paket atau path)",
      "riskLevel": "SAFE_ACTION",
      "empiricalEvidence": "Bukti empiris nyata mengapa tindakan ini aman atau bahaya",
      "proposedActionTitle": "Judul tindakan ringkas profesional",
      "adbCommand": "pm disable-user --user 0 com.contoh.adware",
      "isExecutable": true
    }
  ],
  "optimizations": [
    {
      "title": "Optimasi Responsivitas Antarmuka",
      "rationale": "Mengurangi beban render UI dengan mengatur skala animasi jendela",
      "adbCommand": "settings put global window_animation_scale 0.5",
      "estimatedImpact": "Navigasi terasa 2x lebih responsif"
    }
  ],
  "criticalWarnings": [
    "Peringatan keamanan jika ada komponen berisiko"
  ]
}
""".trimIndent()
    }
}
