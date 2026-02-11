package org.ntqqrev.acidify.internal.crypto.pow

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.writeUShort
import org.ntqqrev.acidify.internal.crypto.hash.SHA256
import org.ntqqrev.acidify.internal.math.BigInt
import org.ntqqrev.acidify.internal.util.Prefix
import org.ntqqrev.acidify.internal.util.reader
import org.ntqqrev.acidify.internal.util.writeBytes
import kotlin.random.Random
import kotlin.time.TimeSource

internal object POW {
    fun generateTlv547(tlv546: ByteArray): ByteArray {
        val reader = tlv546.reader()

        val version = reader.readByte()
        val typ = reader.readByte()
        val hashType = reader.readByte()
        var ok = reader.readByte().toInt() == 0
        val maxIndex = reader.readUShort()
        val reserved = reader.readByteArray(2)

        val src = reader.readPrefixedBytes(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        val tgt = reader.readPrefixedBytes(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        val cpy = reader.readPrefixedBytes(Prefix.UINT_16 or Prefix.LENGTH_ONLY)

        val dst: ByteArray
        val elapsed: Int
        var cnt = 0

        var inputNum = BigInt.fromBytes(src, bigEndian = true)
        if (tgt.size == 32) {
            val mark = TimeSource.Monotonic.markNow()
            var hash = SHA256.hash(inputNum.toBytes(bigEndian = true))

            while (!hash.contentEquals(tgt)) {
                inputNum = inputNum + BigInt.ONE
                hash = SHA256.hash(inputNum.toBytes(bigEndian = true))
                cnt++

                if (cnt > 6_000_000) {
                    throw IllegalStateException("Calculating PoW cost too much time, maybe something wrong")
                }
            }

            ok = true
            dst = inputNum.toBytes(bigEndian = true)
            elapsed = mark.elapsedNow().inWholeMilliseconds.toInt()
        } else {
            throw IllegalStateException("Only support SHA256 PoW")
        }

        return Buffer().apply {
            writeByte(version)
            writeByte(typ)
            writeByte(hashType)
            writeByte((if (ok) 1 else 0))
            writeUShort(maxIndex)
            writeBytes(reserved)
            writeBytes(src, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
            writeBytes(tgt, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
            writeBytes(cpy, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
            writeBytes(dst, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
            writeInt(elapsed)
            writeInt(cnt)
        }.readByteArray()
    }

    fun generateTlv548(): ByteArray {
        val src = Random.nextBytes(128).also { it[0] = 21 }

        val srcNum = BigInt.fromBytes(src, bigEndian = true)
        val cnt = 10_000
        val dstNum = srcNum + BigInt(cnt)
        val dst = dstNum.toBytes(bigEndian = true)
        val tgt = SHA256.hash(dst)

        val header = Buffer().apply {
            writeByte(1) // version
            writeByte(2) // typ
            writeByte(1) // hashType
            writeByte(2) // ok
            writeUShort(10u) // maxIndex
            writeBytes(ByteArray(2)) // reserveBytes
            writeBytes(src, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
            writeBytes(tgt, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        }.readByteArray()

        val tlv546 = Buffer().apply {
            writeBytes(header)
            writeBytes(header, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        }.readByteArray()

        return generateTlv547(tlv546)
    }
}
