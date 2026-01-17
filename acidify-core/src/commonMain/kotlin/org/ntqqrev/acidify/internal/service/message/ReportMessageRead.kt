package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.message.action.SsoReadedReportC2C
import org.ntqqrev.acidify.internal.packet.message.action.SsoReadedReportGroup
import org.ntqqrev.acidify.internal.packet.message.action.SsoReadedReportReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputService

internal object ReportMessageRead :
    NoOutputService<ReportMessageRead.Req>("trpc.msg.msg_svc.MsgService.SsoReadedReport") {
    class Req(
        val groupUin: Long?,
        val targetUid: String?,
        val startSequence: Long,
        val time: Long,
    )

    override fun build(client: IClient, payload: Req): ByteArray {
        return if (payload.targetUid != null) {
            // Friend message
            SsoReadedReportReq {
                it[c2c] = SsoReadedReportC2C {
                    it[targetUid] = payload.targetUid
                    it[time] = payload.time
                    it[startSequence] = payload.startSequence
                }
            }.toByteArray()
        } else {
            // Group message
            SsoReadedReportReq {
                it[group] = SsoReadedReportGroup {
                    it[groupUin] = payload.groupUin!!
                    it[startSequence] = payload.startSequence
                }
            }.toByteArray()
        }
    }
}

