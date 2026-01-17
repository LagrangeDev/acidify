package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.FetchGroupExtraInfoReq
import org.ntqqrev.acidify.internal.packet.oidb.FetchGroupExtraInfoResp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService
import kotlin.random.Random

internal object FetchGroupExtraInfo : OidbService<Long, FetchGroupExtraInfo.Resp>(0x88d, 0) {
    class Resp(val latestMessageSeq: Long)

    override fun buildOidb(client: IClient, payload: Long): ByteArray = FetchGroupExtraInfoReq {
        it[random] = Random.nextInt()
        it[config] = FetchGroupExtraInfoReq.Config {
            it[groupUin] = payload
            it[flags] = FetchGroupExtraInfoReq.Config.Flags {
                it[latestMessageSeq] = true
            }
        }
    }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray): Resp {
        val resp = FetchGroupExtraInfoResp(payload)
        val info = resp.get { info }
        val results = info.get { results }
        return Resp(
            latestMessageSeq = results.get { latestMessageSeq }
        )
    }
}

