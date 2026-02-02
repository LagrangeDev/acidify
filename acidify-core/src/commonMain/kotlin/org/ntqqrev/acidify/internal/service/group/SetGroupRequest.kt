package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.SetGroupRequestBody
import org.ntqqrev.acidify.internal.proto.oidb.SetGroupRequestReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal abstract class SetGroupRequest(isFiltered: Boolean) :
    NoOutputOidbService<SetGroupRequest.Req>(0x10c8, if (!isFiltered) 1 else 2) {

    class Req(
        val groupUin: Long,
        val sequence: Long,
        val eventType: Int,
        val accept: Int,
        val reason: String
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        SetGroupRequestReq(
            accept = payload.accept,
            body = SetGroupRequestBody(
                sequence = payload.sequence,
                eventType = payload.eventType,
                groupUin = payload.groupUin,
                message = payload.reason,
            )
        ).pbEncode()

    object Normal : SetGroupRequest(false)

    object Filtered : SetGroupRequest(true)
}
