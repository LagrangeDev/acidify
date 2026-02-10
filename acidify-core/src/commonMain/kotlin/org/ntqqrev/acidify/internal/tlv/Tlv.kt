package org.ntqqrev.acidify.internal.tlv

import kotlinx.io.readByteArray
import kotlinx.io.writeUByte
import kotlinx.io.writeUInt
import kotlinx.io.writeUShort
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.crypto.tea.TEA
import org.ntqqrev.acidify.internal.util.Prefix
import org.ntqqrev.acidify.internal.util.writeBytes
import org.ntqqrev.acidify.internal.util.writeString

internal class Tlv(val client: LagrangeClient) : TlvBuilder() {
    fun tlv18() = writeTlv(0x18u) {
        writeUShort(0u) // ping ver
        writeUInt(5u)
        writeUInt(0u)
        writeUInt(8001u) // app client ver
        writeUInt(client.sessionStore.uin.toUInt())
        writeUShort(0u)
        writeUShort(0u)
    }

    fun tlv100() = writeTlv(0x100u) {
        writeUShort(0u) // db buf ver
        writeUInt(5u) // sso ver, dont over 7
        writeInt(client.appInfo.appId)
        writeInt(client.appInfo.subAppId)
        writeInt(client.appInfo.appClientVersion) // app client ver
        writeInt(client.appInfo.mainSigMap)
    }

    fun tlv106A2() = writeTlv(0x106u) {
        writeBytes(client.sessionStore.encryptedA1)
    }

    fun tlv107() = writeTlv(0x107u) {
        writeUShort(1u) // pic type
        writeUByte(0x0du) // captcha type
        writeUShort(0u) // pic size
        writeUByte(1u) // ret type
    }

    fun tlv116() = writeTlv(0x116u) {
        writeUByte(0u)
        writeUInt(12058620u)
        writeInt(client.appInfo.subSigMap)
        writeUByte(0u)
    }

    fun tlv124() = writeTlv(0x124u) {
        writeBytes(ByteArray(12))
    }

    fun tlv128() = writeTlv(0x128u) {
        writeUShort(0u)
        writeUByte(0u) // guid new
        writeUByte(0u) // guid available
        writeUByte(0u) // guid changed
        writeUInt(0u) // guid flag
        writeString(client.appInfo.os, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        writeBytes(client.sessionStore.guid, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        writeString("", Prefix.UINT_16 or Prefix.LENGTH_ONLY) // brand
    }

    fun tlv141() = writeTlv(0x141u) {
        writeString("Unknown", Prefix.UINT_32 or Prefix.LENGTH_ONLY)
        writeUInt(0u)
    }

    fun tlv142() = writeTlv(0x142u) {
        writeUShort(0u)
        writeString(client.appInfo.packageName, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv144() = writeTlv(0x144u) {
        val tlvPack = Tlv(client).apply {
            tlv16e()
            tlv147()
            tlv128()
            tlv124()
        }

        writeBytes(TEA.encrypt(tlvPack.build().readByteArray(), client.sessionStore.tgtgt))
    }

    fun tlv145() = writeTlv(0x145u) {
        writeBytes(client.sessionStore.guid)
    }

    fun tlv147() = writeTlv(0x147u) {
        writeInt(client.appInfo.appId)
        writeString(client.appInfo.ptVersion, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        writeString(client.appInfo.packageName, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv166() = writeTlv(0x166u) {
        writeUByte(5u)
    }

    fun tlv16a() = writeTlv(0x16au) {
        writeBytes(client.sessionStore.noPicSig)
    }

    fun tlv16e() = writeTlv(0x16eu) {
        writeString(client.sessionStore.deviceName)
    }

    fun tlv177() = writeTlv(0x177u) {
        writeUByte(1u)
        writeUInt(0u)
        writeString(client.appInfo.wtLoginSdk, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv191() = writeTlv(0x191u) {
        writeUByte(0u)
    }

    fun tlv318() = writeTlv(0x318u) {
    }

    fun tlv521() = writeTlv(0x521u) {
        writeUInt(0x13u) // product type
        writeString("basicim", Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }
}