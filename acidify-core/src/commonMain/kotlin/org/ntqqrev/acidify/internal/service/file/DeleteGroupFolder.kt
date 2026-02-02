package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D7Req
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D7Resp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object DeleteGroupFolder : OidbService<DeleteGroupFolder.Req, Unit>(0x6d7, 1, true) {
    class Req(
        val groupUin: Long,
        val folderId: String
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        Oidb0x6D7Req(
            deleteFolder = Oidb0x6D7Req.DeleteFolder(
                groupUin = payload.groupUin,
                folderId = payload.folderId,
            )
        ).pbEncode()

    override fun parseOidb(client: LagrangeClient, payload: ByteArray) {
        val resp = payload.pbDecode<Oidb0x6D7Resp>().deleteFolder
        val retCode = resp.retCode
        if (retCode != 0) {
            val retMsg = resp.retMsg
            throw Exception("$retCode $retMsg")
        }
    }
}
