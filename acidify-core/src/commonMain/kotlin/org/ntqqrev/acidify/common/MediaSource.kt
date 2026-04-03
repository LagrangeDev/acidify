package org.ntqqrev.acidify.common

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlin.js.JsExport

@JsExport
class MediaSource internal constructor(
    val size: Long,
    private val opener: () -> RawSource,
) {
    init {
        require(size >= 0L) { "size must be non-negative" }
    }

    internal fun openRawSource(): RawSource = opener()

    internal fun readByteArray(): ByteArray {
        return openRawSource().buffered().use { source ->
            source.readByteArray()
        }
    }

    companion object {
        fun fromByteArray(data: ByteArray): MediaSource = MediaSource(data.size.toLong()) {
            ByteArrayRawSource(data)
        }

        @JsExport.Ignore
        fun ByteArray.toMediaSource(): MediaSource = fromByteArray(this)
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