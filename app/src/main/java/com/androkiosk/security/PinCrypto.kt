package com.androkiosk.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

data class PinDigest(val salt: String, val mac: String)

object PinCrypto {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "androkiosk.pin"
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_HMAC_SHA256
    private const val SALT_BYTES = 16

    fun isReady(): Boolean = runCatching { keyStore().containsAlias(ALIAS) }.getOrDefault(false)

    fun digest(pin: String): PinDigest? = runCatching {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        PinDigest(encode(salt), encode(mac(pin, salt)))
    }.getOrNull()

    fun matches(pin: String, digest: PinDigest): Boolean = runCatching {
        MessageDigest.isEqual(decode(digest.mac), mac(pin, decode(digest.salt)))
    }.getOrDefault(false)

    private fun mac(pin: String, salt: ByteArray): ByteArray =
        Mac.getInstance(ALGORITHM).run {
            init(key())
            update(salt)
            doFinal(pin.toByteArray(Charsets.UTF_8))
        }

    private fun key(): SecretKey {
        val store = keyStore()
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(ALGORITHM, KEYSTORE).apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN).build())
        }.generateKey()
    }

    private fun keyStore() = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)
}
