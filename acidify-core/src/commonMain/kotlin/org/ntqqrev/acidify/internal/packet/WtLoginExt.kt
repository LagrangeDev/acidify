package org.ntqqrev.acidify.internal.packet

import io.ktor.util.date.*
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.writeUInt
import kotlinx.io.writeUShort
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.crypto.ecdh.Ecdh
import org.ntqqrev.acidify.internal.crypto.tea.TeaProvider
import org.ntqqrev.acidify.internal.util.Prefix
import org.ntqqrev.acidify.internal.util.barrier
import org.ntqqrev.acidify.internal.util.reader
import org.ntqqrev.acidify.internal.util.writeBytes
import kotlin.random.Random

private val ecdhKey =
    "04928D8850673088B343264E0C6BACB8496D697799F37211DEB25BB73906CB089FEA9639B4E0260498B51A992D50813DA8".hexToByteArray()
private val secp192k1 = Ecdh.generateKeyPair(Ecdh.Secp192K1)

internal fun LagrangeClient.buildCode2DPacket(tlvPack: ByteArray, command: Short): ByteArray {
    val requestBody = Buffer().apply {
        writeInt((getTimeMillis() / 1000).toInt())
        writeByte(0x2) // packet Start
        writeShort((43 + tlvPack.size + 1).toShort()) // _head_len = 43 + data.size +1
        writeShort(command)
        writeBytes(ByteArray(21))
        writeByte(0x3)
        writeShort(0x0) // close
        writeShort(0x32) // Version Code: 50
        writeInt(0) // trans_emp sequence
        writeLong(0) // dummy uin
        writeBytes(tlvPack)
        writeByte(0x3)
    }

    val packet = Buffer().apply {
        writeByte(0x0) // encryptMethod == EncryptMethod.EM_ST || encryptMethod == EncryptMethod.EM_ECDH_ST
        writeShort(requestBody.size.toShort())
        writeInt(appInfo.appId)
        writeInt(0x72) // Role
        writeBytes(ByteArray(0), Prefix.UINT_16 or Prefix.LENGTH_ONLY) // uSt
        writeBytes(ByteArray(0), Prefix.UINT_8 or Prefix.LENGTH_ONLY) // rollback
        transferFrom(requestBody)
    }

    return packet.readByteArray()
}

internal fun LagrangeClient.buildWtLogin(payload: ByteArray, command: Short): ByteArray {
    val encrypted = TeaProvider.encrypt(
        data = payload,
        key = Ecdh.keyExchange(secp192k1, ecdhKey, true)
    )
    val packet = Buffer()
    packet.writeByte(2)
    packet.barrier(Prefix.UINT_16 or Prefix.INCLUDE_PREFIX, 1) {
        writeShort(8001)
        writeShort(command)
        writeShort(0) // sequence
        writeUInt(sessionStore.uin.toUInt()) // uin
        writeByte(3) // extVer
        writeByte(135.toByte()) // cmdVer
        writeInt(0) // actually unknown const 0
        writeByte(19) // pubId
        writeShort(0) // insId
        writeUShort(appInfo.appClientVersion.toUShort())
        writeInt(0) // retryTime
        // start encrypt head
        writeByte(1)
        writeByte(1)
        writeBytes(Random.nextBytes(16))
        writeUShort(0x102u) // unknown const
        writeBytes(secp192k1.packPublic(true), Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        // end encrypt head
        writeBytes(encrypted)
        writeByte(3)
    } // addition of 1, aiming to include packet start

    return packet.readByteArray()
}

internal fun parseCode2DPacket(wtlogin: ByteArray): ByteArray {
    val reader = wtlogin.reader()
    reader.readUInt() // packetLength
    reader.discard(4)
    reader.readUShort() // command
    reader.discard(40)
    reader.readUInt() // appid
    return reader.readByteArray(reader.remaining)
}

internal fun parseWtLogin(raw: ByteArray): ByteArray {
    val reader = raw.reader()
    val header = reader.readByte()
    if (header != 0x02.toByte()) throw Exception("Invalid Header")
    reader.skip(15) // internalLength(2) + ver(2) + cmd(2) + sequence(2) + uin(4) + flag(1) + retryTime(2)
    val encrypted = reader.readByteArray(reader.remaining - 1)
    val decrypted = TeaProvider.decrypt(
        encrypted,
        Ecdh.keyExchange(secp192k1, ecdhKey, true)
    )
    if (reader.readByte() != 0x03.toByte()) throw Exception("Packet end not found")

    return decrypted
}