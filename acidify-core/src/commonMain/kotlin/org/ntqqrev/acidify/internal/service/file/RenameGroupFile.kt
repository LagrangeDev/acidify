package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D6Req
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D6Resp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.checkRetCode
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object RenameGroupFile : OidbService<RenameGroupFile.Req, Unit>(0x6d6, 4, true) {
    class Req(
        val groupUin: Long,
        val fileId: String,
        val parentFolderId: String,
        val newFileName: String
    )

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray =
        Oidb0x6D6Req(
            renameFile = Oidb0x6D6Req.RenameFile(
                groupUin = payload.groupUin,
                appId = 7,
                busId = 102,
                fileId = payload.fileId,
                parentFolderId = payload.parentFolderId,
                newFileName = payload.newFileName,
            )
        ).pbEncode()

    override fun parseOidb(client: AbstractClient, payload: ByteArray) {
        val resp = payload.pbDecode<Oidb0x6D6Resp>().renameFile
        checkRetCode(resp.retCode, resp.retMsg)
    }
}
