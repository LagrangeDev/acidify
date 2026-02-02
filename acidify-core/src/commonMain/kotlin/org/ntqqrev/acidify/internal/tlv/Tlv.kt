package org.ntqqrev.acidify.internal.tlv

import kotlinx.io.*
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.crypto.tea.TeaProvider
import org.ntqqrev.acidify.internal.util.Prefix
import org.ntqqrev.acidify.internal.util.barrier
import org.ntqqrev.acidify.internal.util.writeBytes
import org.ntqqrev.acidify.internal.util.writeString
import kotlin.random.Random
import kotlin.time.Clock

internal class Tlv(val client: LagrangeClient) {
    private val builder = Buffer()

    private var tlvCount: UShort = 0u

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

    fun tlv106(md5pass: ByteArray) = writeTlv(0x106u) {
        val body = Buffer().apply {
            writeUShort(4u) // tgtgt ver
            writeBytes(Random.nextBytes(4)) // crypto.randomBytes(4)
            writeUInt(0u) // sso ver
            writeInt(client.appInfo.appId)
            writeInt(8001) // app client ver
            writeULong(client.sessionStore.uin.toULong())
            writeInt(Clock.System.now().epochSeconds.toInt())
            writeUInt(0u) // dummy ip
            writeByte(1) // save password
            writeBytes(md5pass)
            writeBytes(client.sessionStore.a2)
            writeUInt(0u)
            writeByte(1) // guid available
            writeBytes(client.sessionStore.guid)
            writeUInt(1u)
            writeUInt(1u) // login type password
            writeString(client.sessionStore.uin.toString(), Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        }

        val buf = Buffer()

        buf.writeInt(client.sessionStore.uin.toInt())
        buf.writeBytes(ByteArray(4))
        buf.writeBytes(md5pass)

        writeBytes(TeaProvider.encrypt(body.readByteArray(), buf.readByteArray()))
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

        writeBytes(TeaProvider.encrypt(tlvPack.build(), client.sessionStore.tgtgt))
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

    fun build(): ByteArray = Buffer().apply {
        writeUShort(tlvCount)
        writeBytes(builder.readByteArray())
    }.readByteArray()

    private fun writeTlv(tag: UShort, tlv: Sink.() -> Unit) {
        tlvCount++

        builder.writeUShort(tag)
        builder.barrier(Prefix.UINT_16 or Prefix.LENGTH_ONLY) {
            tlv()
        }
    }
}

internal inline fun LagrangeClient.buildTlv(block: Tlv.() -> Unit): ByteArray {
    val tlv = Tlv(this)
    tlv.block()
    return tlv.build()
}