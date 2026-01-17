package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0x6D7Req
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0x6D7Resp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService

internal object RenameGroupFolder : OidbService<RenameGroupFolder.Req, Unit>(0x6d7, 2, true) {
    class Req(
        val groupUin: Long,
        val folderId: String,
        val newFolderName: String
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        Oidb0x6D7Req {
            it[renameFolder] = Oidb0x6D7Req.RenameFolder {
                it[groupUin] = payload.groupUin
                it[folderId] = payload.folderId
                it[newFolderName] = payload.newFolderName
            }
        }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray) {
        val resp = Oidb0x6D7Resp(payload).get { renameFolder }
        val retCode = resp.get { retCode }
        if (retCode != 0) {
            val retMsg = resp.get { retMsg }
            throw Exception("重命名群文件夹失败: $retCode $retMsg")
        }
    }
}
