package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0x6D6Req
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0x6D6Resp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService

internal object DeleteGroupFile : OidbService<DeleteGroupFile.Req, Unit>(0x6d6, 3, true) {
    class Req(
        val groupUin: Long,
        val fileId: String
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        Oidb0x6D6Req {
            it[deleteFile] = Oidb0x6D6Req.DeleteFile {
                it[groupUin] = payload.groupUin
                it[appId] = 7
                it[busId] = 102
                it[parentFolderId] = "/"
                it[fileId] = payload.fileId
            }
        }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray) {
        val resp = Oidb0x6D6Resp(payload).get { deleteFile }
        val retCode = resp.get { retCode }
        if (retCode != 0) {
            val retMsg = resp.get { retMsg }
            throw Exception("删除群文件失败: $retCode $retMsg")
        }
    }
}
