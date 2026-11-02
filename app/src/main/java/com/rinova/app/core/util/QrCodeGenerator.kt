package com.rinova.app.core.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeGenerator {

    /**
     * Menghasilkan Bitmap QR Code untuk pairing ADB Wireless Android 11+
     * Format resmi AOSP: WIFI:T:ADB;S:<service_name>;P:<password>;;
     */
    fun generateAdbPairingPayload(serviceName: String, password: String): String {
        return "WIFI:T:ADB;S:$serviceName;P:$password;;"
    }

    fun createQrCodeBitmap(
        content: String,
        sizePx: Int = 512,
        foregroundColor: Color = Color.White,
        backgroundColor: Color = Color.Transparent
    ): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            val fg = foregroundColor.toArgb()
            val bg = backgroundColor.toArgb()

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) fg else bg
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (_: Exception) {
            null
        }
    }
}
