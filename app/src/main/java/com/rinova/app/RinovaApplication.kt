package com.rinova.app

import android.app.Application
import android.os.Build
import com.rinova.app.core.log.RinovaLogManager
import org.conscrypt.Conscrypt
import java.security.Security

class RinovaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            // Pasang Conscrypt provider di posisi terdepan untuk engine kriptografi TLS 1.3 & SPAKE2
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
            RinovaLogManager.d("INIT", "Conscrypt Security Provider berhasil dipasang sebagai provider primer.")
        } catch (e: Throwable) {
            RinovaLogManager.d("INIT", "Menggunakan default security provider platform: ${e.message}")
        }

        RinovaLogManager.i(
            "INIT",
            "Rinova Toolkit v1.6.0 diinisialisasi pada ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})"
        )
    }
}
