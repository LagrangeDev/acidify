package org.ntqqrev.acidify.internal.crypto.hash

internal class SHA1Stream {
    private val state = IntArray(5)
    private val count = IntArray(2)
    private val buffer = ByteArray(Sha1BlockSize)

    companion object {
        const val Sha1BlockSize = 64
        const val Sha1DigestSize = 20

        private val Padding = byteArrayOf(
            0x80.toByte(), 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
        )
    }

    init {
        reset()
    }

    fun reset() {
        state[0] = 0x67452301
        state[1] = 0xEFCDAB89.toInt()
        state[2] = 0x98BADCFE.toInt()
        state[3] = 0x10325476
        state[4] = 0xC3D2E1F0.toInt()
        count[0] = 0
        count[1] = 0
    }

    private fun transform(block: ByteArray, offset: Int) {
        val w = IntArray(80)

        // 1. 16 × 32-bit words
        for (i in 0 until 16) {
            val j = offset + i * 4
            w[i] = ((block[j].toInt() and 0xFF) shl 24) or
                    ((block[j + 1].toInt() and 0xFF) shl 16) or
                    ((block[j + 2].toInt() and 0xFF) shl 8) or
                    (block[j + 3].toInt() and 0xFF)
        }

        // 2. Extend to 80 words
        for (i in 16 until 80) {
            val v = w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]
            w[i] = (v shl 1) or (v ushr 31)
        }

        var a = state[0]
        var b = state[1]
        var c = state[2]
        var d = state[3]
        var e = state[4]

        // 3. Main loop
        for (i in 0 until 80) {
            val temp = when (i) {
                in 0..19 -> ((b and c) or ((b.inv()) and d)) + 0x5A827999
                in 20..39 -> (b xor c xor d) + 0x6ED9EBA1
                in 40..59 -> ((b and c) or (b and d) or (c and d)) + 0x8F1BBCDC.toInt()
                else -> (b xor c xor d) + 0xCA62C1D6.toInt()
            }

            val t = ((a shl 5) or (a ushr 27)) + temp + e + w[i]
            e = d
            d = c
            c = (b shl 30) or (b ushr 2)
            b = a
            a = t
        }

        state[0] += a
        state[1] += b
        state[2] += c
        state[3] += d
        state[4] += e
    }

    fun update(data: ByteArray, len: Int = data.size) {
        var index = (count[0] ushr 3) and 0x3F
        val old0 = count[0]
        count[0] += len shl 3
        if (count[0].toUInt() < old0.toUInt()) count[1]++
        count[1] += len ushr 29

        val partLen = Sha1BlockSize - index
        var i = 0

        if (len >= partLen) {
            // copy manually
            for (j in 0 until partLen) {
                buffer[index + j] = data[j]
            }
            transform(buffer, 0)

            i = partLen
            while (i + Sha1BlockSize <= len) {
                transform(data, i)
                i += Sha1BlockSize
            }
            index = 0
        }

        if (i < len) {
            for (j in 0 until (len - i)) {
                buffer[index + j] = data[i + j]
            }
        }
    }

    fun hash(digest: ByteArray, bigEndian: Boolean) {
        if (digest.size < Sha1DigestSize) {
            throw IllegalArgumentException("Digest array too small")
        }

        for (i in state.indices) {
            val v = state[i]
            if (bigEndian) {
                digest[i * 4] = ((v ushr 24) and 0xFF).toByte()
                digest[i * 4 + 1] = ((v ushr 16) and 0xFF).toByte()
                digest[i * 4 + 2] = ((v ushr 8) and 0xFF).toByte()
                digest[i * 4 + 3] = (v and 0xFF).toByte()
            } else {
                digest[i * 4] = (v and 0xFF).toByte()
                digest[i * 4 + 1] = ((v ushr 8) and 0xFF).toByte()
                digest[i * 4 + 2] = ((v ushr 16) and 0xFF).toByte()
                digest[i * 4 + 3] = ((v ushr 24) and 0xFF).toByte()
            }
        }
    }

    fun final(digest: ByteArray) {
        if (digest.size != Sha1DigestSize) {
            throw IllegalArgumentException("Digest array must be of size $Sha1DigestSize")
        }

        val bits = ByteArray(8)
        for (i in 0 until 8) {
            val byteIndex = if (i >= 4) 0 else 1
            val shift = (3 - (i and 3)) * 8
            bits[i] = ((count[byteIndex] ushr shift) and 0xFF).toByte()
        }

        val index = (count[0] ushr 3) and 0x3F
        val padLen = if (index < 56) 56 - index else 120 - index

        update(Padding, padLen)
        update(bits, 8)

        // write digest
        for (i in 0 until Sha1DigestSize) {
            val byteIndex = i ushr 2
            val shift = (3 - (i and 3)) * 8
            digest[i] = ((state[byteIndex] ushr shift) and 0xFF).toByte()
        }
    }
}