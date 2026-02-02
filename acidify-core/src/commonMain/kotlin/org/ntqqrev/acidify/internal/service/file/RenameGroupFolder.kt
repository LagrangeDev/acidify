package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D7Req
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D7Resp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object RenameGroupFolder : OidbService<RenameGroupFolder.Req, Unit>(0x6d7, 2, true) {
    class Req(
        val groupUin: Long,
        val folderId: String,
        val newFolderName: String
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        Oidb0x6D7Req(
            renameFolder = Oidb0x6D7Req.RenameFolder(
                groupUin = payload.groupUin,
                folderId = payload.folderId,
                newFolderName = payload.newFolderName,
            )
        ).pbEncode()

    override fun parseOidb(client: LagrangeClient, payload: ByteArray) {
        val resp = payload.pbDecode<Oidb0x6D7Resp>().renameFolder
        val retCode = resp.retCode
        if (retCode != 0) {
            val retMsg = resp.retMsg
            throw Exception("$retCode $retMsg")
        }
    }
}
