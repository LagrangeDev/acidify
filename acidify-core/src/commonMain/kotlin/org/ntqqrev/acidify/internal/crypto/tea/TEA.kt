package org.ntqqrev.acidify.internal.crypto.tea

import kotlin.random.Random

internal object TEA {
    /**
     * Get the cipher length for given data size
     * @param dataSize Size of data to encrypt
     * @return Required cipher buffer size
     */
    fun getCipherLength(dataSize: Int): Int {
        return (10 - ((dataSize + 1) and 7)) + dataSize + 7
    }

    /**
     * Get the plain length from cipher size
     * @param dataSize Size of encrypted data
     * @return Plain data size
     */
    fun getPlainLength(dataSize: Int): Int {
        return dataSize - ((dataSize and 7) + 3) - 7
    }

    fun encrypt(data: ByteArray, key: ByteArray): ByteArray = TeaSession().encrypt(data, key)
    fun decrypt(data: ByteArray, key: ByteArray): ByteArray = TeaSession().decrypt(data, key)
}

private class TeaSession {
    private var contextStart = 0
    private var crypt = 0
    private var key: ByteArray = ByteArray(0)
    private var out: ByteArray = ByteArray(0)
    private var padding = 0
    private var plain: ByteArray = ByteArray(0)
    private var pos = 0
    private var preCrypt = 0
    private var prePlain: ByteArray = ByteArray(0)
    private var header = true

    private fun decipher(bArr: ByteArray?, i2: Int = 0): ByteArray {
        var i3 = 16
        var unsignedInt = getUnsignedInt(bArr, i2, 4)
        var unsignedInt2 = getUnsignedInt(bArr, i2 + 4, 4)
        val unsignedInt3 = getUnsignedInt(this.key, 0, 4)
        val unsignedInt4 = getUnsignedInt(this.key, 4, 4)
        val unsignedInt5 = getUnsignedInt(this.key, 8, 4)
        val unsignedInt6 = getUnsignedInt(this.key, 12, 4)
        var j2 = J_2
        while (true) {
            val i4 = i3 - 1
            if (i3 <= 0) {
                return unsignedInt.toInt().encodeToBigEndian() + unsignedInt2.toInt().encodeToBigEndian()
            }
            unsignedInt2 =
                (unsignedInt2 - ((((unsignedInt shl 4) + unsignedInt5) xor (unsignedInt + j2)) xor ((unsignedInt ushr 5) + unsignedInt6))) and FFF
            unsignedInt =
                (unsignedInt - ((((unsignedInt2 shl 4) + unsignedInt3) xor (unsignedInt2 + j2)) xor ((unsignedInt2 ushr 5) + unsignedInt4))) and FFF
            j2 = (j2 - J_1) and FFF
            i3 = i4
        }
    }

    private fun decrypt8Bytes(bArr: ByteArray, i3: Int): Boolean {
        val i2 = 0
        this.pos = 0
        while (true) {
            val i4 = this.pos
            if (i4 < 8) {
                if (this.contextStart + i4 >= i3) {
                    return true
                }
                val bArr2 = this.prePlain
                bArr2[i4] = (bArr2[i4].toInt() xor bArr[this.crypt + i2 + i4].toInt()).toByte()
                this.pos = i4 + 1
            } else {
                val decipher = decipher(this.prePlain)
                this.prePlain = decipher
                this.contextStart += 8
                this.crypt += 8
                this.pos = 0
                return true
            }
        }
    }

    private fun encipher(bArr: ByteArray): ByteArray {
        var i2 = 16
        var unsignedInt = getUnsignedInt(bArr, 0, 4)
        var unsignedInt2 = getUnsignedInt(bArr, 4, 4)
        val unsignedInt3 = getUnsignedInt(this.key, 0, 4)
        val unsignedInt4 = getUnsignedInt(this.key, 4, 4)
        val unsignedInt5 = getUnsignedInt(this.key, 8, 4)
        val unsignedInt6 = getUnsignedInt(this.key, 12, 4)
        var j2: Long = 0
        while (true) {
            val i3 = i2 - 1
            if (i2 <= 0) {
                return unsignedInt.toInt().encodeToBigEndian() + unsignedInt2.toInt().encodeToBigEndian()
            }
            j2 = (j2 + J_1) and FFF
            unsignedInt =
                (unsignedInt + ((((unsignedInt2 shl 4) + unsignedInt3) xor (unsignedInt2 + j2)) xor ((unsignedInt2 ushr 5) + unsignedInt4))) and FFF
            unsignedInt2 =
                (unsignedInt2 + ((((unsignedInt shl 4) + unsignedInt5) xor (unsignedInt + j2)) xor ((unsignedInt ushr 5) + unsignedInt6))) and FFF
            i2 = i3
        }
    }

