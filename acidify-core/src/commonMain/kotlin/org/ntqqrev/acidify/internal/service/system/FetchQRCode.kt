package org.ntqqrev.acidify.internal.service.system

import kotlinx.io.*
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.packet.EncryptType
import org.ntqqrev.acidify.internal.packet.buildCode2DPacket
import org.ntqqrev.acidify.internal.packet.buildWtLogin
import org.ntqqrev.acidify.internal.packet.parseCode2DPacket
import org.ntqqrev.acidify.internal.packet.parseWtLogin
import org.ntqqrev.acidify.internal.proto.login.TlvQRCodeBodyD1Resp
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.tlv.buildTlvQRCode
import org.ntqqrev.acidify.internal.util.*

internal object FetchQRCode : NoInputService<FetchQRCode.Result>("wtlogin.trans_emp") {
    override val ssoEncryptType = EncryptType.WithEmptyKey

    override fun build(client: LagrangeClient, payload: Unit): ByteArray {
        val tlvPack = client.buildTlvQRCode {
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
            writeBytes(tlvPack)
        }
        val code2d = client.buildCode2DPacket(packet.readByteArray(), 0x31)
        return client.buildWtLogin(code2d, 2066)
    }

    override fun parse(client: LagrangeClient, payload: ByteArray): Result {
        val wtLogin = parseWtLogin(payload)
        val code2d = parseCode2DPacket(wtLogin)
        val reader = code2d.reader()
        reader.discard(1)
        val sig = reader.readPrefixedBytes(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        val tlv = reader.readTlv()
        client.sessionStore.qrSig = sig
        val respD1Body = tlv[0xD1u]!!.pbDecode<TlvQRCodeBodyD1Resp>()
        return Result(
            qrCodeUrl = respD1Body.qrCodeUrl,
            qrCodePng = tlv[0x17u]!!
        )
    }

    class Result(
        val qrCodeUrl: String,
        val qrCodePng: ByteArray
    )
}