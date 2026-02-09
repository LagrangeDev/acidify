package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D7Req
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D7Resp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.checkRetCode
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object RenameGroupFolder : OidbService<RenameGroupFolder.Req, Unit>(0x6d7, 2, true) {
    class Req(
        val groupUin: Long,
        val folderId: String,
        val newFolderName: String
    )

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray =
        Oidb0x6D7Req(
            renameFolder = Oidb0x6D7Req.RenameFolder(
                groupUin = payload.groupUin,
                folderId = payload.folderId,
                newFolderName = payload.newFolderName,
            )
        ).pbEncode()

    override fun parseOidb(client: AbstractClient, payload: ByteArray) {
        val resp = payload.pbDecode<Oidb0x6D7Resp>().renameFolder
        checkRetCode(resp.retCode, resp.retMsg)
    }
}
