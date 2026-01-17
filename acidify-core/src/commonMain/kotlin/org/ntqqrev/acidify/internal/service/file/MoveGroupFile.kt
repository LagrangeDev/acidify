package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0x6D6Req
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0x6D6Resp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService

internal object MoveGroupFile : OidbService<MoveGroupFile.Req, Unit>(0x6d6, 5, true) {
    class Req(
        val groupUin: Long,
        val fileId: String,
        val parentFolderId: String,
        val targetFolderId: String
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        Oidb0x6D6Req {
            it[moveFile] = Oidb0x6D6Req.MoveFile {
                it[groupUin] = payload.groupUin
                it[appId] = 7
                it[busId] = 102
                it[fileId] = payload.fileId
                it[parentFolderId] = payload.parentFolderId
                it[destFolderId] = payload.targetFolderId
            }
        }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray) {
        val resp = Oidb0x6D6Resp(payload).get { moveFile }
        val retCode = resp.get { retCode }
        if (retCode != 0) {
            val retMsg = resp.get { retMsg }
            throw Exception("移动群文件失败: $retCode $retMsg")
        }
    }
}
