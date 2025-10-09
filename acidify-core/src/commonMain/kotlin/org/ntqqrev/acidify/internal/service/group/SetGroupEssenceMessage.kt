package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.packet.oidb.SetGroupEssenceMessageReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.pb.invoke

internal abstract class SetGroupEssenceMessage(isSet: Boolean) :
    NoOutputOidbService<SetGroupEssenceMessage.Req>(0xeac, if (isSet) 1 else 2) {
    class Req(
        val groupUin: Long,
        val sequence: Long,
        val random: Int
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        SetGroupEssenceMessageReq {
            it[groupCode] = payload.groupUin
            it[sequence] = payload.sequence
            it[random] = payload.random
        }.toByteArray()

    object Set : SetGroupEssenceMessage(true)

    object Unset : SetGroupEssenceMessage(false)
}

