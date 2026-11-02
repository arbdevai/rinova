package com.rinova.app.core.adb

import android.content.Context
import com.rinova.app.core.log.RinovaLogManager
import dadb.AdbKeyPair
import org.bouncycastle.x509.X509V3CertificateGenerator
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import java.util.Date
import javax.security.auth.x500.X500Principal

data class RinovaKeyBundle(
    val javaKeyPair: KeyPair,
    val certificate: X509Certificate,
    val dadbKeyPair: AdbKeyPair
)

object RinovaAdbKeyStore {

    private var cachedBundle: RinovaKeyBundle? = null
    private val lock = Any()

    private const val PEM_PREFIX = "-----BEGIN PRIVATE KEY-----"
    private const val PEM_SUFFIX = "-----END PRIVATE KEY-----"

    fun getOrGenerateKeyBundle(context: Context): RinovaKeyBundle = synchronized(lock) {
        cachedBundle?.let { return it }

        val keysDir = File(context.filesDir, "adb_keys").apply { mkdirs() }
        val privKeyFile = File(keysDir, "adbkey")
        val pubKeyFile = File(keysDir, "adbkey.pub")
        val certFile = File(keysDir, "adbkey.crt")

        if (privKeyFile.exists() && pubKeyFile.exists() && certFile.exists()) {
            try {
                RinovaLogManager.d("KEY", "Memuat kredensial ADB persisten dari penyimpanan lokal...")
                val privKey = parsePemPrivateKey(privKeyFile.readBytes())
                val javaKeyPair = deriveKeyPairFromPrivate(privKey)

                val cf = CertificateFactory.getInstance("X.509")
                val cert = FileInputStream(certFile).use { cf.generateCertificate(it) as X509Certificate }

                val dadbKeyPair = AdbKeyPair.read(privKeyFile, pubKeyFile)
                val bundle = RinovaKeyBundle(javaKeyPair, cert, dadbKeyPair)
                cachedBundle = bundle
                return bundle
            } catch (e: Exception) {
                RinovaLogManager.w("KEY", "Kredensial tersimpan korup (${e.message}), membuat ulang kunci baru...")
            }
        }

        RinovaLogManager.i("KEY", "Menghasilkan kunci RSA 2048-bit & sertifikat X.509 TLS untuk engine ADB Rinova...")
        privKeyFile.delete()
        pubKeyFile.delete()
        certFile.delete()

        // 1. Generate standard adbkey dan adbkey.pub menggunakan generator dadb
        AdbKeyPair.generate(privKeyFile, pubKeyFile)

        // 2. Parse kunci privat yang dihasilkan
        val privKey = parsePemPrivateKey(privKeyFile.readBytes())
        val javaKeyPair = deriveKeyPairFromPrivate(privKey)

        // 3. Buat sertifikat X.509 self-signed untuk autentikasi TLS 1.3 & SPAKE2
        @Suppress("DEPRECATION")
        val certGen = X509V3CertificateGenerator()
        val principal = X500Principal("CN=Rinova Technician Toolkit, OU=Diagnostics, O=Rinova")
        val now = System.currentTimeMillis()

        certGen.setSerialNumber(BigInteger.valueOf(now))
        certGen.setIssuerDN(principal)
        certGen.setSubjectDN(principal)
        certGen.setNotBefore(Date(now - 86400000L)) // 24 jam lalu
        certGen.setNotAfter(Date(now + 86400000L * 365 * 25)) // Berlaku 25 tahun
        certGen.setPublicKey(javaKeyPair.public)
        certGen.setSignatureAlgorithm("SHA256WithRSA")

        @Suppress("DEPRECATION")
        val cert = certGen.generate(javaKeyPair.private)

        // 4. Simpan sertifikat ke disk
        FileOutputStream(certFile).use { fos ->
            fos.write(cert.encoded)
        }

        val dadbKeyPair = AdbKeyPair.read(privKeyFile, pubKeyFile)
        val bundle = RinovaKeyBundle(javaKeyPair, cert, dadbKeyPair)
        cachedBundle = bundle
        RinovaLogManager.i("KEY", "Kredensial RSA & Sertifikat TLS Rinova berhasil diinisialisasi dan disimpan.")
        bundle
    }

    private fun parsePemPrivateKey(bytes: ByteArray): PrivateKey {
        val pemString = String(bytes)
            .replace(PEM_PREFIX, "")
            .replace(PEM_SUFFIX, "")
            .replace(Regex("\\s+"), "")
        val decoded = Base64.getDecoder().decode(pemString)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(PKCS8EncodedKeySpec(decoded))
    }

    private fun deriveKeyPairFromPrivate(privateKey: PrivateKey): KeyPair {
        val crtKey = privateKey as? RSAPrivateCrtKey
            ?: throw IllegalStateException("Kunci privat bukan RSAPrivateCrtKey")
        val pubSpec = RSAPublicKeySpec(crtKey.modulus, crtKey.publicExponent)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(pubSpec)
        return KeyPair(publicKey, privateKey)
    }
}
