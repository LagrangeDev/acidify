package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D9Req
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object PersistGroupFile : NoOutputOidbService<PersistGroupFile.Req>(0x6d9, 0, true) {
    class Req(
        val groupUin: Long,
        val fileId: String,
    )

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray =
        Oidb0x6D9Req(
            persistBody = Oidb0x6D9Req.PersistBody(
                groupUin = payload.groupUin,
                busiType = 102,
                fileId = payload.fileId,
            )
        ).pbEncode()
}