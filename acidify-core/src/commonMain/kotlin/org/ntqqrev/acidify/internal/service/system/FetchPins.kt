package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.FetchPinsResp
import org.ntqqrev.acidify.internal.service.NoInputOidbService
import org.ntqqrev.acidify.internal.util.pbDecode

internal object FetchPins : NoInputOidbService<FetchPins.Resp>(0x12b3, 0) {
    class Resp(
        val friendUids: List<String>,
        val groupUins: List<Long>
    )

    override fun buildOidb(client: LagrangeClient, payload: Unit): ByteArray = ByteArray(0)

    override fun parseOidb(client: LagrangeClient, payload: ByteArray): Resp {
        val resp = payload.pbDecode<FetchPinsResp>()
        return Resp(
            friendUids = resp.friends.map { it.uid },
            groupUins = resp.groups.map { it.uin }
        )
    }
}
