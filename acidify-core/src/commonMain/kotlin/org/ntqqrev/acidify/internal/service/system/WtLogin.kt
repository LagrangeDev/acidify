package org.ntqqrev.acidify.internal.service.system

import kotlinx.io.*
import org.ntqqrev.acidify.exception.WtLoginException
import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.crypto.ecdh.Ecdh
import org.ntqqrev.acidify.internal.crypto.pow.POW
import org.ntqqrev.acidify.internal.crypto.tea.TEA
import org.ntqqrev.acidify.internal.proto.login.TlvBody543
import org.ntqqrev.acidify.internal.proto.login.TlvQRCodeBodyD1Resp
import org.ntqqrev.acidify.internal.service.EncryptType
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.internal.util.*
import org.ntqqrev.acidify.struct.QRCodeState
import kotlin.random.Random
import kotlin.time.Clock

internal abstract class WtLogin<T, R>(
    cmdSuffix: String,
    val wtLoginSubCmd: Short
) : Service<T, R>("wtlogin.$cmdSuffix") {
    private val secp192k1BobPublicKey =
        "04928D8850673088B343264E0C6BACB8496D697799F37211DEB25BB73906CB089FEA9639B4E0260498B51A992D50813DA8".hexToByteArray()
    private val secp192k1 = Ecdh.generateKeyPair(Ecdh.Secp192K1)
    private val shareKey = Ecdh.keyExchange(secp192k1, secp192k1BobPublicKey, true)

    override val ssoEncryptType = EncryptType.WithEmptyKey

    override fun build(client: AbstractClient, payload: T): ByteArray {
        val encrypted = TEA.encrypt(buildWtLoginPayload(client, payload), shareKey)
        val packet = Buffer()
        packet.writeByte(2)
        packet.barrier(Prefix.UINT_16 or Prefix.INCLUDE_PREFIX, 1) {
            writeShort(8001)
            writeShort(wtLoginSubCmd)
            writeShort(0) // sequence
            writeUInt(client.uin.toUInt()) // uin
            writeByte(3) // extVer
            writeByte(135.toByte()) // cmdVer
            writeInt(0) // actually unknown const 0
            writeByte(19) // pubId
            writeShort(0) // insId
            writeUShort(client.appClientVersion.toUShort())
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

    override fun parse(client: AbstractClient, payload: ByteArray): R {
        val reader = payload.reader()
        val header = reader.readByte()
        if (header != 0x02.toByte()) throw Exception("Invalid Header")
        reader.skip(15) // internalLength(2) + ver(2) + cmd(2) + sequence(2) + uin(4) + flag(1) + retryTime(2)
        val encrypted = reader.readByteArray(reader.remaining - 1)
        val decrypted = TEA.decrypt(encrypted, shareKey)
        if (reader.readByte() != 0x03.toByte()) throw Exception("Packet end not found")
        return parseWtLoginPayload(
            client = client,
            wtLogin = decrypted,
        )
    }

    abstract fun buildWtLoginPayload(client: AbstractClient, payload: T): ByteArray

    abstract fun parseWtLoginPayload(client: AbstractClient, wtLogin: ByteArray): R

    abstract class TransEmp<R>(val transEmpSubCmd: Short) : WtLogin<Unit, R>("trans_emp", 2066) {
        override fun buildWtLoginPayload(client: AbstractClient, payload: Unit): ByteArray {
            val tlvPack = buildCode2DPayload(client)
            val requestBody = Buffer().apply {
                writeInt(Clock.System.now().epochSeconds.toInt())
                writeByte(0x2) // packet Start
                writeShort((43 + tlvPack.size + 1).toShort()) // _head_len = 43 + data.size +1
                writeShort(short = transEmpSubCmd)
                writeBytes(ByteArray(21))
                writeByte(0x3)
                writeShort(0x0) // close
                writeShort(0x32) // Version Code: 50
                writeInt(0) // trans_emp sequence
                writeLong(0) // dummy uin
                writeBytes(value = tlvPack)
                writeByte(0x3)
            }
            val packet = Buffer().apply {
                writeByte(0x0) // encryptMethod == EncryptMethod.EM_ST || encryptMethod == EncryptMethod.EM_ECDH_ST
                writeShort(requestBody.size.toShort())
                writeInt(client.appId)
                writeInt(0x72) // Role
                writeBytes(ByteArray(0), Prefix.UINT_16 or Prefix.LENGTH_ONLY) // uSt
                writeBytes(ByteArray(0), Prefix.UINT_8 or Prefix.LENGTH_ONLY) // rollback
                transferFrom(requestBody)
            }
            return packet.readByteArray()
        }

        override fun parseWtLoginPayload(client: AbstractClient, wtLogin: ByteArray): R {
            val reader = wtLogin.reader()
            reader.readUInt() // packetLength
            reader.discard(4)
            reader.readUShort() // command
            reader.discard(40)
            reader.readUInt() // appid
            return parseCode2DPayload(
                client = client,
                code2D = reader.readByteArray(reader.remaining),
            )
        }

        abstract fun buildCode2DPayload(client: AbstractClient): ByteArray

        abstract fun parseCode2DPayload(client: AbstractClient, code2D: ByteArray): R

        object FetchQRCode : TransEmp<FetchQRCode.Result>(0x31) {
            class Result(
                val qrSig: ByteArray,
                val qrCodeUrl: String,
                val qrCodePng: ByteArray
            )

            override fun buildCode2DPayload(client: AbstractClient): ByteArray = Buffer().apply {
                client.ensureLagrange()
                writeUShort(0u)
                writeUInt(client.appInfo.appId.toUInt())
                writeULong(0u) // uin
                writeBytes(ByteArray(0))
                writeByte(0)
                writeBytes(ByteArray(0), Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                transferFrom(client.buildTlvQRCode {
                    tlv16()
                    tlv1b()
                    tlv1d()
                    tlv33()
                    tlv35()
                    tlv66()
                    tlvD1()
                })
            }.readByteArray()

            override fun parseCode2DPayload(
                client: AbstractClient,
                code2D: ByteArray
            ): Result {
                client.ensureLagrange()
                val reader = code2D.reader()
                reader.discard(1)
                val sig = reader.readPrefixedBytes(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                val tlv = reader.readTlv()
                val respD1Body = tlv[0xD1u]!!.pbDecode<TlvQRCodeBodyD1Resp>()
                return Result(
                    qrSig = sig,
                    qrCodeUrl = respD1Body.qrCodeUrl,
                    qrCodePng = tlv[0x17u]!!
                )
            }
        }

        object QueryQRCodeState : TransEmp<QueryQRCodeState.Result>(0x12) {
            sealed class Result(val state: QRCodeState) {
                class Success(
                    val uin: Long,
                    val tgtgt: ByteArray,
                    val encryptedA1: ByteArray,
                    val noPicSig: ByteArray
                ) : Result(QRCodeState.CONFIRMED)

                class Other(state: QRCodeState) : Result(state)
            }

            override fun buildCode2DPayload(client: AbstractClient): ByteArray = Buffer().apply {
                client.ensureLagrange()
                writeUShort(0u)
                writeUInt(client.appInfo.appId.toUInt())
                writeBytes(client.sessionStore.qrSig, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                writeULong(0u) // uin
                writeByte(0)
                writeBytes(ByteArray(0), Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                writeUShort(0u)  // actually it is the tlv count, but there is no tlv so 0x0 is used
            }.readByteArray()

            override fun parseCode2DPayload(
                client: AbstractClient,
                code2D: ByteArray
            ): Result {
                client.ensureLagrange()
                val reader = code2D.reader()
                val state = QRCodeState.fromByte(reader.readByte())
                if (state == QRCodeState.CONFIRMED) {
                    reader.discard(4)
                    val uin = reader.readUInt().toLong()
                    reader.discard(4)
                    val tlv = reader.readTlv()
                    return Result.Success(
                        uin = uin,
                        tgtgt = tlv[0x1eu]!!,
                        encryptedA1 = tlv[0x18u]!!,
                        noPicSig = tlv[0x19u]!!
                    )
                } else {
                    return Result.Other(state)
                }
            }
        }
    }

    abstract class Login<T, R>(val internalCmd: Short) : WtLogin<T, R>("login", 2064) {
        override fun buildWtLoginPayload(client: AbstractClient, payload: T): ByteArray {
            val tlvPack = buildLoginTlv(client, payload)
            val packet = Buffer().apply {
                writeUShort(internalCmd.toUShort())
                transferFrom(tlvPack)
            }
            return packet.readByteArray()
        }

        override fun parseWtLoginPayload(client: AbstractClient, wtLogin: ByteArray): R {
            val reader = wtLogin.reader()
            reader.readUShort() // command
            val state = reader.readUByte()
            val tlvPack = reader.readTlv()
            return parseLoginTlv(client, state, tlvPack)
        }

        abstract fun buildLoginTlv(client: AbstractClient, payload: T): Buffer

        abstract fun parseLoginTlv(client: AbstractClient, state: UByte, tlvPack: Map<UShort, ByteArray>): R
    }

    object PCLogin : Login<Unit, PCLogin.Result>(9) {
        class Result(
            val d2Key: ByteArray,
            val uid: String,
            val a2: ByteArray,
            val d2: ByteArray,
            val encryptedA1: ByteArray,
        )

        override fun buildLoginTlv(
            client: AbstractClient,
            payload: Unit
        ): Buffer = client.ensureLagrange().buildTlv {
            tlv106A2()
            tlv144()
            tlv116()
            tlv142()
            tlv145()
            tlv18()
            tlv141()
            tlv177()
            tlv191()
            tlv100()
            tlv107()
            tlv318()
            tlv16a()
            tlv166()
            tlv521()
        }

        override fun parseLoginTlv(
            client: AbstractClient,
            state: UByte,
            tlvPack: Map<UShort, ByteArray>
        ): Result {
            client.ensureLagrange()
            if (state.toInt() == 0) {
                val tlv119 = tlvPack[0x119u]!!
                val array = TEA.decrypt(tlv119, client.sessionStore.tgtgt)
                val internalTlvPack = array.parseTlv()
                return Result(
                    d2Key = internalTlvPack[0x305u]!!,
                    uid = internalTlvPack[0x543u]!!.pbDecode<TlvBody543>().layer1.layer2.uid,
                    a2 = internalTlvPack[0x10Au]!!,
                    d2 = internalTlvPack[0x143u]!!,
                    encryptedA1 = internalTlvPack[0x106u]!!,
                )
            } else {
                val tlv146 = tlvPack[0x146u]!!.reader()
                val code = tlv146.readInt()
                val tag = tlv146.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                val message = tlv146.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                throw WtLoginException(code, tag, message)
            }
        }
    }

    abstract class AndroidLogin<T>(internalCmd: Short) : Login<T, AndroidLogin.Resp>(internalCmd) {
        class Resp(
            val state: UByte,
            val tlvPack: Map<UShort, ByteArray>
        )

        override fun parseLoginTlv(client: AbstractClient, state: UByte, tlvPack: Map<UShort, ByteArray>): Resp {
            tlvPack[0x146u]?.let {
                val tlv146 = it.reader()
                val code = tlv146.readInt()
                val tag = tlv146.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                val message = tlv146.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                throw WtLoginException(code, tag, message)
            }
            return Resp(state, tlvPack)
        }

        object Tgtgt : AndroidLogin<Tgtgt.Req>(0x09) {
            class Req(
                val energy: ByteArray,
                val debugXwid: ByteArray,
            )

            override fun buildLoginTlv(
                client: AbstractClient,
                payload: Req
            ): Buffer = client.ensureKurome().buildTlv {
                tlv18()
                tlv1()
                tlv106Pwd()
                tlv116()
                tlv100()
                tlv107()
                tlv142()
                tlv144Report()
                tlv145()
                tlv147()
                tlv154()
                tlv141()
                tlv8()
                tlv511()
                tlv187()
                tlv188()
                tlv191(0x82u)
                tlv177()
                tlv516()
                tlv521()
                tlv525()
                tlv544(payload.energy)
                tlv545()
                tlv548(POW.generateTlv548())
                tlv553(payload.debugXwid)
            }
        }

        object SubmitCaptchaTicket : AndroidLogin<SubmitCaptchaTicket.Req>(0x02) {
            class Req(
                val energy: ByteArray,
                val debugXwid: ByteArray,
                val ticket: String,
            )

            override fun buildLoginTlv(
                client: AbstractClient,
                payload: Req
            ): Buffer = client.ensureKurome().buildTlv {
                tlv193(payload.ticket.encodeToByteArray())
                tlv8()
                client.sessionStore.state.tlv104?.let {
                    tlv104(it)
                }
                tlv116()
                client.sessionStore.state.tlv547?.let {
                    tlv547(it)
                }
                tlv544(payload.energy)
                tlv553(payload.debugXwid)
            }
        }

        object FetchSMSCode : AndroidLogin<FetchSMSCode.Req>(0x08) {
            class Req(
                val debugXwid: ByteArray,
            )

            override fun buildLoginTlv(
                client: AbstractClient,
                payload: Req
            ): Buffer = client.ensureKurome().buildTlv {
                tlv8()
                client.sessionStore.state.tlv104?.let {
                    tlv104(it)
                }
                tlv116()
                client.sessionStore.state.tlv174?.let {
                    tlv174(it)
                }
                tlv17a()
                tlv197()
                tlv553(payload.debugXwid)
            }
        }

        object SubmitSMSCode : AndroidLogin<SubmitSMSCode.Req>(0x07) {
            class Req(
                val energy: ByteArray,
                val debugXwid: ByteArray,
                val smsCode: String,
            )

            override fun buildLoginTlv(
                client: AbstractClient,
                payload: Req
            ): Buffer = client.ensureKurome().buildTlv {
                tlv8()
                client.sessionStore.state.tlv104?.let {
                    tlv104(it)
                }
                tlv116()
                client.sessionStore.state.tlv174?.let {
                    tlv174(it)
                }
                tlv17c(payload.smsCode)
                tlv401()
                tlv198()
                tlv544(payload.energy)
                tlv553(payload.debugXwid)
            }
        }
    }
}