    private fun encryptRange(bArr: ByteArray, bArr2: ByteArray): ByteArray {
        var i2 = 0
        var i3 = bArr.size
        var i4: Int
        val bArr3 = ByteArray(8)
        this.plain = bArr3
        this.prePlain = ByteArray(8)
        this.pos = 1
        this.padding = 0
        this.preCrypt = 0
        this.crypt = 0
        this.key = bArr2
        this.header = true
        val i5 = (i3 + 10) % 8
        this.pos = i5
        if (i5 != 0) {
            this.pos = 8 - i5
        }
        this.out = ByteArray(this.pos + i3 + 10)
        bArr3[0] = ((rand() and 248) or this.pos).toByte()
        var i6 = 1
        while (true) {
            i4 = this.pos
            if (i6 > i4) {
                break
            }
            plain[i6] = (rand() and 255).toByte()
            i6++
        }
        this.pos = i4 + 1
        for (i7 in 0..7) {
            prePlain[i7] = 0
        }
        this.padding = 1
        while (this.padding <= 2) {
            val i8 = this.pos
            if (i8 < 8) {
                val bArr4 = this.plain
                this.pos = i8 + 1
                bArr4[i8] = (rand() and 255).toByte()
                padding++
            }
            if (this.pos == 8) {
                encrypt8Bytes()
            }
        }
        while (i3 > 0) {
            val i9 = this.pos
            if (i9 < 8) {
                val bArr5 = this.plain
                this.pos = i9 + 1
                bArr5[i9] = bArr[i2]
                i3--
                i2++
            }
            if (this.pos == 8) {
                encrypt8Bytes()
            }
        }
        this.padding = 1
        while (true) {
            val i10 = this.padding
            if (i10 <= 7) {
                val i11 = this.pos
                if (i11 < 8) {
                    val bArr6 = this.plain
                    this.pos = i11 + 1
                    bArr6[i11] = 0
                    this.padding = i10 + 1
                }
                if (this.pos == 8) {
                    encrypt8Bytes()
                }
            } else {
                return this.out
            }
        }
    }

    private fun encrypt8Bytes() {
        this.pos = 0
        while (true) {
            val i2 = this.pos
            if (i2 >= 8) {
                break
            }
            if (this.header) {
                val bArr = this.plain
                bArr[i2] = (bArr[i2].toInt() xor prePlain[i2].toInt()).toByte()
            } else {
                val bArr2 = this.plain
                bArr2[i2] = (bArr2[i2].toInt() xor out[preCrypt + i2].toInt()).toByte()
            }
            this.pos = i2 + 1
        }
        encipher(this.plain).copyInto(this.out, this.crypt, 0, 8)
        this.pos = 0
        while (true) {
            val i3 = this.pos
            if (i3 < 8) {
                val bArr3 = this.out
                val i4 = this.crypt + i3
                bArr3[i4] = (bArr3[i4].toInt() xor prePlain[i3].toInt()).toByte()
                this.pos = i3 + 1
            } else {
                this.plain.copyInto(this.prePlain, 0, 0, 8)
                val i5 = this.crypt
                this.preCrypt = i5
                this.crypt = i5 + 8
                this.pos = 0
                this.header = false
                return
            }
        }
    }

    private fun rand(): Int {
        return Random.nextInt()
    }

    fun decryptRange(bArr: ByteArray, bArr2: ByteArray): ByteArray? {
        val i2 = 0
        val i3 = bArr.size
        var i4 = 0
        this.preCrypt = 0
        this.crypt = 0
        this.key = bArr2
        val i5 = i2 + 8
        var bArr3 = ByteArray(i5)
        if (i3 % 8 != 0 || i3 < 16) {
            return null
        }
        val decipher = decipher(bArr, i2)
        this.prePlain = decipher
        val i6 = decipher[0].toInt() and 7
        this.pos = i6
        var i7 = (i3 - i6) - 10
        if (i7 < 0) {
            return null
        }
        for (i8 in i2 until i5) {
            bArr3[i8] = 0
        }
        this.out = ByteArray(i7)
        this.preCrypt = 0
        this.crypt = 8
        this.contextStart = 8
        pos++
        this.padding = 1
        while (true) {
            val i9 = this.padding
            if (i9 <= 2) {
                val i10 = this.pos
                if (i10 < 8) {
                    this.pos = i10 + 1
                    this.padding = i9 + 1
                }
                if (this.pos == 8) {
                    if (!decrypt8Bytes(bArr, i3)) {
                        return null
                    }
                    bArr3 = bArr
                }
            } else {
                while (i7 != 0) {
                    val i11 = this.pos
                    if (i11 < 8) {
                        out[i4] = (bArr3[this.preCrypt + i2 + i11].toInt() xor prePlain[i11].toInt()).toByte()
                        i4++
                        i7--
                        this.pos = i11 + 1
                    }
                    if (this.pos == 8) {
                        this.preCrypt = this.crypt - 8
                        if (!decrypt8Bytes(bArr, i3)) {
                            return null
                        }
                        bArr3 = bArr
                    }
                }
                this.padding = 1
                while (this.padding < 8) {
                    val i12 = this.pos
                    if (i12 < 8) {
                        if ((bArr3[this.preCrypt + i2 + i12].toInt() xor prePlain[i12].toInt()) != 0) {
                            return null
                        }
                        this.pos = i12 + 1
                    }
                    if (this.pos == 8) {
                        this.preCrypt = this.crypt
                        if (!decrypt8Bytes(bArr, i3)) {
                            return null
                        }
                        bArr3 = bArr
                    }
                    padding++
                }
                return this.out
            }
        }
    }

    fun decrypt(bArr: ByteArray, bArr2: ByteArray): ByteArray {
        return decryptRange(bArr, bArr2) ?: throw RuntimeException("decrypt failed")
    }

    fun encrypt(bArr: ByteArray, bArr2: ByteArray): ByteArray {
        return encryptRange(bArr, bArr2)
    }

    companion object {
        private const val FFF = 0xffffffffL
        private const val J_1 = 2654435769L
        private const val J_2 = 3816266640L
        private fun getUnsignedInt(bArr: ByteArray?, i2: Int, i3: Int): Long {
            var i2 = i2
            val i4 = if (i3 > 4) i2 + 4 else i3 + i2
            var j2: Long = 0
            while (i2 < i4) {
                j2 = (j2 shl 8) or (bArr!![i2].toInt() and 255).toLong()
                i2++
            }
            return FFF and j2
        }
    }
}

private fun Int.encodeToBigEndian(): ByteArray {
    val result = ByteArray(4)
    result[0] = (this ushr 24).toByte()
    result[1] = (this ushr 16).toByte()
    result[2] = (this ushr 8).toByte()
    result[3] = this.toByte()
    return result
}