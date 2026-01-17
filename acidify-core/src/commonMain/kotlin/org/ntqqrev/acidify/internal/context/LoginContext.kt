package org.ntqqrev.acidify.internal.context

import io.ktor.util.date.*
import io.ktor.utils.io.core.*
import kotlinx.io.*
import kotlinx.io.Buffer
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.crypto.ecdh.Ecdh
import org.ntqqrev.acidify.internal.crypto.tea.TeaProvider
import org.ntqqrev.acidify.internal.util.Prefix
import org.ntqqrev.acidify.internal.util.barrier
import org.ntqqrev.acidify.internal.util.reader
import org.ntqqrev.acidify.internal.util.writeBytes
import kotlin.random.Random

internal class LoginContext(override val client: LagrangeClient) : AbstractContext(client) {
    private val ecdhKey =
        "04928D8850673088B343264E0C6BACB8496D697799F37211DEB25BB73906CB089FEA9639B4E0260498B51A992D50813DA8".hexToByteArray()
    private val ecdhProvider = Ecdh.generateKeyPair(Ecdh.Secp192K1)

    fun buildCode2DPacket(tlvPack: ByteArray, command: UShort): ByteArray {
        val newPacket = Buffer().apply {
            writeByte(0x2) // packet Start
            writeUShort((43 + tlvPack.size + 1).toUShort()) // _head_len = 43 + data.size +1
            writeUShort(command)
            writeFully(ByteArray(21))
            writeByte(0x3)
            writeShort(0x0) // close
            writeShort(0x32) // Version Code: 50
            writeUInt(0u) // trans_emp sequence
            writeULong(0.toULong()) // dummy uin
            writeFully(tlvPack)
            writeByte(0x3)
        }

        val requestBody = Buffer().apply {
            writeUInt((getTimeMillis() / 1000).toUInt())
            writeFully(newPacket.readByteArray())
        }

        val packet = Buffer().apply {
            writeByte(0x0) // encryptMethod == EncryptMethod.EM_ST || encryptMethod == EncryptMethod.EM_ECDH_ST
            writeUShort(requestBody.size.toUShort())
            writeInt(client.appInfo.appId)
            writeInt(0x72) // Role
            writeBytes(ByteArray(0), Prefix.UINT_16 or Prefix.LENGTH_ONLY) // uSt
            writeBytes(ByteArray(0), Prefix.UINT_8 or Prefix.LENGTH_ONLY) // rollback
            writeFully(requestBody.readByteArray())
        }

        return buildWtLogin(packet.readByteArray(), 2066u)
    }

    fun parseCode2DPacket(wtlogin: ByteArray): ByteArray {
        val reader = wtlogin.reader()

        reader.readUInt() // packetLength
        reader.discard(4)
        reader.readUShort() // command
        reader.discard(40)
        reader.readUInt() // appid

        return reader.readByteArray(reader.remaining)
    }

    fun buildWtLogin(payload: ByteArray, command: UShort): ByteArray {
        val encrypted = TeaProvider.encrypt(payload, Ecdh.keyExchange(ecdhProvider, ecdhKey, true))
        val packet = Buffer()
        packet.writeByte(2)
        packet.barrier(Prefix.UINT_16 or Prefix.INCLUDE_PREFIX, 1) {
            writeUShort(8001u)
            writeUShort(command)
            writeUShort(0u) // sequence
            writeUInt(client.sessionStore.uin.toUInt()) // uin
            writeByte(3) // extVer
            writeByte(135.toByte()) // cmdVer
            writeUInt(0u) // actually unknown const 0
            writeByte(19) // pubId
            writeUShort(0u) // insId
            writeUShort(client.appInfo.appClientVersion.toUShort())
            writeUInt(0u) // retryTime
            writeFully(buildEncryptHead())
            writeFully(encrypted)
            writeByte(3)
        } // addition of 1, aiming to include packet start

        return packet.readByteArray()
    }

    fun parseWtLogin(raw: ByteArray): ByteArray {
        val reader = raw.reader()
        val header = reader.readByte()
        if (header != 0x02.toByte()) throw Exception("Invalid Header")

        /*
        val internalLength = reader.readUShort()
        val ver = reader.readUShort()
        val cmd = reader.readUShort()
        val sequence = reader.readUShort()
        val uin = reader.readUInt()
        val flag = reader.readByte()
        val retryTime = reader.readUShort()
         */
        reader.skip(15)

        val encrypted = reader.readByteArray(reader.remaining - 1)
        val decrypted = TeaProvider.decrypt(
            encrypted,
            Ecdh.keyExchange(ecdhProvider, ecdhKey, true)
        )
        if (reader.readByte() != 0x03.toByte()) throw Exception("Packet end not found")

        return decrypted
    }

    private fun buildEncryptHead(): ByteArray = Buffer().apply {
        writeByte(1)
        writeByte(1)
        writeBytes(Random.nextBytes(16))
        writeUShort(0x102u) // unknown const
        writeBytes(ecdhProvider.packPublic(true), Prefix.UINT_16 or Prefix.LENGTH_ONLY)
    }.readByteArray()
}