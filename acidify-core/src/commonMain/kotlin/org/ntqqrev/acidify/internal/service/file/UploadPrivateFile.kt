package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0xE37Req
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0xE37Resp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService
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

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        Oidb0xE37Req {
            it[subCommand] = 1700
            it[seq] = 0
            it[uploadBody] = Oidb0xE37Req.UploadBody {
                it[senderUid] = payload.senderUid
                it[receiverUid] = payload.receiverUid
                it[fileSize] = payload.fileSize
                it[fileName] = payload.fileName
                it[md510MCheckSum] = payload.md510M
                it[sha1CheckSum] = payload.fileSha1
                it[localPath] = "/"
                it[md5CheckSum] = payload.fileMd5
                it[sha3CheckSum] = payload.fileTriSha1
            }
            it[field101] = 3
            it[field102] = 1
            it[field200] = 1
        }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray): Resp {
        val resp = Oidb0xE37Resp(payload).get { uploadBody }
        val retCode = resp.get { retCode }
        if (retCode != 0) {
            val retMsg = resp.get { retMsg }
            throw Exception("上传私聊文件失败: $retCode $retMsg")
        }
        return Resp(
            fileExist = resp.get { boolFileExist },
            fileId = resp.get { uuid },
            uploadKey = resp.get { mediaPlatformUploadKey },
            ipAndPorts = resp.get { rtpMediaPlatformUploadAddress }.map {
                it.get { inIP }.toIpString(reverseEndian = true) to it.get { inPort }
            },
            fileCrcMedia = resp.get { fileIdCrc }
        )
    }
}