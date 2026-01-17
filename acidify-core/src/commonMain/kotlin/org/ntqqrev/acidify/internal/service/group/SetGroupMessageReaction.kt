package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.SetGroupMessageReactionReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal abstract class SetGroupMessageReaction(isAdd: Boolean) :
    NoOutputOidbService<SetGroupMessageReaction.Req>(0x9082, if (isAdd) 1 else 2) {
    class Req(
        val groupUin: Long,
        val sequence: Long,
        val code: String,
        val type: Int,
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        SetGroupMessageReactionReq {
            it[groupCode] = payload.groupUin
            it[sequence] = payload.sequence
            it[code] = payload.code
            it[type] = payload.type
        }.toByteArray()

    object Add : SetGroupMessageReaction(true)

    object Remove : SetGroupMessageReaction(false)
}

