package com.example.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android KeyStore Manager.
 *
 * Encrypts and persists sensitive credentials (e.g. Gemini Cloud API keys)
 * using AES-256-GCM authenticated encryption. The master cryptographic key never leaves
 * the hardware security module / TEE / StrongBox.
 */
class KeyStoreManager(context: Context) {

    companion object {
        private const val TAG = "KeyStoreManager"
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "LocalAgent_MasterSecretKey_v1"
        private const val PREFS_NAME = "localagent_secure_vault"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
        load(null)
    }

    init {
        ensureMasterKeyExists()
    }

    private fun ensureMasterKeyExists() {
        try {
            if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE_PROVIDER
                )
                val keyGenSpec = KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()

                keyGenerator.init(keyGenSpec)
                keyGenerator.generateKey()
                Log.i(TAG, "Initialized new hardware-backed AES-256 master key in AndroidKeyStore.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize KeyStore master key: ${e.message}", e)
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving secret key from KeyStore: ${e.message}", e)
            null
        }
    }

    /**
     * Encrypts and securely stores a secret string under the given key alias.
     */
    @Synchronized
    fun storeSecret(alias: String, secret: String): Boolean {
        if (secret.isBlank()) {
            removeSecret(alias)
            return true
        }

        val key = getSecretKey() ?: return false
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))

            val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encodedPayload = Base64.encodeToString(cipherText, Base64.NO_WRAP)

            prefs.edit()
                .putString("${alias}_iv", encodedIv)
                .putString("${alias}_payload", encodedPayload)
                .apply()

            Log.d(TAG, "Stored encrypted secret for alias '$alias'")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt secret for alias '$alias': ${e.message}", e)
            false
        }
    }

    /**
     * Decrypts and retrieves a secret string. Returns null if not found or decryption fails.
     */
    @Synchronized
    fun getSecret(alias: String): String? {
        val encodedIv = prefs.getString("${alias}_iv", null) ?: return null
        val encodedPayload = prefs.getString("${alias}_payload", null) ?: return null
        val key = getSecretKey() ?: return null

        return try {
            val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
            val cipherText = Base64.decode(encodedPayload, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val plainTextBytes = cipher.doFinal(cipherText)
            String(plainTextBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt secret for alias '$alias': ${e.message}", e)
            null
        }
    }

    /**
     * Checks if a secret exists under the given alias.
     */
    fun hasSecret(alias: String): Boolean {
        return prefs.contains("${alias}_payload") && prefs.contains("${alias}_iv")
    }

    /**
     * Deletes a secret from storage.
     */
    @Synchronized
    fun removeSecret(alias: String) {
        prefs.edit()
            .remove("${alias}_iv")
            .remove("${alias}_payload")
            .apply()
        Log.d(TAG, "Removed secret for alias '$alias'")
    }
}
