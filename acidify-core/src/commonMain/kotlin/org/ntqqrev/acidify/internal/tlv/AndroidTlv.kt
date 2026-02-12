package org.ntqqrev.acidify.internal.tlv

import kotlinx.io.*
import org.ntqqrev.acidify.internal.KuromeClient
import org.ntqqrev.acidify.internal.crypto.tea.TEA
import org.ntqqrev.acidify.internal.proto.system.AndroidDeviceReport
import org.ntqqrev.acidify.internal.util.*
import kotlin.random.Random
import kotlin.time.Clock

internal class AndroidTlv(val client: KuromeClient) : TlvBuilder() {
    fun tlv1() = writeTlv(0x1u) {
        writeUShort(0x0001u)
        writeInt(Random.nextInt())
        writeUInt(client.sessionStore.uin.toUInt())
        writeUInt(Clock.System.now().epochSeconds.toUInt())
        writeUInt(0u) // dummy IP Address
        writeUShort(0x0000u)
    }

    fun tlv8() = writeTlv(0x8u) {
        writeUShort(0u)
        writeInt(2052) // locale_id
        writeUShort(0u)
    }

    fun tlv18() = writeTlv(0x18u) {
        writeUShort(0x0001u)
        writeUInt(0x00000600u)
        writeInt(client.appInfo.appId)
        writeInt(client.appInfo.appClientVersion)
        writeUInt(client.sessionStore.uin.toUInt())
        writeUShort(0x0000u)
        writeUShort(0x0000u)
    }

    fun tlv100(mainSigMap: UInt = client.appInfo.sdkInfo.mainSigMap.toUInt()) = writeTlv(0x100u) {
        writeUShort(1u) // db buf ver
        writeInt(client.appInfo.ssoVersion) // sso ver, dont over 7
        writeInt(client.appInfo.appId)
        writeInt(client.appInfo.subAppId)
        writeInt(client.appInfo.appClientVersion) // app client ver
        writeUInt(mainSigMap)
    }

    fun tlv104(verificationToken: ByteArray) = writeTlv(0x104u) {
        writeBytes(verificationToken)
    }

    fun tlv106Pwd(password: String = client.sessionStore.password) = writeTlv(0x106u) {
        val md5 = password.encodeToByteArray().md5()

        val key = Buffer().apply {
            writeBytes(md5)
            writeInt(0) // empty 4 bytes
            writeUInt(client.sessionStore.uin.toUInt())
        }.readByteArray().md5()

        val plain = Buffer().apply {
            writeShort(4) // TGTGT Version
            writeInt(Random.nextInt())
            writeInt(client.appInfo.ssoVersion)
            writeInt(client.appInfo.appId)
            writeInt(client.appInfo.appClientVersion)
            writeLong(client.sessionStore.uin)
            writeInt(Clock.System.now().epochSeconds.toInt())
            writeInt(0) // dummy IP Address
            writeByte(1)
            writeBytes(md5)
            writeBytes(client.sessionStore.wloginSigs.tgtgtKey)
            writeInt(0) // unknown
            writeByte(1) // guidAvailable
            writeBytes(client.sessionStore.guid)
            writeInt(client.appInfo.subAppId)
            writeInt(1) // flag
            writeString(client.sessionStore.uin.toString(), Prefix.UINT_16 or Prefix.LENGTH_ONLY)
            writeShort(0)
        }.readByteArray()

        writeBytes(TEA.encrypt(plain, key))
    }

    fun tlv106EncryptedA1() = writeTlv(0x106u) {
        writeBytes(client.sessionStore.wloginSigs.a1)
    }

    fun tlv107() = writeTlv(0x107u) {
        writeUShort(0u) // pic type
        writeUByte(0u) // captcha type
        writeUShort(0u) // pic size
        writeUByte(1u) // ret type
    }

    fun tlv109() = writeTlv(0x109u) {
        writeBytes(client.sessionStore.androidId.encodeToByteArray().md5())
    }

    fun tlv112(qid: String) = writeTlv(0x112u) {
        writeString(qid)
    }

    fun tlv116() = writeTlv(0x116u) {
        writeUByte(0u) // version
        writeInt(client.appInfo.sdkInfo.miscBitMap.toInt()) // miscBitMap
        writeInt(client.appInfo.sdkInfo.subSigMap.toInt())
        writeUByte(0u) // length of subAppId
    }

    fun tlv11b() = writeTlv(0x11bu) {
        writeUByte(2u)
    }

