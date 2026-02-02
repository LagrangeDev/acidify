package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.BroadcastGroupFileReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode
import kotlin.random.Random

internal object BroadcastGroupFile : NoOutputOidbService<BroadcastGroupFile.Req>(0x6d9, 4) {
    class Req(
        val groupUin: Long,
        val fileId: String
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        BroadcastGroupFileReq(
            body = BroadcastGroupFileReq.Body(
                groupUin = payload.groupUin,
                type = 2,
                info = BroadcastGroupFileReq.Body.Info(
                    busiType = 102,
                    fileId = payload.fileId,
                    field3 = Random.nextInt(),
                    field5 = true,
                )
            )
        ).pbEncode()
}
