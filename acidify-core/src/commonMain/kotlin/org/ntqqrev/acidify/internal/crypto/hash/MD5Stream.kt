package org.ntqqrev.acidify.internal.crypto.hash

internal class MD5Stream {
    private val state = IntArray(4)
    private val count = IntArray(2)
    private val buffer = ByteArray(Md5BlockSize)

    companion object {
        const val Md5BlockSize = 64
        const val Md5DigestSize = 16

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
        val K = intArrayOf(
            0xd76aa478.toInt(), 0xe8c7b756.toInt(), 0x242070db, 0xc1bdceee.toInt(),
            0xf57c0faf.toInt(), 0x4787c62a, 0xa8304613.toInt(), 0xfd469501.toInt(),
            0x698098d8, 0x8b44f7af.toInt(), 0xffff5bb1.toInt(), 0x895cd7be.toInt(),
            0x6b901122, 0xfd987193.toInt(), 0xa679438e.toInt(), 0x49b40821,
            0xf61e2562.toInt(), 0xc040b340.toInt(), 0x265e5a51, 0xe9b6c7aa.toInt(),
            0xd62f105d.toInt(), 0x02441453, 0xd8a1e681.toInt(), 0xe7d3fbc8.toInt(),
            0x21e1cde6, 0xc33707d6.toInt(), 0xf4d50d87.toInt(), 0x455a14ed,
            0xa9e3e905.toInt(), 0xfcefa3f8.toInt(), 0x676f02d9, 0x8d2a4c8a.toInt(),
            0xfffa3942.toInt(), 0x8771f681.toInt(), 0x6d9d6122, 0xfde5380c.toInt(),
            0xa4beea44.toInt(), 0x4bdecfa9, 0xf6bb4b60.toInt(), 0xbebfbc70.toInt(),
            0x289b7ec6, 0xeaa127fa.toInt(), 0xd4ef3085.toInt(), 0x04881d05,
            0xd9d4d039.toInt(), 0xe6db99e5.toInt(), 0x1fa27cf8, 0xc4ac5665.toInt(),
            0xf4292244.toInt(), 0x432aff97, 0xab9423a7.toInt(), 0xfc93a039.toInt(),
            0x655b59c3, 0x8f0ccc92.toInt(), 0xffeff47d.toInt(), 0x85845dd1.toInt(),
            0x6fa87e4f, 0xfe2ce6e0.toInt(), 0xa3014314.toInt(), 0x4e0811a1,
            0xf7537e82.toInt(), 0xbd3af235.toInt(), 0x2ad7d2bb, 0xeb86d391.toInt()
        )

        val S = intArrayOf(
            7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
            5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
            4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
            6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
        )
    }

    init {
        reset()
    }

    fun reset() {
        state[0] = 0x67452301
        state[1] = 0xefcdab89.toInt()
        state[2] = 0x98badcfe.toInt()
        state[3] = 0x10325476
        count[0] = 0
        count[1] = 0
    }

    fun update(data: ByteArray, len: Int = data.size) {
        update(data, 0, len)
    }

    fun update(data: ByteArray, startIndex: Int, len: Int) {
        require(startIndex >= 0 && len >= 0 && startIndex + len <= data.size) {
            "Invalid range startIndex=$startIndex, len=$len, size=${data.size}"
        }

        var index = (count[0] ushr 3) and 0x3F
        val old0 = count[0]
        count[0] += len shl 3
        if (count[0].toUInt() < old0.toUInt()) count[1]++
        count[1] += len ushr 29

        val partLen = Md5BlockSize - index
        var i = 0

        if (len >= partLen) {
            for (j in 0 until partLen) {
                buffer[index + j] = data[startIndex + j]
            }
            transform(buffer, 0)

            i = partLen
            while (i + Md5BlockSize <= len) {
                transform(data, startIndex + i)
                i += Md5BlockSize
            }
            index = 0
        }

        if (i < len) {
            for (j in 0 until (len - i)) {
                buffer[index + j] = data[startIndex + i + j]
            }
        }
    }

    fun final(digest: ByteArray) {
        if (digest.size != Md5DigestSize) {
            throw IllegalArgumentException("Digest array must be of size $Md5DigestSize")
        }

        val bits = ByteArray(8)
        for (i in 0 until 8) {
            val byteIndex = i ushr 2
            val shift = (i and 3) * 8
            bits[i] = ((count[byteIndex] ushr shift) and 0xFF).toByte()
        }

        val index = (count[0] ushr 3) and 0x3F
        val padLen = if (index < 56) 56 - index else 120 - index
        update(Padding, padLen)
        update(bits, 8)

        for (i in 0 until 4) {
            val word = state[i]
            digest[i * 4] = (word and 0xFF).toByte()
            digest[i * 4 + 1] = ((word ushr 8) and 0xFF).toByte()
            digest[i * 4 + 2] = ((word ushr 16) and 0xFF).toByte()
            digest[i * 4 + 3] = ((word ushr 24) and 0xFF).toByte()
        }
    }

    private fun transform(block: ByteArray, offset: Int) {
        val m = IntArray(16)

        for (i in 0 until 16) {
            val idx = offset + i * 4
            m[i] = (block[idx].toInt() and 0xFF) or
                    ((block[idx + 1].toInt() and 0xFF) shl 8) or
                    ((block[idx + 2].toInt() and 0xFF) shl 16) or
                    ((block[idx + 3].toInt() and 0xFF) shl 24)
        }

        var a = state[0]
        var b = state[1]
        var c = state[2]
        var d = state[3]

        for (i in 0 until 64) {
            val (f, g) = when {
                i < 16 -> ((b and c) or (b.inv() and d)) to i
                i < 32 -> ((d and b) or (d.inv() and c)) to ((5 * i + 1) % 16)
                i < 48 -> (b xor c xor d) to ((3 * i + 5) % 16)
                else -> (c xor (b or d.inv())) to ((7 * i) % 16)
            }

            val temp = d
            d = c
            c = b
            b += rotateLeft(a + f + K[i] + m[g], S[i])
            a = temp
        }

        state[0] += a
        state[1] += b
        state[2] += c
        state[3] += d
    }

    private fun rotateLeft(x: Int, n: Int): Int = (x shl n) or (x ushr (32 - n))
}
