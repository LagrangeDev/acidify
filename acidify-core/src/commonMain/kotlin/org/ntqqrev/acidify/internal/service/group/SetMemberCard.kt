package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x8FCReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object SetMemberCard : NoOutputOidbService<SetMemberCard.Req>(0x8fc, 3) {
    class Req(
        val groupUin: Long,
        val memberUid: String,
        val card: String
    )

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray =
        Oidb0x8FCReq(
            groupCode = payload.groupUin,
            memLevelInfo = listOf(
                Oidb0x8FCReq.MemberInfo(
                    uid = payload.memberUid,
                    memberCardName = payload.card.encodeToByteArray(),
                )
            )
        ).pbEncode()
}
