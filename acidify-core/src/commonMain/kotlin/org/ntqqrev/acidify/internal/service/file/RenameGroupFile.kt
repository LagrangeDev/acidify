package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0x6D6Req
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0x6D6Resp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService

internal object RenameGroupFile : OidbService<RenameGroupFile.Req, Unit>(0x6d6, 4, true) {
    class Req(
        val groupUin: Long,
        val fileId: String,
        val parentFolderId: String,
        val newFileName: String
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        Oidb0x6D6Req {
            it[renameFile] = Oidb0x6D6Req.RenameFile {
                it[groupUin] = payload.groupUin
                it[appId] = 7
                it[busId] = 102
                it[fileId] = payload.fileId
                it[parentFolderId] = payload.parentFolderId
                it[newFileName] = payload.newFileName
            }
        }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray) {
        val resp = Oidb0x6D6Resp(payload).get { renameFile }
        val retCode = resp.get { retCode }
        if (retCode != 0) {
            val retMsg = resp.get { retMsg }
            throw Exception("重命名群文件失败: $retCode $retMsg")
        }
    }
}
