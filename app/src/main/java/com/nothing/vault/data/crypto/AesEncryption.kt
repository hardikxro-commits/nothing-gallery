package com.nothing.vault.data.crypto

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object AesEncryption {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    fun encrypt(inputFile: File, outputFile: File, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv

        FileOutputStream(outputFile).use { fos ->
            fos.write(iv)
            CipherOutputStream(fos, cipher).use { cos ->
                FileInputStream(inputFile).use { fis ->
                    fis.copyTo(cos)
                }
            }
        }

        return iv
    }

    fun encryptWithIv(inputFile: File, outputFile: File, key: SecretKey, iv: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        FileOutputStream(outputFile).use { fos ->
            CipherOutputStream(fos, cipher).use { cos ->
                FileInputStream(inputFile).use { fis ->
                    fis.copyTo(cos)
                }
            }
        }
    }

    fun decrypt(encryptedFile: File, outputFile: File, key: SecretKey, iv: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        FileInputStream(encryptedFile).use { fis ->
            skipBytes(fis, iv.size)
            CipherInputStream(fis, cipher).use { cis ->
                FileOutputStream(outputFile).use { fos ->
                    cis.copyTo(fos)
                }
            }
        }
    }

    fun decryptToBytes(encryptedFile: File, key: SecretKey, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        FileInputStream(encryptedFile).use { fis ->
            skipBytes(fis, iv.size)
            val bis = java.io.BufferedInputStream(fis)
            CipherInputStream(bis, cipher).use { cis ->
                val baos = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8192)
                while (true) {
                    val n = cis.read(buf)
                    if (n < 0) break
                    baos.write(buf, 0, n)
                }
                baos.flush()
                return baos.toByteArray()
            }
        }
    }

    fun readIv(encryptedFile: File): ByteArray {
        FileInputStream(encryptedFile).use { fis ->
            val iv = ByteArray(IV_LENGTH)
            var offset = 0
            while (offset < IV_LENGTH) {
                val read = fis.read(iv, offset, IV_LENGTH - offset)
                if (read < 0) break
                offset += read
            }
            return iv
        }
    }

    private fun skipBytes(fis: FileInputStream, count: Int) {
        var remaining = count
        val buffer = ByteArray(minOf(count, 4096))
        while (remaining > 0) {
            val toRead = minOf(buffer.size, remaining)
            val read = fis.read(buffer, 0, toRead)
            if (read < 0) throw java.io.EOFException("Unexpected EOF")
            remaining -= read
        }
    }
}
