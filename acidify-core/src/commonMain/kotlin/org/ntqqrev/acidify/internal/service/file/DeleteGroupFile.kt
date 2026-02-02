package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D6Req
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D6Resp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object DeleteGroupFile : OidbService<DeleteGroupFile.Req, Unit>(0x6d6, 3, true) {
    class Req(
        val groupUin: Long,
        val fileId: String
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        Oidb0x6D6Req(
            deleteFile = Oidb0x6D6Req.DeleteFile(
                groupUin = payload.groupUin,
                appId = 7,
                busId = 102,
                parentFolderId = "/",
                fileId = payload.fileId,
            )
        ).pbEncode()

    override fun parseOidb(client: LagrangeClient, payload: ByteArray) {
        val resp = payload.pbDecode<Oidb0x6D6Resp>().deleteFile
        val retCode = resp.retCode
        if (retCode != 0) {
            val retMsg = resp.retMsg
            throw Exception("$retCode $retMsg")
        }
    }
}
