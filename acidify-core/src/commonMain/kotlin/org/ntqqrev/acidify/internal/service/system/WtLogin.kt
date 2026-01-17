package org.ntqqrev.acidify.internal.service.system

import io.ktor.utils.io.core.*
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.writeUShort
import org.ntqqrev.acidify.exception.WtLoginException
import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.crypto.tea.TeaProvider
import org.ntqqrev.acidify.internal.ensureIsLagrange
import org.ntqqrev.acidify.internal.packet.login.Tlv
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.Prefix
import org.ntqqrev.acidify.internal.util.parseTlv
import org.ntqqrev.acidify.internal.util.readTlv
import org.ntqqrev.acidify.internal.util.reader

internal object WtLogin : NoInputService<Boolean>("wtlogin.login") {
    override fun build(client: IClient, payload: Unit): ByteArray {
        client.ensureIsLagrange()
        val tlvPack = Tlv(client).apply {
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
            writeFully(tlvPack.build())
        }
        return client.loginContext.buildWtLogin(packet.readByteArray(), 2064u)
    }

    override fun parse(client: IClient, payload: ByteArray): Boolean {
        client.ensureIsLagrange()
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
                uid = Tlv.Body543(tlvPack[0x543u]!!)
                    .get { layer1 }
                    .get { layer2 }
                    .get { uid }
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