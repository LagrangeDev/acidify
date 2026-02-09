package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.message.*
import org.ntqqrev.acidify.internal.proto.message.action.PbSendMsgReq
import org.ntqqrev.acidify.internal.proto.message.action.PbSendMsgResp
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object SendFriendMessage : Service<SendFriendMessage.Req, SendFriendMessage.Resp>("MessageSvc.PbSendMsg") {
    class Req(
        val friendUin: Long,
        val friendUid: String,
        val elems: List<Elem>,
        val clientSequence: Long,
        val random: Int,
    )

    class Resp(
        val result: Int,
        val errMsg: String,
        val sendTime: Long,
        val sequence: Long
    )

    override fun build(client: AbstractClient, payload: Req): ByteArray {
        return PbSendMsgReq(
            routingHead = SendRoutingHead(
                c2c = SendRoutingHead.C2C(
                    peerUin = payload.friendUin,
                    peerUid = payload.friendUid,
                )
            ),
            contentHead = SendContentHead(pkgNum = 1),
            messageBody = MessageBody(
                richText = RichText(elems = payload.elems)
            ),
            clientSequence = payload.clientSequence,
            random = payload.random,
        ).pbEncode()
    }

    override fun parse(client: AbstractClient, payload: ByteArray): Resp {
        val resp = payload.pbDecode<PbSendMsgResp>()
        return Resp(
            result = resp.result,
            errMsg = resp.errMsg,
            sendTime = resp.sendTime,
            sequence = resp.clientSequence
        )
    }
}
