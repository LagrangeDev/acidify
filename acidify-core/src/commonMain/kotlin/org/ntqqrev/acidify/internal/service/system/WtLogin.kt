package org.ntqqrev.acidify.internal.service.system

import kotlinx.io.*
import org.ntqqrev.acidify.exception.WtLoginException
import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.crypto.tea.TeaProvider
import org.ntqqrev.acidify.internal.packet.*
import org.ntqqrev.acidify.internal.proto.login.TlvBody543
import org.ntqqrev.acidify.internal.proto.login.TlvQRCodeBodyD1Resp
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.tlv.buildTlv
import org.ntqqrev.acidify.internal.tlv.buildTlvQRCode
import org.ntqqrev.acidify.internal.util.*
import org.ntqqrev.acidify.struct.QRCodeState

internal abstract class WtLogin<R>(
    cmdSuffix: String,
    val wtLoginSubCmd: Short
) : NoInputService<R>("wtlogin.$cmdSuffix") {
    override val ssoEncryptType = EncryptType.WithEmptyKey

    override fun build(client: AbstractClient, payload: Unit): ByteArray {
        client.ensureLagrange()
        return client.buildWtLogin(
            payload = buildWtLoginPayload(client),
            command = wtLoginSubCmd,
        )
    }

    override fun parse(client: AbstractClient, payload: ByteArray): R = parseWtLoginPayload(
        client = client.ensureLagrange(),
        wtLogin = unwrapWtLogin(payload),
    )

    abstract fun buildWtLoginPayload(client: LagrangeClient): ByteArray

    abstract fun parseWtLoginPayload(client: LagrangeClient, wtLogin: ByteArray): R

    abstract class TransEmp<R>(val transEmpSubCmd: Short) : WtLogin<R>("trans_emp", 2066) {
        override fun buildWtLoginPayload(client: LagrangeClient): ByteArray =
            client.buildCode2DPacket(
                tlvPack = buildCode2DPayload(client),
                command = transEmpSubCmd,
            )

        override fun parseWtLoginPayload(client: LagrangeClient, wtLogin: ByteArray): R =
            parseCode2DPayload(
                client = client,
                code2D = unwrapCode2DPacket(wtLogin),
            )

        abstract fun buildCode2DPayload(client: LagrangeClient): ByteArray

        abstract fun parseCode2DPayload(client: LagrangeClient, code2D: ByteArray): R

        object FetchQRCode : TransEmp<FetchQRCode.Result>(0x31) {
            class Result(
                val qrCodeUrl: String,
                val qrCodePng: ByteArray
            )

            override fun buildCode2DPayload(client: LagrangeClient): ByteArray = Buffer().apply {
                writeUShort(0u)
                writeUInt(client.appInfo.appId.toUInt())
                writeULong(0u) // uin
                writeBytes(ByteArray(0))
                writeByte(0)
                writeBytes(ByteArray(0), Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                writeBytes(client.buildTlvQRCode {
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
                client: LagrangeClient,
                code2D: ByteArray
            ): Result {
                val reader = code2D.reader()
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
        }

        object QueryQRCodeState : TransEmp<QRCodeState>(0x12) {
            override fun buildCode2DPayload(client: LagrangeClient): ByteArray = Buffer().apply {
                writeUShort(0u)
                writeUInt(client.appInfo.appId.toUInt())
                writeBytes(client.sessionStore.qrSig, Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                writeULong(0u) // uin
                writeByte(0)
                writeBytes(ByteArray(0), Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                writeUShort(0u)  // actually it is the tlv count, but there is no tlv so 0x0 is used
            }.readByteArray()

            override fun parseCode2DPayload(
                client: LagrangeClient,
                code2D: ByteArray
            ): QRCodeState {
                val reader = code2D.reader()
                val state = QRCodeState.fromByte(reader.readByte())
                if (state == QRCodeState.CONFIRMED) {
                    reader.discard(4)
                    client.sessionStore.uin = reader.readUInt().toLong()
                    reader.discard(4)

                    val tlv = reader.readTlv()
                    client.sessionStore.tgtgt = tlv[0x1eu]!!
                    client.sessionStore.encryptedA1 = tlv[0x18u]!!
                    client.sessionStore.noPicSig = tlv[0x19u]!!
                }
                return state
            }
        }
    }

    object Login : WtLogin<Unit>("login", 2064) {
        override fun buildWtLoginPayload(client: LagrangeClient): ByteArray {
            val tlvPack = client.buildTlv {
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
            val packet = Buffer().apply {
                writeUShort(9u) // internal command
                writeBytes(tlvPack)
            }
            return packet.readByteArray()
        }

        override fun parseWtLoginPayload(
            client: LagrangeClient,
            wtLogin: ByteArray
        ) {
            val reader = unwrapWtLogin(wtLogin).reader()
            reader.readUShort() // command
            val state = reader.readUByte()
            val tlv119Reader = reader.readTlv()
            if (state.toInt() == 0) {
                val tlv119 = tlv119Reader[0x119u]!!
                val array = TeaProvider.decrypt(tlv119, client.sessionStore.tgtgt)
                val tlvPack = array.parseTlv()
                client.sessionStore.apply {
                    d2Key = tlvPack[0x305u]!!
                    uid = tlvPack[0x543u]!!.pbDecode<TlvBody543>().layer1.layer2.uid
                    a2 = tlvPack[0x10Au]!!
                    d2 = tlvPack[0x143u]!!
                    encryptedA1 = tlvPack[0x106u]!!
                }
                return
            } else {
                val tlv146 = tlv119Reader[0x146u]!!.reader()
                val code = tlv146.readInt()
                val tag = tlv146.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                val message = tlv146.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                throw WtLoginException(code, tag, message)
            }
        }
    }
}