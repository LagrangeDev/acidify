package org.ntqqrev.acidify.internal.service.message

import korlibs.io.compression.compress
import korlibs.io.compression.deflate.GZIP
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.message.CommonMessage
import org.ntqqrev.acidify.internal.proto.message.action.*
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.message.MessageScene

internal object SendLongMsg :
    Service<SendLongMsg.Req, String>("trpc.group.long_msg_interface.MsgService.SsoSendLongMsg") {
    class Req(
        val scene: MessageScene,
        val peerUin: Long,
        val peerUid: String,
        val messages: List<CommonMessage>,
        val nestedForwardTrace: Map<String, List<CommonMessage>>
    )

    override fun build(client: LagrangeClient, payload: Req): ByteArray {
        val content = PbMultiMsgTransmit(
            items = buildList {
                add(
                    PbMultiMsgItem(
                        fileName = "MultiMsg",
                        buffer = PbMultiMsgNew(msg = payload.messages),
                    )
                )
                addAll(payload.nestedForwardTrace.map { (key, value) ->
                    PbMultiMsgItem(
                        fileName = key,
                        buffer = PbMultiMsgNew(msg = value),
                    )
                })
            }
        )

        val compressedContent = GZIP.compress(content.pbEncode())

        val longMsg = LongMsgInterfaceReq(
            sendReq = LongMsgSendReq(
                msgType = if (payload.scene == MessageScene.FRIEND) 1 else 3,
                peerInfo = LongMsgPeerInfo(peerUid = payload.peerUid),
                groupUin = if (payload.scene == MessageScene.GROUP) payload.peerUin else 0L,
                payload = compressedContent,
            ),
            attr = LongMsgAttr(
                subCmd = 4,
                clientType = 1,
                platform = when (client.appInfo.os) {
                    "Windows" -> 3
                    "Linux" -> 6
                    "Mac" -> 7
                    else -> 0
                },
                proxyType = 0,
            ),
        )

        return longMsg.pbEncode()
    }

    override fun parse(client: LagrangeClient, payload: ByteArray): String {
        val rsp = payload.pbDecode<LongMsgInterfaceResp>()
        return rsp.sendResp?.resId
            ?: throw IllegalStateException("No resId in LongMsgInterfaceResp")
    }
}
