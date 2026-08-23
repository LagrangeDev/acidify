package org.ntqqrev.acidify.internal.crypto.aes

import kotlin.random.Random

internal object AesGcmProvider {
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 16

    fun encrypt(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray = Random.nextBytes(IV_SIZE),
    ): ByteArray {
        require(iv.size == IV_SIZE) { "AES-GCM IV must be $IV_SIZE bytes" }

        val result = aesGcmEncrypt(key, data, iv)
        return iv + result.ciphertext + result.tag
    }

    fun decrypt(data: ByteArray, key: ByteArray): ByteArray {
        require(data.size >= IV_SIZE + TAG_SIZE) { "AES-GCM envelope must be at least ${IV_SIZE + TAG_SIZE} bytes" }

        val iv = data.copyOfRange(0, IV_SIZE)
        val ciphertext = data.copyOfRange(IV_SIZE, data.size - TAG_SIZE)
        val tag = data.copyOfRange(data.size - TAG_SIZE, data.size)
        return aesGcmDecrypt(key, ciphertext, iv, tag)
    }
}
