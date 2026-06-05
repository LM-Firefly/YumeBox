package com.github.yumelira.yumebox.common.util

import android.util.Base64
import java.security.SecureRandom
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters

/**
 * Helper for generating and deriving age encryption keys.
 *
 * Age encryption uses X25519 key exchange. The key formats are:
 * - Secret key: AGE-SECRET-KEY-1XXXXXXX (Base64-encoded 32-byte X25519 scalar)
 * - Public key (recipient): age1XXXXXXX (Base64-encoded 32-byte X25519 point)
 */
object AgeKeyHelper {
    private const val SECRET_KEY_PREFIX = "AGE-SECRET-KEY-1"
    private const val RECIPIENT_PREFIX = "age1"
    /**
     * Generate a new age secret key.
     * Returns the secret key in the format: AGE-SECRET-KEY-1XXXXXXX
     */
    fun generateSecretKey(): String {
        val generator = X25519KeyPairGenerator()
        generator.init(X25519KeyGenerationParameters(SecureRandom()))
        val keyPair = generator.generateKeyPair()
        val privateKey = keyPair.private as X25519PrivateKeyParameters
        val encoded = Base64.encodeToString(
            privateKey.encoded,
            Base64.NO_WRAP or Base64.NO_PADDING
        )
        return "$SECRET_KEY_PREFIX$encoded"
    }
    /**
     * Derive the age public key (recipient) from an age secret key (identity).
     * The secret key should be in the format: AGE-SECRET-KEY-1XXXXXXX
     * Returns the public key in the format: age1XXXXXXX
     */
    fun deriveRecipient(secretKey: String): Result<String> {
        return try {
            val trimmed = secretKey.trim()
            if (!trimmed.startsWith(SECRET_KEY_PREFIX)) {
                return Result.failure(
                    IllegalArgumentException("age 私钥格式无效，应以 $SECRET_KEY_PREFIX 开头")
                )
            }
            val keyBase64 = trimmed.removePrefix(SECRET_KEY_PREFIX)
            val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP or Base64.NO_PADDING)
            val privateKey = X25519PrivateKeyParameters(keyBytes)
            val publicKey = privateKey.generatePublicKey()
            val publicKeyBase64 = Base64.encodeToString(
                publicKey.encoded,
                Base64.NO_WRAP or Base64.NO_PADDING
            )
            Result.success("$RECIPIENT_PREFIX$publicKeyBase64")
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("无效的 age 私钥: ${e.message}"))
        }
    }
    /**
     * Validate an age secret key format.
     */
    fun isValidSecretKeyFormat(secretKey: String): Boolean {
        return secretKey.trim().startsWith(SECRET_KEY_PREFIX)
    }
}
