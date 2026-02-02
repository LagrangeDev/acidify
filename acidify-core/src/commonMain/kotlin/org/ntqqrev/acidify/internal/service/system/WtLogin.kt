package org.ntqqrev.acidify.internal.service.system

import io.ktor.utils.io.core.*
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.writeUShort
import org.ntqqrev.acidify.exception.WtLoginException
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.crypto.tea.TeaProvider
import org.ntqqrev.acidify.internal.proto.login.TlvBody543
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.tlv.buildTlv
import org.ntqqrev.acidify.internal.util.*

internal object WtLogin : NoInputService<Boolean>("wtlogin.login") {
    override fun build(client: LagrangeClient, payload: Unit): ByteArray {
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
            writeFully(tlvPack)
        }
        return client.loginContext.buildWtLogin(packet.readByteArray(), 2064u)
    }

    override fun parse(
        client: LagrangeClient,
        payload: ByteArray
    ): Boolean {
        val reader = client.loginContext.parseWtLogin(payload).reader()

        val command = reader.readUShort()
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
            return true
        } else {
            val tlv146 = tlv119Reader[0x146u]!!.reader()
            val code = tlv146.readInt()
            val tag = tlv146.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
            val message = tlv146.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
            throw WtLoginException(code, tag, message)
        }
    }
}