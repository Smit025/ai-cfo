package com.aicfo.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.aicfo.shared.security.LinkPolicy
import com.aicfo.shared.security.LocalStore
import com.aicfo.shared.security.SecureStore
import com.aicfo.shared.security.TokenVault
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Link tokens only, encrypted with an Android Keystore AES-GCM key.
 * Ciphertext lives in a private prefs file. Plaintext is never logged.
 */
class AndroidKeystoreTokenVault(context: Context) : TokenVault {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun put(key: String, value: String): Boolean {
        if (!LinkPolicy.accepts(value)) return false
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(value.encodeToByteArray())
            val buffer = ByteBuffer.allocate(4 + iv.size + encrypted.size)
            buffer.putInt(iv.size)
            buffer.put(iv)
            buffer.put(encrypted)
            prefs.edit().putString(key, Base64.encodeToString(buffer.array(), Base64.NO_WRAP)).apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun read(key: String): String? {
        val raw = prefs.getString(key, null) ?: return null
        return try {
            val bytes = Base64.decode(raw, Base64.NO_WRAP)
            val buffer = ByteBuffer.wrap(bytes)
            val ivLen = buffer.int
            if (ivLen !in 12..32) return null
            val iv = ByteArray(ivLen)
            buffer.get(iv)
            val encrypted = ByteArray(buffer.remaining())
            buffer.get(encrypted)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).decodeToString()
        } catch (_: Exception) {
            null
        }
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "aicfo.token.vault"
        const val PREFS = "aicfo.vault.cipher"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

/**
 * Account session and device PIN. Same Keystore AES-GCM approach as link tokens,
 * separate alias and prefs so logout does not wipe institution tokens.
 */
class AndroidKeystoreSecureStore(context: Context) : SecureStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun put(key: String, value: String): Boolean {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(value.encodeToByteArray())
            val buffer = ByteBuffer.allocate(4 + iv.size + encrypted.size)
            buffer.putInt(iv.size)
            buffer.put(iv)
            buffer.put(encrypted)
            prefs.edit().putString(key, Base64.encodeToString(buffer.array(), Base64.NO_WRAP)).apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun read(key: String): String? {
        val raw = prefs.getString(key, null) ?: return null
        return try {
            val bytes = Base64.decode(raw, Base64.NO_WRAP)
            val buffer = ByteBuffer.wrap(bytes)
            val ivLen = buffer.int
            if (ivLen !in 12..32) return null
            val iv = ByteArray(ivLen)
            buffer.get(iv)
            val encrypted = ByteArray(buffer.remaining())
            buffer.get(encrypted)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).decodeToString()
        } catch (_: Exception) {
            null
        }
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "aicfo.session.vault"
        const val PREFS = "aicfo.session.cipher"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

/** Trial clock, onboarding, move status, and sync metadata. Not used for bank tokens or the account session. */
class AndroidLocalStore(context: Context) : LocalStore {
    private val prefs = context.applicationContext.getSharedPreferences("aicfo.local", Context.MODE_PRIVATE)

    override fun read(key: String): String? = prefs.getString(key, null)

    override fun write(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
