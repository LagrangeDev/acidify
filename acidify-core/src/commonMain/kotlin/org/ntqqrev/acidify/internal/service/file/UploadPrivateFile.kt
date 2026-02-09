package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0xE37Req
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0xE37Resp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.checkRetCode
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.internal.util.toIpString

internal object UploadPrivateFile : OidbService<UploadPrivateFile.Req, UploadPrivateFile.Resp>(0xe37, 1700) {
    class Req(
        val senderUid: String,
        val receiverUid: String,
        val fileName: String,
        val fileSize: Int,
        val fileMd5: ByteArray,
        val fileSha1: ByteArray,
        val md510M: ByteArray,
        val fileTriSha1: ByteArray
    )

    class Resp(
        val fileExist: Boolean,
        val fileId: String,
        val uploadKey: ByteArray,
        val ipAndPorts: List<Pair<String, Int>>,
        val fileCrcMedia: String
    )

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray =
        Oidb0xE37Req(
            subCommand = 1700,
            seq = 0,
            uploadBody = Oidb0xE37Req.UploadBody(
                senderUid = payload.senderUid,
                receiverUid = payload.receiverUid,
                fileSize = payload.fileSize,
                fileName = payload.fileName,
                md510MCheckSum = payload.md510M,
                sha1CheckSum = payload.fileSha1,
                localPath = "/",
                md5CheckSum = payload.fileMd5,
                sha3CheckSum = payload.fileTriSha1,
            ),
            field101 = 3,
            field102 = 1,
            field200 = 1,
        ).pbEncode()

    override fun parseOidb(client: AbstractClient, payload: ByteArray): Resp {
        val resp = payload.pbDecode<Oidb0xE37Resp>().uploadBody
        checkRetCode(resp.retCode, resp.retMsg)
        return Resp(
            fileExist = resp.boolFileExist,
            fileId = resp.uuid,
            uploadKey = resp.mediaPlatformUploadKey,
            ipAndPorts = resp.rtpMediaPlatformUploadAddress.map {
                it.inIP.toIpString() to it.inPort
            },
            fileCrcMedia = resp.fileIdCrc
        )
    }
}
