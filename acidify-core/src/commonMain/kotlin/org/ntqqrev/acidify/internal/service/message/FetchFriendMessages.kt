package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.message.CommonMessage
import org.ntqqrev.acidify.internal.proto.message.action.SsoGetC2cMsgReq
import org.ntqqrev.acidify.internal.proto.message.action.SsoGetC2cMsgResp
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object FetchFriendMessages :
    Service<FetchFriendMessages.Req, List<CommonMessage>>("trpc.msg.register_proxy.RegisterProxy.SsoGetC2cMsg") {
    class Req(
        val peerUid: String,
        val startSequence: Long,
        val endSequence: Long
    )

    override fun build(client: LagrangeClient, payload: Req): ByteArray {
        return SsoGetC2cMsgReq(
            peerUid = payload.peerUid,
            startSequence = payload.startSequence,
            endSequence = payload.endSequence,
        ).pbEncode()
    }

    override fun parse(client: LagrangeClient, payload: ByteArray): List<CommonMessage> {
        val resp = payload.pbDecode<SsoGetC2cMsgResp>()
        val retcode = resp.retcode
        val errorMsg = resp.errorMsg

        if (retcode != 0) {
            throw Exception("Failed to get friend messages: $errorMsg (retcode=$retcode)")
        }

        return resp.messages
    }
}
