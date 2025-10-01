package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.packet.message.CommonMessage
import org.ntqqrev.acidify.internal.packet.message.action.SsoGetGroupMsgReq
import org.ntqqrev.acidify.internal.packet.message.action.SsoGetGroupMsgResp
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.pb.PbObject
import org.ntqqrev.acidify.pb.invoke

internal object FetchGroupMessages :
    Service<FetchGroupMessages.Req, List<PbObject<CommonMessage>>>("trpc.msg.register_proxy.RegisterProxy.SsoGetGroupMsg") {
    class Req(
        val groupUin: Long,
        val startSequence: Long,
        val endSequence: Long,
        val filter: Int = 1
    )

    override fun build(client: LagrangeClient, payload: Req): ByteArray {
        return SsoGetGroupMsgReq {
            it[groupInfo] = SsoGetGroupMsgReq.GroupInfo {
                it[groupUin] = payload.groupUin
                it[startSequence] = payload.startSequence
                it[endSequence] = payload.endSequence
            }
            it[filter] = payload.filter
        }.toByteArray()
    }

    override fun parse(client: LagrangeClient, payload: ByteArray): List<PbObject<CommonMessage>> {
        val resp = SsoGetGroupMsgResp(payload)
        val retcode = resp.get { retcode }
        val errorMsg = resp.get { errorMsg }

        if (retcode != 0) {
            throw Exception("Failed to get group messages: $errorMsg (retcode=$retcode)")
        }

        return resp.get { body }.get { messages }
    }
}

