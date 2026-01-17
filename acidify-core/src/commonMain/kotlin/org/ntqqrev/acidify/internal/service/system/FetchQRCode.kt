package org.ntqqrev.acidify.internal.service.system

import kotlinx.io.*
import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.ensureIsLagrange
import org.ntqqrev.acidify.internal.packet.login.TlvQRCode
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.Prefix
import org.ntqqrev.acidify.internal.util.readTlv
import org.ntqqrev.acidify.internal.util.reader
import org.ntqqrev.acidify.internal.util.writeBytes

internal object FetchQRCode : NoInputService<FetchQRCode.Result>("wtlogin.trans_emp") {
    override fun build(client: IClient, payload: Unit): ByteArray {
        client.ensureIsLagrange()
        val tlvPack = TlvQRCode(client).apply {
            tlv16()
            tlv1b()
            tlv1d()
            tlv33()
            tlv35()
            tlv66()
            tlvD1()
        }
        val packet = Buffer().apply {
            writeUShort(0u)
            writeUInt(client.appInfo.appId.toUInt())
            writeULong(0u) // uin
            writeBytes(ByteArray(0))
            writeByte(0)
            writeBytes(ByteArray(0), Prefix.UINT_16 or Prefix.LENGTH_ONLY)
            writeBytes(tlvPack.build())
        }
        return client.loginContext.buildCode2DPacket(packet.readByteArray(), 0x31u)
    }

    override fun parse(client: IClient, payload: ByteArray): Result {
        client.ensureIsLagrange()
        val wtLogin = client.loginContext.parseWtLogin(payload)
        val code2d = client.loginContext.parseCode2DPacket(wtLogin)
        val reader = code2d.reader()
        reader.discard(1)
        val sig = reader.readPrefixedBytes(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        val tlv = reader.readTlv()
        client.sessionStore.qrSig = sig
        val respD1Body = TlvQRCode.BodyD1Response(tlv.getValue(0xD1u))
        return Result(
            qrCodeUrl = respD1Body.get { qrCodeUrl },
            qrCodePng = tlv.getValue(0x17u)
        )
    }

    class Result(
        val qrCodeUrl: String,
        val qrCodePng: ByteArray
    )
}