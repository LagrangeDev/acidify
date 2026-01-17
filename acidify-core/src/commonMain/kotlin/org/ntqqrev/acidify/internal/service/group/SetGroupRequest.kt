package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.SetGroupRequestBody
import org.ntqqrev.acidify.internal.packet.oidb.SetGroupRequestReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal abstract class SetGroupRequest(isFiltered: Boolean) :
    NoOutputOidbService<SetGroupRequest.Req>(0x10c8, if (!isFiltered) 1 else 2) {

    class Req(
        val groupUin: Long,
        val sequence: Long,
        val eventType: Int,
        val accept: Int,
        val reason: String
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        SetGroupRequestReq {
            it[accept] = payload.accept
            it[body] = SetGroupRequestBody {
                it[sequence] = payload.sequence
                it[eventType] = payload.eventType
                it[groupUin] = payload.groupUin
                it[message] = payload.reason
            }
        }.toByteArray()

    object Normal : SetGroupRequest(false)

    object Filtered : SetGroupRequest(true)
}