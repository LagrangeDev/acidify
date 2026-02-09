package org.ntqqrev.acidify.internal.util

import kotlinx.io.Buffer
import org.ntqqrev.acidify.exception.ServiceInternalException
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.internal.tlv.Tlv
import org.ntqqrev.acidify.internal.tlv.TlvQRCode

internal fun Service<*, *>.checkRetCode(retCode: Int, retMsg: String? = null) {
    if (retCode != 0) {
        throw ServiceInternalException(this, retCode, retMsg ?: "")
    }
}

internal inline fun LagrangeClient.buildTlv(block: Tlv.() -> Unit): Buffer {
    val tlv = Tlv(this)
    tlv.block()
    return tlv.build()
}

internal inline fun LagrangeClient.buildTlvQRCode(block: TlvQRCode.() -> Unit): Buffer {
    val tlv = TlvQRCode(this)
    block(tlv)
    return tlv.build()
}