package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.FetchPinsResp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoInputOidbService

internal object FetchPins : NoInputOidbService<FetchPins.Resp>(0x12b3, 0) {
    class Resp(
        val friendUids: List<String>,
        val groupUins: List<Long>
    )

    override fun buildOidb(client: IClient, payload: Unit): ByteArray = ByteArray(0)

    override fun parseOidb(client: IClient, payload: ByteArray): Resp {
        val resp = FetchPinsResp(payload)
        return Resp(
            friendUids = resp.get { friends }.map { it.get { uid } },
            groupUins = resp.get { groups }.map { it.get { uin } }
        )
    }
}
