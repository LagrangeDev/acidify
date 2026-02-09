package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.message.CommonMessage
import org.ntqqrev.acidify.internal.proto.message.action.*
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.internal.util.gzipUncompress
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object RecvLongMsg :
    Service<RecvLongMsg.Req, List<CommonMessage>>("trpc.group.long_msg_interface.MsgService.SsoRecvLongMsg") {
    class Req(val resId: String, val isGroup: Boolean = false)

    override fun build(client: AbstractClient, payload: Req): ByteArray {
        return LongMsgInterfaceReq(
            recvReq = LongMsgRecvReq(
                peerInfo = LongMsgPeerInfo(peerUid = client.uid),
                resId = payload.resId,
                msgType = if (payload.isGroup) 1 else 3,
            ),
            attr = LongMsgAttr(
                subCmd = 2,
                clientType = when (client.os) {
                    "Windows", "Linux", "Mac" -> 1
                    "AndroidPhone" -> 2
                    // 3 for iOS, 4 for iPadOS
                    "AndroidPad" -> 5
                    else -> 0
                },
                platform = when (client.os) {
                    "Windows" -> 3
                    "Linux" -> 6
                    "Mac" -> 7
                    // 8 for iOS
                    "AndroidPhone", "AndroidPad" -> 9
                    else -> 0
                },
                proxyType = 0,
            ),
        ).pbEncode()
    }

    override fun parse(client: AbstractClient, payload: ByteArray): List<CommonMessage> {
        val resp = payload.pbDecode<LongMsgInterfaceResp>()
        val compressedPayload = resp.recvResp?.payload
            ?: throw IllegalStateException("No payload in LongMsgInterfaceResp")

        val decompressed = gzipUncompress(compressedPayload)
        val content = decompressed.pbDecode<PbMultiMsgTransmit>()

        return content.items
            .firstOrNull { it.fileName == "MultiMsg" }
            ?.buffer?.msg
            ?: emptyList()
    }
}
