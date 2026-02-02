package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D6Req
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x6D6Resp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

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

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        Oidb0x6D6Req(
            uploadFile = Oidb0x6D6Req.UploadFile(
                groupUin = payload.groupUin,
                appId = 7,
                busId = 102,
                entrance = 6,
                parentFolderId = payload.parentFolderId,
                fileName = payload.fileName,
                localPath = "/${payload.fileName}",
                fileSize = payload.fileSize,
                sha = payload.fileSha1,
                sha3 = payload.fileTriSha1,
                md5 = payload.fileMd5,
            )
        ).pbEncode()

    override fun parseOidb(client: LagrangeClient, payload: ByteArray): Resp {
        val resp = payload.pbDecode<Oidb0x6D6Resp>().uploadFile
        val retCode = resp.retCode
        if (retCode != 0) {
            val retMsg = resp.retMsg
            throw Exception("$retCode $retMsg")
        }

        return Resp(
            fileExist = resp.fileExist,
            fileId = resp.fileId,
            fileKey = resp.fileKey,
            checkKey = resp.checkKey,
            uploadIp = resp.uploadIp,
            uploadPort = resp.uploadPort
        )
    }
}
