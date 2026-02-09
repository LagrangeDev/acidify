package org.ntqqrev.acidify.internal.service.system

import kotlinx.io.*
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.packet.buildCode2DPacket
import org.ntqqrev.acidify.internal.packet.buildWtLogin
import org.ntqqrev.acidify.internal.packet.parseCode2DPacket
import org.ntqqrev.acidify.internal.packet.parseWtLogin
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.Prefix
import org.ntqqrev.acidify.internal.util.readTlv
import org.ntqqrev.acidify.internal.util.reader
import org.ntqqrev.acidify.internal.util.writeBytes
import org.ntqqrev.acidify.struct.QRCodeState

internal object QueryQRCodeState : NoInputService<QRCodeState>("wtlogin.trans_emp") {
    override fun build(client: LagrangeClient, payload: Unit): ByteArray {
        val packet = Buffer().apply {
            writeUShort(0u)
            writeUInt(client.appInfo.appId.toUInt())
            writeBytes(client.sessionStore.qrSig, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
            writeULong(0u) // uin
            writeByte(0)
            writeBytes(ByteArray(0), Prefix.UINT_16 or Prefix.LENGTH_ONLY)
            writeUShort(0u)  // actually it is the tlv count, but there is no tlv so 0x0 is used
        }
        val code2d = client.buildCode2DPacket(packet.readByteArray(), 0x12)
        return client.buildWtLogin(code2d, 2066)
    }

    override fun parse(client: LagrangeClient, payload: ByteArray): QRCodeState {
        val wtlogin = parseWtLogin(payload)
        val reader = parseCode2DPacket(wtlogin).reader()
        val state = QRCodeState.fromByte(reader.readByte())
        if (state == QRCodeState.CONFIRMED) {
            reader.discard(4)
            client.sessionStore.uin = reader.readUInt().toLong()
            reader.discard(4)

            val tlv = reader.readTlv()
            client.sessionStore.tgtgt = tlv[0x1eu]!!
            client.sessionStore.encryptedA1 = tlv[0x18u]!!
            client.sessionStore.noPicSig = tlv[0x19u]!!
        }
        return state
    }
}