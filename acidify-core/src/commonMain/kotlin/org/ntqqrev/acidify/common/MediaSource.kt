package org.ntqqrev.acidify.common

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlin.js.JsExport

@JsExport
abstract class MediaSource {
    abstract val size: Long

    abstract fun openRawSource(): RawSource

    abstract fun dispose()

    open fun readByteArray(): ByteArray {
        return openRawSource().buffered().use { source ->
            source.readByteArray()
        }
    }

    class Bytes(private val data: ByteArray) : MediaSource() {
        override val size: Long
            get() = data.size.toLong()

        override fun openRawSource(): RawSource {
            return ByteArrayRawSource(data)
        }

        override fun dispose() {
            // No-op, let GC handle it
        }

        override fun readByteArray(): ByteArray {
            return data // reduce reallocation
        }

        override fun toString(): String {
            return "MediaSource.Bytes(size=$size)"
        }
    }

    companion object {
        @JsExport.Ignore
        fun ByteArray.toMediaSource(): MediaSource = Bytes(this)
    }
}

private class ByteArrayRawSource(
    private val data: ByteArray,
) : RawSource {
    private var offset: Int = 0
    private var closed: Boolean = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "Source is closed." }
        require(byteCount >= 0L) { "byteCount: $byteCount" }

        if (offset >= data.size) {
            return -1L
        }

        val readCount = minOf(byteCount, (data.size - offset).toLong()).toInt()
        sink.write(data, offset, offset + readCount)
        offset += readCount
        return readCount.toLong()
    }

    override fun close() {
        closed = true
    }
}