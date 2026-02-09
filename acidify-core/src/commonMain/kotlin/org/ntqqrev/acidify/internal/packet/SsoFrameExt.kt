package org.ntqqrev.acidify.internal.packet

import dev.karmakrafts.kompress.Inflater
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import org.ntqqrev.acidify.common.SsoResponse
import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.KuromeClient
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.crypto.tea.TeaProvider
import org.ntqqrev.acidify.internal.proto.system.SsoReservedFields
import org.ntqqrev.acidify.internal.proto.system.SsoSecureInfo
import org.ntqqrev.acidify.internal.util.*

internal val buildSsoFixedBytes = byteArrayOf(
    0x02, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00,
)

internal fun AbstractClient.buildSsoReserved(secureInfo: SsoSecureInfo?) = when (this) {
    is LagrangeClient -> SsoReservedFields(
        trace = generateTrace(),
        uid = uid,
        secureInfo = secureInfo,
    )

    is KuromeClient -> SsoReservedFields(
        trace = generateTrace(),
        uid = uid,
        msgType = 32,
        secureInfo = secureInfo,
        ntCoreVersion = 100,
    )
}.pbEncode()

internal fun AbstractClient.buildProtocol12(
    command: String,
    payload: ByteArray,
    sequence: Int,
    ssoSecureInfo: SsoSecureInfo?,
    encryptType: EncryptType,
): Buffer = Buffer().apply {
    barrier(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) {
        writeInt(12)
        writeByte(encryptType.underlying)
        writeBytes(
            when (encryptType) {
                EncryptType.WithD2Key -> d2
                else -> ByteArray(0)
            },
            Prefix.UINT_32 or Prefix.INCLUDE_PREFIX
        )
        writeByte(0) // unknown
        writeString(uin.toString(), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
        val sso = Buffer().apply {
            barrier(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) {
                writeInt(sequence)
                writeInt(subAppId)
                writeInt(2052)  // locale id
                writeBytes(buildSsoFixedBytes)
                writeBytes(a2, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
                writeString(command, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
                writeBytes(ByteArray(0), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) // unknown
                writeString(guid.toHexString(), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
                writeBytes(ByteArray(0), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) // unknown
                writeString(currentVersion, Prefix.UINT_16 or Prefix.INCLUDE_PREFIX)
                writeBytes(buildSsoReserved(ssoSecureInfo), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
            }
            writeBytes(payload, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
        }
        when (encryptType) {
            EncryptType.None -> transferFrom(sso)
            EncryptType.WithD2Key -> writeBytes(TeaProvider.encrypt(sso.readByteArray(), d2Key))
            EncryptType.WithEmptyKey -> writeBytes(TeaProvider.encrypt(sso.readByteArray(), ByteArray(16)))
        }
    }
}

internal fun AbstractClient.buildProtocol13(
    command: String,
    payload: ByteArray,
    sequence: Int,
    ssoSecureInfo: SsoSecureInfo?,
    encryptType: EncryptType,
): Buffer = Buffer().apply {
    barrier(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) {
        writeInt(13)
        writeByte(encryptType.underlying)
        writeInt(sequence)
        writeByte(0)
        writeString(uin.toString(), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
        val sso = Buffer().apply {
            barrier(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) {
                writeString(command, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
                writeInt(4) // uint 32 with prefix, ByteArray(0)
                writeBytes(buildSsoReserved(ssoSecureInfo), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
            }
            writeBytes(payload, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
        }
        when (encryptType) {
            EncryptType.None -> transferFrom(sso)
            EncryptType.WithD2Key -> writeBytes(TeaProvider.encrypt(sso.readByteArray(), d2Key))
            EncryptType.WithEmptyKey -> writeBytes(TeaProvider.encrypt(sso.readByteArray(), ByteArray(16)))
        }
    }
}

internal fun AbstractClient.parseSsoFrame(packet: ByteArray): SsoResponse {
    val rawReader = packet.reader()
    val protocol = rawReader.readUInt()
    val authFlag = rawReader.readByte()
    rawReader.readByte() // dummy
    rawReader.readPrefixedString(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) // uin
    if (protocol != 12u && protocol != 13u) throw Exception("Unrecognized protocol: $protocol")
    val encrypted = rawReader.readByteArray()
    val decrypted = when (authFlag) {
        EncryptType.None.underlying -> encrypted
        EncryptType.WithD2Key.underlying -> TeaProvider.decrypt(encrypted, d2Key)
        EncryptType.WithEmptyKey.underlying -> TeaProvider.decrypt(encrypted, ByteArray(16))
        else -> throw Exception("Unrecognized auth flag: $authFlag")
    }

    val reader = decrypted.reader()
    reader.readUInt() // headLen
    val sequence = reader.readUInt()
    val retCode = reader.readInt()
    val extra = reader.readPrefixedString(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
    val command = reader.readPrefixedString(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
    reader.readPrefixedBytes(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) // messageCookie
    val isCompressed = reader.readInt() == 1
    reader.readPrefixedBytes(Prefix.UINT_32) // reservedField
    var payload = reader.readPrefixedBytes(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)

    if (isCompressed) {
        payload = Inflater.inflate(payload, raw = false)
    }

    return if (retCode == 0) {
        SsoResponse(retCode, command, payload, sequence.toInt())
    } else {
        SsoResponse(retCode, command, payload, sequence.toInt(), extra)
    }
}