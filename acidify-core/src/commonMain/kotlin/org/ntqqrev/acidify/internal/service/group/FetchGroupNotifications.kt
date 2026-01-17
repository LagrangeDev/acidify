package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.FetchGroupNotificationsReq
import org.ntqqrev.acidify.internal.packet.oidb.FetchGroupNotificationsResp
import org.ntqqrev.acidify.internal.packet.oidb.GroupNotification
import org.ntqqrev.acidify.internal.protobuf.PbObject
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService

internal abstract class FetchGroupNotifications(val isFiltered: Boolean) :
    OidbService<FetchGroupNotifications.Req, FetchGroupNotifications.Resp>(
        0x10c0,
        if (!isFiltered) 1 else 2
    ) {
    class Req(
        val startSequence: Long,
        val count: Int
    )

    class Resp(
        val nextSequence: Long,
        val notifications: List<PbObject<GroupNotification>>
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        FetchGroupNotificationsReq {
            it[startSeq] = payload.startSequence
            it[count] = payload.count
        }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray): Resp {
        val resp = FetchGroupNotificationsResp(payload)
        return Resp(
            nextSequence = resp.get { nextStartSeq },
            notifications = resp.get { notifications }
        )
    }

    object Normal : FetchGroupNotifications(false)

    object Filtered : FetchGroupNotifications(true)
}