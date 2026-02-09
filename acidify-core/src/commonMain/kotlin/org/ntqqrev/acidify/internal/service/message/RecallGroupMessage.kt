package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.message.action.GroupRecallMsg
import org.ntqqrev.acidify.internal.service.NoOutputService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object RecallGroupMessage :
    NoOutputService<RecallGroupMessage.Req>("trpc.msg.msg_svc.MsgService.SsoGroupRecallMsg") {
    class Req(
        val groupUin: Long,
        val sequence: Long
    )

    override fun build(client: AbstractClient, payload: Req): ByteArray {
        return GroupRecallMsg(
            type = 1,
            groupUin = payload.groupUin,
            info = GroupRecallMsg.Info(
                sequence = payload.sequence,
                field3 = 0,
            ),
            field4 = GroupRecallMsg.Field4(
                field1 = 0,
            ),
        ).pbEncode()
    }
}
