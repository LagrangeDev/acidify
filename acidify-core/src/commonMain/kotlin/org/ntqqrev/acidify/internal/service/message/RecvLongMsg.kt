package org.ntqqrev.acidify.internal.service.message

import korlibs.io.compression.deflate.GZIP
import korlibs.io.compression.uncompress
import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.message.CommonMessage
import org.ntqqrev.acidify.internal.packet.message.action.*
import org.ntqqrev.acidify.internal.protobuf.PbObject
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.Service

internal object RecvLongMsg :
    Service<RecvLongMsg.Req, List<PbObject<CommonMessage>>>("trpc.group.long_msg_interface.MsgService.SsoRecvLongMsg") {
    class Req(val resId: String, val isGroup: Boolean = false)

    override fun build(client: IClient, payload: Req): ByteArray {
        return LongMsgInterfaceReq {
            it[recvReq] = LongMsgRecvReq {
                it[peerInfo] = LongMsgPeerInfo {
                    it[peerUid] = client.uid
                }
                it[resId] = payload.resId
                it[msgType] = if (payload.isGroup) 1 else 3
            }
            it[attr] = LongMsgAttr {
                it[subCmd] = 2
                it[clientType] = 1
                it[platform] = when (client.os) {
                    "Windows" -> 3
                    "Linux" -> 6
                    "Mac" -> 7
                    else -> 0
                }
                it[proxyType] = 0
            }
        }.toByteArray()
    }

    override fun parse(client: IClient, payload: ByteArray): List<PbObject<CommonMessage>> {
        val resp = PbObject(LongMsgInterfaceResp, payload)
        val compressedPayload = resp.get { recvResp }?.get { this.payload }
            ?: throw IllegalStateException("No payload in LongMsgInterfaceResp")

        val decompressed = GZIP.uncompress(compressedPayload)
        val content = PbObject(PbMultiMsgTransmit, decompressed)

        return content.get { items }
            .firstOrNull { it.get { fileName } == "MultiMsg" }
            ?.get { buffer }?.get { msg }
            ?: emptyList()
    }
}

