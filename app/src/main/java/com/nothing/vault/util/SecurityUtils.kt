package com.nothing.vault.util

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.security.SecureRandom
import java.security.spec.KeySpec

object SecurityUtils {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val HASH_ITERATIONS = 10000
    private const val HASH_LENGTH = 256

    fun hashPin(pin: String, salt: ByteArray): String {
        val spec: KeySpec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            HASH_ITERATIONS,
            HASH_LENGTH
        )
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return factory.generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun verifyPin(pin: String, storedHash: String, saltHex: String): Boolean {
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val hash = hashPin(pin, salt)
        return hash == storedHash
    }

    fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    fun hexToBytes(hex: String): ByteArray =
        hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
