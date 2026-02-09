package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.message.CommonMessage
import org.ntqqrev.acidify.internal.proto.message.action.SsoGetGroupMsgReq
import org.ntqqrev.acidify.internal.proto.message.action.SsoGetGroupMsgResp
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.internal.util.checkRetCode
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object FetchGroupMessages :
    Service<FetchGroupMessages.Req, List<CommonMessage>>("trpc.msg.register_proxy.RegisterProxy.SsoGetGroupMsg") {
    class Req(
        val groupUin: Long,
        val startSequence: Long,
        val endSequence: Long,
        val filter: Int = 1
    )

    override fun build(client: AbstractClient, payload: Req): ByteArray {
        return SsoGetGroupMsgReq(
            groupInfo = SsoGetGroupMsgReq.GroupInfo(
                groupUin = payload.groupUin,
                startSequence = payload.startSequence,
                endSequence = payload.endSequence,
            ),
            filter = payload.filter,
        ).pbEncode()
    }

    override fun parse(client: AbstractClient, payload: ByteArray): List<CommonMessage> {
        val resp = payload.pbDecode<SsoGetGroupMsgResp>()
        checkRetCode(resp.retcode, resp.errorMsg)
        return resp.body.messages
    }
}
