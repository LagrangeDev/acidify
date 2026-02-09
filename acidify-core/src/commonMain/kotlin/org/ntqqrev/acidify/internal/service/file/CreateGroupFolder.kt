package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D7Req
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D7Resp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.checkRetCode
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object CreateGroupFolder : OidbService<CreateGroupFolder.Req, CreateGroupFolder.Resp>(0x6d7, 0, true) {
    class Req(
        val groupUin: Long,
        val folderName: String
    )

    class Resp(
        val folderId: String
    )

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray =
        Oidb0x6D7Req(
            createFolder = Oidb0x6D7Req.CreateFolder(
                groupUin = payload.groupUin,
                rootDirectory = "/",
                folderName = payload.folderName,
            )
        ).pbEncode()

    override fun parseOidb(client: AbstractClient, payload: ByteArray): Resp {
        val resp = payload.pbDecode<Oidb0x6D7Resp>().createFolder
        checkRetCode(resp.retCode, resp.retMsg)
        return Resp(folderId = resp.folderId)
    }
}
