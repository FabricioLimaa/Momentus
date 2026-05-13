package br.com.fabriciolima.momentus.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurityUtils {
    private const val KEY_ALIAS = "momentus_db_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS_NAME = "security_prefs"
    private const val ENCRYPTED_KEY = "encrypted_db_passphrase"
    private const val IV = "db_iv"

    /**
     * Obtém ou gera uma frase secreta para o SQLCipher.
     */
    fun getDatabasePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedKeyBase64 = prefs.getString(ENCRYPTED_KEY, null)
        val ivBase64 = prefs.getString(IV, null)

        return if (encryptedKeyBase64 != null && ivBase64 != null) {
            decryptKey(encryptedKeyBase64, ivBase64)
        } else {
            generateAndStoreKey(context)
        }
    }

    private fun generateAndStoreKey(context: Context): ByteArray {
        // 1. Gera uma chave aleatória de 32 bytes (256 bits)
        val passphrase = ByteArray(32)
        java.security.SecureRandom().nextBytes(passphrase)

        // 2. Gera a chave mestra no Android Keystore
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        keyGenerator.init(spec)
        val masterKey = keyGenerator.generateKey()

        // 3. Criptografa a frase secreta com a chave mestra
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val encryptedKey = cipher.doFinal(passphrase)
        val iv = cipher.iv

        // 4. Salva o IV e a chave criptografada no SharedPreferences
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(ENCRYPTED_KEY, Base64.encodeToString(encryptedKey, Base64.DEFAULT))
            putString(IV, Base64.encodeToString(iv, Base64.DEFAULT))
            apply()
        }

        return passphrase
    }

    private fun decryptKey(encryptedKeyBase64: String, ivBase64: String): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = Base64.decode(ivBase64, Base64.DEFAULT)
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val encryptedKey = Base64.decode(encryptedKeyBase64, Base64.DEFAULT)
        return cipher.doFinal(encryptedKey)
    }
}
