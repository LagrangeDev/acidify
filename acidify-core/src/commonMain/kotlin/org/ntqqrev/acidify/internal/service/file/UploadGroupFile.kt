package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0x6D6Req
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0x6D6Resp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService

internal object UploadGroupFile : OidbService<UploadGroupFile.Req, UploadGroupFile.Resp>(0x6d6, 0, true) {
    class Req(
        val groupUin: Long,
        val fileName: String,
        val fileSize: Long,
        val fileMd5: ByteArray,
        val fileSha1: ByteArray,
        val fileTriSha1: ByteArray,
        val parentFolderId: String
    )

    class Resp(
        val fileExist: Boolean,
        val fileId: String,
        val fileKey: ByteArray,
        val checkKey: ByteArray,
        val uploadIp: String,
        val uploadPort: Int
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray {
        return Oidb0x6D6Req {
            it[uploadFile] = Oidb0x6D6Req.UploadFile {
                it[groupUin] = payload.groupUin
                it[appId] = 7
                it[busId] = 102
                it[entrance] = 6
                it[parentFolderId] = payload.parentFolderId
                it[fileName] = payload.fileName
                it[localPath] = "/${payload.fileName}"
                it[fileSize] = payload.fileSize
                it[sha] = payload.fileSha1
                it[sha3] = payload.fileTriSha1
                it[md5] = payload.fileMd5
            }
        }.toByteArray()
    }

    override fun parseOidb(client: IClient, payload: ByteArray): Resp {
        val resp = Oidb0x6D6Resp(payload).get { uploadFile }
        val retCode = resp.get { retCode }
        if (retCode != 0) {
            val retMsg = resp.get { retMsg }
            throw Exception("上传群文件失败: $retCode $retMsg")
        }

        return Resp(
            fileExist = resp.get { fileExist },
            fileId = resp.get { fileId },
            fileKey = resp.get { fileKey },
            checkKey = resp.get { checkKey },
            uploadIp = resp.get { uploadIp },
            uploadPort = resp.get { uploadPort }
        )
    }
}
