package com.rinova.app.core.log

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import com.rinova.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class LogEntry(
    val id: Long,
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val stackTrace: String? = null
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

/**
 * RinovaLogManager: Pusat Pencatatan Telemetri Real-Time (No-Root Logging Engine)
 * Memungkinkan teknisi dan pengembang menginspeksi error riil, socket handshake, dan ADB output
 * tanpa perlu menebak-nebak asumsi.
 */
object RinovaLogManager {
    private const val MAX_LOGS = 1000
    private var counter = 0L

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    init {
        log(
            LogLevel.INFO,
            "INIT",
            "Rinova v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) diinisialisasi pada ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})"
        )
    }

    @Synchronized
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val stack = throwable?.let {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            it.printStackTrace(pw)
            sw.toString()
        }

        val entry = LogEntry(
            id = ++counter,
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            stackTrace = stack
        )

        val current = _logs.value
        val updated = if (current.size >= MAX_LOGS) {
            current.drop(1) + entry
        } else {
            current + entry
        }
        _logs.value = updated

        // Juga log ke logcat standar
        when (level) {
            LogLevel.DEBUG -> android.util.Log.d("Rinova:$tag", message, throwable)
            LogLevel.INFO -> android.util.Log.i("Rinova:$tag", message, throwable)
            LogLevel.WARN -> android.util.Log.w("Rinova:$tag", message, throwable)
            LogLevel.ERROR -> android.util.Log.e("Rinova:$tag", message, throwable)
        }
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)

    fun clear() {
        _logs.value = emptyList()
        log(LogLevel.INFO, "SYSTEM", "Log dibersihkan oleh teknisi.")
    }

    fun exportToString(): String {
        val sb = StringBuilder()
        sb.append("=== RINOVA SYSTEM DIAGNOSTIC LOG ===\n")
        sb.append("App Version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})\n")
        sb.append("Host Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.BRAND})\n")
        sb.append("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}, ABI: ${Build.SUPPORTED_ABIS.joinToString()})\n")
        sb.append("Timestamp Export: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("====================================\n\n")

        _logs.value.forEach { entry ->
            sb.append("[${entry.formattedTime}] [${entry.level.name}] [${entry.tag}]: ${entry.message}\n")
            if (!entry.stackTrace.isNullOrBlank()) {
                sb.append("Stack Trace:\n${entry.stackTrace}\n")
            }
        }
        return sb.toString()
    }

    fun copyToClipboard(context: Context): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Rinova Diagnostic Log", exportToString())
            clipboard.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }
}
