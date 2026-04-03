package org.ntqqrev.acidify.internal.util

import kotlinx.io.buffered
import kotlinx.io.readTo
import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.acidify.internal.crypto.hash.MD5Stream
import org.ntqqrev.acidify.internal.crypto.hash.SHA1Stream

private const val TRI_SHA1_SAMPLE_SIZE = 30 * 1024 * 1024
private const val TRI_SHA1_SLICE_SIZE = 10 * 1024 * 1024
private const val DEFAULT_SCAN_BUFFER_SIZE = 64 * 1024
private const val MD510M_SIZE = 10002432

internal class MediaSourceMetadata(
    val size: Long,
    val md5: ByteArray,
    val sha1: ByteArray,
    val triSha1: ByteArray,
    val md510M: ByteArray,
) {
    companion object {
        internal fun from(source: MediaSource): MediaSourceMetadata {
            val md5Stream = MD5Stream()
            val sha1Stream = SHA1Stream()
            val md510MStream = MD5Stream()

            val size = source.size
            val triSample = if (size <= TRI_SHA1_SAMPLE_SIZE.toLong()) {
                ByteArray(size.toInt())
            } else {
                ByteArray(TRI_SHA1_SAMPLE_SIZE)
            }

            val middleStart = if (size <= TRI_SHA1_SAMPLE_SIZE.toLong()) {
                0L
            } else {
                (size / 2) - (TRI_SHA1_SLICE_SIZE / 2).toLong()
            }
            val tailStart = if (size <= TRI_SHA1_SAMPLE_SIZE.toLong()) {
                0L
            } else {
                size - TRI_SHA1_SLICE_SIZE.toLong()
            }

            val rawSource = source.openRawSource()
            val bufferedSource = rawSource.buffered()
            val buffer = ByteArray(DEFAULT_SCAN_BUFFER_SIZE)
            var offset = 0L

            try {
                while (offset < size) {
                    val chunkSize = minOf(buffer.size.toLong(), size - offset).toInt()
                    bufferedSource.readTo(buffer, 0, chunkSize)

                    md5Stream.update(buffer, chunkSize)
                    sha1Stream.update(buffer, chunkSize)

                    val remainingMd510MBytes = MD510M_SIZE.toLong() - offset
                    if (remainingMd510MBytes > 0L) {
                        val md510MChunkSize = minOf(chunkSize.toLong(), remainingMd510MBytes).toInt()
                        md510MStream.update(buffer, 0, md510MChunkSize)
                    }

                    if (size <= TRI_SHA1_SAMPLE_SIZE.toLong()) {
                        buffer.copyInto(triSample, offset.toInt(), 0, chunkSize)
                    } else {
                        copyIntersection(
                            src = buffer,
                            srcOffset = offset,
                            srcLength = chunkSize,
                            dest = triSample,
                            destOffset = 0L,
                            rangeStart = 0L,
                            rangeEnd = TRI_SHA1_SLICE_SIZE.toLong()
                        )
                        copyIntersection(
                            src = buffer,
                            srcOffset = offset,
                            srcLength = chunkSize,
                            dest = triSample,
                            destOffset = TRI_SHA1_SLICE_SIZE.toLong(),
                            rangeStart = middleStart,
                            rangeEnd = middleStart + TRI_SHA1_SLICE_SIZE.toLong()
                        )
                        copyIntersection(
                            src = buffer,
                            srcOffset = offset,
                            srcLength = chunkSize,
                            dest = triSample,
                            destOffset = (TRI_SHA1_SLICE_SIZE * 2).toLong(),
                            rangeStart = tailStart,
                            rangeEnd = tailStart + TRI_SHA1_SLICE_SIZE.toLong()
                        )
                    }

                    offset += chunkSize
                }
            } finally {
                bufferedSource.close()
            }

            val md5 = ByteArray(MD5Stream.Md5DigestSize)
            md5Stream.final(md5)

            val sha1 = ByteArray(SHA1Stream.Sha1DigestSize)
            sha1Stream.final(sha1)

            val md510M = ByteArray(MD5Stream.Md5DigestSize)
            md510MStream.final(md510M)

            val payload = ByteArray(triSample.size + 8)
            triSample.copyInto(payload, 0, 0, triSample.size)
            for (i in 0 until 8) {
                payload[triSample.size + i] = ((size shr (i * 8)) and 0xFF).toByte()
            }

            return MediaSourceMetadata(
                size = size,
                md5 = md5,
                sha1 = sha1,
                triSha1 = payload.sha1(),
                md510M = md510M,
            )
        }
    }
}

private fun copyIntersection(
    src: ByteArray,
    srcOffset: Long,
    srcLength: Int,
    dest: ByteArray,
    destOffset: Long,
    rangeStart: Long,
    rangeEnd: Long,
) {
    val chunkEnd = srcOffset + srcLength
    val copyStart = maxOf(srcOffset, rangeStart)
    val copyEnd = minOf(chunkEnd, rangeEnd)
    if (copyStart >= copyEnd) {
        return
    }
    val srcStartIndex = (copyStart - srcOffset).toInt()
    val srcEndIndex = (copyEnd - srcOffset).toInt()
    val destStartIndex = (destOffset + (copyStart - rangeStart)).toInt()
    src.copyInto(dest, destStartIndex, srcStartIndex, srcEndIndex)
}
