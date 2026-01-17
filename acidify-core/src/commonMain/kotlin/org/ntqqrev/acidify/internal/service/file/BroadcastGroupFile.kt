package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.BroadcastGroupFileReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import kotlin.random.Random

internal object BroadcastGroupFile : NoOutputOidbService<BroadcastGroupFile.Req>(0x6d9, 4) {
    class Req(
        val groupUin: Long,
        val fileId: String
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        BroadcastGroupFileReq {
            it[body] = BroadcastGroupFileReq.Body {
                it[groupUin] = payload.groupUin
                it[type] = 2
                it[info] = BroadcastGroupFileReq.Body.Info {
                    it[busiType] = 102
                    it[fileId] = payload.fileId
                    it[field3] = Random.nextInt()
                    it[field5] = true
                }
            }
        }.toByteArray()
}

