package org.ntqqrev.acidify.internal.util

import io.ktor.utils.io.core.*
import kotlinx.io.*
import kotlinx.io.Buffer
import org.ntqqrev.acidify.internal.crypto.hash.MD5
import org.ntqqrev.acidify.internal.crypto.hash.SHA1

// ======== Hash Functions ========

internal fun ByteArray.md5(): ByteArray = MD5.hash(this)

internal fun ByteArray.sha1(): ByteArray = SHA1.hash(this)

// ======== ByteArray Extensions ========

internal fun ByteArray.writeUInt32BE(value: Long, offset: Int) {
    this[offset] = (value ushr 24).toByte()
    this[offset + 1] = (value ushr 16).toByte()
    this[offset + 2] = (value ushr 8).toByte()
    this[offset + 3] = value.toByte()
}

internal fun ByteArray.readUInt32BE(offset: Int): Long {
    return ((this[offset].toUInt() shl 24) or
            ((this[offset + 1].toUInt() and 0xffu) shl 16) or
            ((this[offset + 2].toUInt() and 0xffu) shl 8) or
            (this[offset + 3].toUInt() and 0xffu)).toLong()
}

internal fun ByteArray.reader() = BinaryReader(this)

internal fun Sink.writeString(value: String, prefix: Prefix = Prefix.NONE) {
    this.writeLength(value.length.toUInt(), prefix)
    this.writeText(value)
}

internal fun Sink.writeBytes(value: ByteArray, prefix: Prefix = (Prefix.NONE)) {
    this.writeLength(value.size.toUInt(), prefix)
    this.writeFully(value)
}

// ======== Tlv Operations ========

internal fun BinaryReader.readTlv(): Map<UShort, ByteArray> {
    val tlvCount = readUShort()
    val result = mutableMapOf<UShort, ByteArray>()
    repeat(tlvCount.toInt()) {
        val tag = readUShort()
        val length = readUShort()
        val value = readByteArray(length.toInt())

        result[tag] = value
    }

    return result
}

internal fun ByteArray.parseTlv() = this.reader().readTlv()

internal inline fun Sink.barrier(prefix: Prefix, addition: Int = 0, target: ((Sink).() -> Unit)) {
    val written = Buffer()
    target(written)

    writeLength(written.size.toUInt() + addition.toUInt(), prefix)
    writePacket(written.build())
}

internal fun Source.readPrefixedString(prefix: Prefix): String {
    val length = readLength(prefix)
    return readByteArray(length.toInt()).decodeToString()
}

internal fun Source.readPrefixedBytes(prefix: Prefix): ByteArray {
    val length = readLength(prefix)
    return this.readByteArray(length.toInt())
}

private fun Sink.writeLength(length: UInt, prefix: Prefix) {
    val prefixLength = prefix.getPrefixLength()
    val includePrefix = prefix.isIncludePrefix()
    val writtenLength = length + (if (includePrefix) prefixLength else 0).toUInt()

    when (prefixLength) {
        1 -> this.writeByte(writtenLength.toByte())
        2 -> this.writeUShort(writtenLength.toUShort())
        4 -> this.writeUInt(writtenLength)
        else -> {}
    }
}

private fun Source.readLength(prefix: Prefix): UInt {
    val prefixLength = prefix.getPrefixLength()
    val includePrefix = prefix.isIncludePrefix()

    return when (prefixLength) {
        1 -> this.readByte().toUInt() - (if (includePrefix) prefixLength else 0).toUInt()
        2 -> this.readUShort().toUInt() - (if (includePrefix) prefixLength else 0).toUInt()
        4 -> this.readUInt() - (if (includePrefix) prefixLength else 0).toUInt()
        else -> 0u
    }
}

// ======== Int Operations ========

internal fun Source.readShortLittleEndian(): Short {
    val value = this.readShort()
    return if (value.toInt() < 0) (value.toInt() + Short.MAX_VALUE * 2).toShort() else value
}

internal fun Source.readIntLittleEndian(): Int {
    val value = this.readInt()
    return if (value < 0) (value + Int.MAX_VALUE * 2) else value
}

internal fun Int.encodeToBigEndian(): ByteArray {
    val result = ByteArray(4)
    result[0] = (this ushr 24).toByte()
    result[1] = (this ushr 16).toByte()
    result[2] = (this ushr 8).toByte()
    result[3] = this.toByte()
    return result
}

internal fun Int.encodeToLittleEndian(): IntArray {
    val result = IntArray(4)
    result[0] = this and 0xFF
    result[1] = (this ushr 8) and 0xFF
    result[2] = (this ushr 16) and 0xFF
    result[3] = (this ushr 24) and 0xFF
    return result
}

internal fun Int.toIpString(): String {
    val byte1 = this and 0xFF
    val byte2 = (this ushr 8) and 0xFF
    val byte3 = (this ushr 16) and 0xFF
    val byte4 = (this ushr 24) and 0xFF
    return "$byte1.$byte2.$byte3.$byte4"
}
