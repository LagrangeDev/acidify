package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.FetchGroupExtraInfoReq
import org.ntqqrev.acidify.internal.proto.oidb.FetchGroupExtraInfoResp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import kotlin.random.Random

internal object FetchGroupExtraInfo : OidbService<Long, FetchGroupExtraInfo.Resp>(0x88d, 0) {
    class Resp(val latestMessageSeq: Long)

    override fun buildOidb(client: LagrangeClient, payload: Long): ByteArray = FetchGroupExtraInfoReq(
        random = Random.nextInt(),
        config = FetchGroupExtraInfoReq.Config(
            groupUin = payload,
            flags = FetchGroupExtraInfoReq.Config.Flags(
                latestMessageSeq = true,
            )
        )
    ).pbEncode()

    override fun parseOidb(client: LagrangeClient, payload: ByteArray): Resp {
        val results = payload.pbDecode<FetchGroupExtraInfoResp>().info.results
        return Resp(
            latestMessageSeq = results.latestMessageSeq
        )
    }
}
