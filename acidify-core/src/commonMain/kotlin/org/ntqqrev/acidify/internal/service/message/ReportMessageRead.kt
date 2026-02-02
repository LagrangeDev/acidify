package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.message.action.SsoReadedReportC2C
import org.ntqqrev.acidify.internal.proto.message.action.SsoReadedReportGroup
import org.ntqqrev.acidify.internal.proto.message.action.SsoReadedReportReq
import org.ntqqrev.acidify.internal.service.NoOutputService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object ReportMessageRead :
    NoOutputService<ReportMessageRead.Req>("trpc.msg.msg_svc.MsgService.SsoReadedReport") {
    class Req(
        val groupUin: Long?,
        val targetUid: String?,
        val startSequence: Long,
        val time: Long,
    )

    override fun build(client: LagrangeClient, payload: Req): ByteArray {
        return if (payload.targetUid != null) {
            // Friend message
            SsoReadedReportReq(
                c2c = SsoReadedReportC2C(
                    targetUid = payload.targetUid,
                    time = payload.time,
                    startSequence = payload.startSequence,
                )
            ).pbEncode()
        } else {
            // Group message
            SsoReadedReportReq(
                group = SsoReadedReportGroup(
                    groupUin = payload.groupUin!!,
                    startSequence = payload.startSequence,
                )
            ).pbEncode()
        }
    }
}
