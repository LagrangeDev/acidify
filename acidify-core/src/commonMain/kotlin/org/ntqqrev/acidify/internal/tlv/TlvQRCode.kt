package org.ntqqrev.acidify.internal.tlv

import kotlinx.io.writeUByte
import kotlinx.io.writeUInt
import kotlinx.io.writeUShort
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.login.TlvQRCodeBodyD1Req
import org.ntqqrev.acidify.internal.util.Prefix
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.internal.util.writeBytes
import org.ntqqrev.acidify.internal.util.writeString

internal class TlvQRCode(val client: LagrangeClient) : TlvBuilder() {
    fun tlv16() = writeTlv(0x16u) {
        writeUInt(0u)
        writeInt(client.appInfo.appId)
        writeInt(client.appInfo.subAppId)
        writeBytes(client.sessionStore.guid)
        writeString(client.appInfo.packageName, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        writeString(client.appInfo.ptVersion, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        writeString(client.appInfo.packageName, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv1b() = writeTlv(0x1bu) {
        writeUInt(0u) // micro
        writeUInt(0u) // version
        writeUInt(3u) // size
        writeUInt(4u) // margin
        writeUInt(72u) // dpi
        writeUInt(2u) // eclevel
        writeUInt(2u) // hint
        writeUShort(0u) // unknown
    }

    fun tlv1d() = writeTlv(0x1du) {
        writeUByte(1u)
        writeInt(client.appInfo.mainSigMap) // misc bitmap
        writeUInt(0u)
        writeUByte(0u)
    }

    fun tlv33() = writeTlv(0x33u) {
        writeBytes(client.sessionStore.guid)
    }

    fun tlv35() = writeTlv(0x35u) {
        writeInt(client.appInfo.ssoVersion)
    }

    fun tlv66() = writeTlv(0x66u) {
        writeInt(client.appInfo.ssoVersion)
    }

    fun tlvD1() = writeTlv(0xd1u) {
        val body = TlvQRCodeBodyD1Req(
            system = TlvQRCodeBodyD1Req.System(
                os = client.appInfo.os,
                deviceName = client.sessionStore.deviceName
            ),
            typeBuf = "3001".hexToByteArray(),
        ).pbEncode()
        writeBytes(body)
    }
}