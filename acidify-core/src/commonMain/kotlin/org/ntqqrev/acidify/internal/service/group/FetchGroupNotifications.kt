package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.FetchGroupNotificationsReq
import org.ntqqrev.acidify.internal.proto.oidb.FetchGroupNotificationsResp
import org.ntqqrev.acidify.internal.proto.oidb.GroupNotification
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

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
        val notifications: List<GroupNotification>
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        FetchGroupNotificationsReq(
            startSeq = payload.startSequence,
            count = payload.count,
        ).pbEncode()

    override fun parseOidb(client: LagrangeClient, payload: ByteArray): Resp {
        val resp = payload.pbDecode<FetchGroupNotificationsResp>()
        return Resp(
            nextSequence = resp.nextStartSeq,
            notifications = resp.notifications
        )
    }

    object Normal : FetchGroupNotifications(false)

    object Filtered : FetchGroupNotifications(true)
}
