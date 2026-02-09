package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D6Req
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D6Resp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.checkRetCode
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object MoveGroupFile : OidbService<MoveGroupFile.Req, Unit>(0x6d6, 5, true) {
    class Req(
        val groupUin: Long,
        val fileId: String,
        val parentFolderId: String,
        val targetFolderId: String
    )

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray =
        Oidb0x6D6Req(
            moveFile = Oidb0x6D6Req.MoveFile(
                groupUin = payload.groupUin,
                appId = 7,
                busId = 102,
                fileId = payload.fileId,
                parentFolderId = payload.parentFolderId,
                destFolderId = payload.targetFolderId,
            )
        ).pbEncode()

    override fun parseOidb(client: AbstractClient, payload: ByteArray) {
        val resp = payload.pbDecode<Oidb0x6D6Resp>().moveFile
        checkRetCode(resp.retCode, resp.retMsg)
    }
}
