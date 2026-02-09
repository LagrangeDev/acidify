package org.ntqqrev.acidify.internal.tlv

import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.writeUShort
import org.ntqqrev.acidify.internal.util.Prefix
import org.ntqqrev.acidify.internal.util.barrier

internal abstract class TlvBuilder {
    protected val buffer = Buffer()
    protected var tlvCount = 0

    protected inline fun writeTlv(tag: UShort, tlv: Sink.() -> Unit) {
        tlvCount++
        buffer.writeUShort(tag)
        buffer.barrier(Prefix.UINT_16 or Prefix.LENGTH_ONLY) {
            tlv()
        }
    }

    fun build(): Buffer {
        return Buffer().apply {
            writeUShort(tlvCount.toUShort())
            transferFrom(this@TlvBuilder.buffer)
        }
    }
}