    fun tlv124() = writeTlv(0x124u) {
        writeString("android", Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        writeString("13", Prefix.UINT_16 or Prefix.LENGTH_ONLY) // os version
        writeShort(0x02) // network type
        writeString("", Prefix.UINT_16 or Prefix.LENGTH_ONLY) // sim info
        writeString("wifi", Prefix.UINT_32 or Prefix.LENGTH_ONLY) // apn
    }

    @Suppress("duplicatedCode")
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
        writeUShort(1u)
        writeString("", Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        writeString("", Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        writeString("wifi", Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv142() = writeTlv(0x142u) {
        writeUShort(0u)
        writeString(client.appInfo.packageName, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv144Report(useA1Key: Boolean = false) = writeTlv(0x144u) {
        val encrypted = AndroidTlv(client).apply {
            tlv109()
            tlv52d()
            tlv124()
            tlv128()
            tlv16e()
        }.build().readByteArray().let { nested ->
            TEA.encrypt(
                nested,
                if (useA1Key) client.sessionStore.wloginSigs.a1Key else client.sessionStore.wloginSigs.tgtgtKey
            )
        }
        writeBytes(encrypted)
    }

    fun tlv145() = writeTlv(0x145u) {
        writeBytes(client.sessionStore.guid)
    }

    fun tlv147() = writeTlv(0x147u) {
        writeInt(client.appInfo.appId)
        writeString(client.appInfo.ptVersion, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        writeBytes(client.appInfo.apkSignatureMd5, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv154() = writeTlv(0x154u) {
        writeInt(0) // seq
    }

    fun tlv166() = writeTlv(0x166u) {
        writeUByte(5u)
    }

    fun tlv16a() = writeTlv(0x16au) {
        writeBytes(client.sessionStore.wloginSigs.noPicSig)
    }

    fun tlv16e() = writeTlv(0x16eu) {
        writeString(client.sessionStore.deviceName)
    }

    fun tlv174(session: ByteArray) = writeTlv(0x174u) {
        writeBytes(session)
    }

    fun tlv177() = writeTlv(0x177u) {
        writeUByte(1u)
        writeInt(0) // sdk build time
        writeString(client.appInfo.sdkInfo.sdkVersion, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv17a() = writeTlv(0x17au) {
        writeInt(9)
    }

    fun tlv17c(code: String) = writeTlv(0x17cu) {
        writeString(code, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv187() = writeTlv(0x187u) {
        writeBytes(byteArrayOf(0x02, 0x00, 0x00, 0x00, 0x00, 0x00).md5())
    }

    fun tlv188() = writeTlv(0x188u) {
        writeBytes(client.sessionStore.androidId.encodeToByteArray().md5())
    }

    fun tlv191(k: UByte = 0u) = writeTlv(0x191u) {
        writeUByte(k)
    }

    fun tlv193(ticket: ByteArray) = writeTlv(0x193u) {
        writeBytes(ticket)
    }

    fun tlv197() = writeTlv(0x197u) {
        writeUByte(0u)
    }

    fun tlv198() = writeTlv(0x198u) {
        writeUByte(0u)
    }

    fun tlv318() = writeTlv(0x318u) {
    }

    fun tlv400() = writeTlv(0x400u) {
        val randomKey = Random.nextBytes(16)
        val randSeed = Random.nextBytes(8)
        val plain = Buffer().apply {
            writeShort(1)
            writeLong(client.sessionStore.uin)
            writeBytes(client.sessionStore.guid)
            writeBytes(randomKey)
            writeInt(16)
            writeInt(1)
            writeUInt(Clock.System.now().epochSeconds.toUInt())
            writeBytes(randSeed)
        }.readByteArray()

        writeBytes(TEA.encrypt(plain, client.sessionStore.guid))
    }

    fun tlv401() = writeTlv(0x401u) {
        writeBytes(Random.nextBytes(16))
    }

    fun tlv511() = writeTlv(0x511u) {
        val domains = arrayOf(
            "office.qq.com",
            "qun.qq.com",
            "gamecenter.qq.com",
            "docs.qq.com",
            "mail.qq.com",
            "tim.qq.com",
            "ti.qq.com",
            "vip.qq.com",
            "tenpay.com",
            "qqweb.qq.com",
            "qzone.qq.com",
            "mma.qq.com",
            "game.qq.com",
            "openmobile.qq.com",
            "connect.qq.com"
        )

        writeUShort(domains.size.toUShort())
        domains.forEach { domain ->
            writeUByte(1u)
            writeString(domain, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        }
    }

    fun tlv516() = writeTlv(0x516u) {
        writeInt(0)
    }

    fun tlv521() = writeTlv(0x521u) {
        writeInt(0)
        writeString("", Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv525() = writeTlv(0x525u) {
        writeShort(1) // tlvCount
        writeShort(0x536) // tlv536
        writeBytes(byteArrayOf(0x02, 0x01, 0x00), Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }

    fun tlv52d() = writeTlv(0x52du) {
        writeBytes(
            AndroidDeviceReport(
                bootId = "unknown",
                procVersion = "Linux version 4.19.157-perf-g92c089fc2d37 (builder@pangu-build-component-vendor-272092-qncbv-vttl3-61r9m) (clang version 10.0.7 for Android NDK, GNU ld (binutils-2.27-bd24d23f) 2.27.0.20170315) 1 SMP PREEMPT Wed Jun 5 13:27:08 UTC 2024",
                codeName = "REL",
                bootloader = "V816.0.6.0.TKHCNXM",
                fingerprint = "Redmi/alioth/alioth:13/TKQ1.221114.001/V816.0.6.0.TKHCNXM:user/release-keys",
                androidId = client.sessionStore.androidId,
                baseBand = "",
                innerVersion = "V816.0.6.0.TKHCNXM"
            ).pbEncode()
        )
    }

    fun tlv544(energy: ByteArray) = writeTlv(0x544u) {
        writeBytes(energy)
    }

    fun tlv545() = writeTlv(0x545u) {
        writeString(client.sessionStore.qimei)
    }

    fun tlv547(clientPow: ByteArray) = writeTlv(0x547u) {
        writeBytes(clientPow)
    }

    fun tlv548(nativeGetTestData: ByteArray) = writeTlv(0x548u) {
        writeBytes(nativeGetTestData)
    }

    fun tlv553(fekitAttach: ByteArray) = writeTlv(0x553u) {
        writeBytes(fekitAttach)
    }
}