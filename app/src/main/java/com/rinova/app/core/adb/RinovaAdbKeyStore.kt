package com.rinova.app.core.adb

import android.content.Context
import com.rinova.app.core.log.RinovaLogManager
import dadb.AdbKeyPair
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Date

data class RinovaKeyBundle(
    val javaKeyPair: KeyPair,
    val certificate: X509Certificate,
    val dadbKeyPair: AdbKeyPair
)

object RinovaAdbKeyStore {

    private var cachedBundle: RinovaKeyBundle? = null
    private val lock = Any()

    fun getOrGenerateKeyBundle(context: Context): RinovaKeyBundle = synchronized(lock) {
        cachedBundle?.let { return it }

        val keysDir = File(context.filesDir, "adb_keys").apply { mkdirs() }
        val privKeyDerFile = File(keysDir, "adbkey.der")
        val pubKeyDerFile = File(keysDir, "adbkey_pub.der")
        val privKeyPemFile = File(keysDir, "adbkey.pem")
        val certFile = File(keysDir, "adbkey.crt")

        if (privKeyDerFile.exists() && pubKeyDerFile.exists() && privKeyPemFile.exists() && certFile.exists()) {
            try {
                RinovaLogManager.d("KEY", "Memuat kredensial ADB persisten dari penyimpanan lokal...")
                val kf = KeyFactory.getInstance("RSA")
                val privKey = kf.generatePrivate(PKCS8EncodedKeySpec(privKeyDerFile.readBytes()))
                val pubKey = kf.generatePublic(X509EncodedKeySpec(pubKeyDerFile.readBytes()))
                val javaKeyPair = KeyPair(pubKey, privKey)

                val cf = CertificateFactory.getInstance("X.509")
                val cert = FileInputStream(certFile).use { cf.generateCertificate(it) as X509Certificate }

                val dadbKeyPair = AdbKeyPair.read(privKeyPemFile)
                val bundle = RinovaKeyBundle(javaKeyPair, cert, dadbKeyPair)
                cachedBundle = bundle
                return bundle
            } catch (e: Exception) {
                RinovaLogManager.w("KEY", "Kredensial tersimpan korup (${e.message}), membuat ulang kunci baru...")
            }
        }

        RinovaLogManager.i("KEY", "Menghasilkan kunci RSA 2048-bit & sertifikat X.509 TLS untuk engine ADB Rinova...")
        privKeyDerFile.delete()
        pubKeyDerFile.delete()
        privKeyPemFile.delete()
        certFile.delete()

        // 1. Generate KeyPair RSA 2048-bit murni langsung via KeyPairGenerator
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val javaKeyPair = kpg.generateKeyPair()

        // 2. Simpan kunci privat & publik dalam format standar DER
        privKeyDerFile.writeBytes(javaKeyPair.private.encoded)
        pubKeyDerFile.writeBytes(javaKeyPair.public.encoded)

        // 3. Simpan kunci privat dalam format PEM untuk dadb
        privKeyPemFile.writer().use { out ->
            val base64 = Base64.getMimeEncoder(64, "\n".toByteArray())
            out.write("-----BEGIN PRIVATE KEY-----\n")
            out.write(base64.encodeToString(javaKeyPair.private.encoded))
            out.write("\n-----END PRIVATE KEY-----")
        }

        // 4. Buat sertifikat X.509 self-signed untuk autentikasi TLS 1.3 & SPAKE2
        val now = System.currentTimeMillis()
        val notBefore = Date(now - 86400000L) // 24 jam lalu
        val notAfter = Date(now + 86400000L * 365 * 25) // Berlaku 25 tahun
        val issuer = X500Name("CN=Rinova Technician Toolkit, OU=Diagnostics, O=Rinova")
        val serial = BigInteger.valueOf(now)

        val certBuilder = JcaX509v3CertificateBuilder(
            issuer,
            serial,
            notBefore,
            notAfter,
            issuer,
            javaKeyPair.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(javaKeyPair.private)
        val certHolder = certBuilder.build(signer)
        val cert = JcaX509CertificateConverter().getCertificate(certHolder)

        // 5. Simpan sertifikat ke disk
        FileOutputStream(certFile).use { fos ->
            fos.write(cert.encoded)
        }

        val dadbKeyPair = AdbKeyPair.read(privKeyPemFile)
        val bundle = RinovaKeyBundle(javaKeyPair, cert, dadbKeyPair)
        cachedBundle = bundle
        RinovaLogManager.i("KEY", "Kredensial RSA 2048 & Sertifikat TLS Rinova berhasil dibuat dan disimpan persisten.")
        bundle
    }
}
