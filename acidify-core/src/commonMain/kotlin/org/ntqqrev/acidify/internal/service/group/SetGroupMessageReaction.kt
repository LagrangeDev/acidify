package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.SetGroupMessageReactionReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal abstract class SetGroupMessageReaction(isAdd: Boolean) :
    NoOutputOidbService<SetGroupMessageReaction.Req>(0x9082, if (isAdd) 1 else 2) {
    class Req(
        val groupUin: Long,
        val sequence: Long,
        val code: String,
        val type: Int,
    )

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray =
        SetGroupMessageReactionReq(
            groupCode = payload.groupUin,
            sequence = payload.sequence,
            code = payload.code,
            type = payload.type,
        ).pbEncode()

    object Add : SetGroupMessageReaction(true)

    object Remove : SetGroupMessageReaction(false)
}
