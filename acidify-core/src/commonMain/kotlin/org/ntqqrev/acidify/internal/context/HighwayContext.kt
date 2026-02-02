package org.ntqqrev.acidify.internal.context

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.message.media.*
import org.ntqqrev.acidify.internal.service.system.FetchHighwayInfo
import org.ntqqrev.acidify.internal.util.md5
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.internal.util.toIpString
import org.ntqqrev.acidify.message.MessageScene

internal class HighwayContext(client: LagrangeClient) : AbstractContext(client) {
    private var highwayHost: String = ""
    private var highwayPort: Int = 0
    private var sigSession: ByteArray = ByteArray(0)
    private val httpClient = HttpClient()
    private val logger = client.createLogger(this)

    companion object {
        const val MAX_BLOCK_SIZE = 1024 * 1024 // 1MB
    }

    override suspend fun postOnline() {
        val highwayInfo = client.callService(FetchHighwayInfo)
        val (host, port) = highwayInfo.servers[1]!![0]
        highwayHost = host
        highwayPort = port
        sigSession = highwayInfo.sigSession
        logger.d { "已配置 Highway 服务器: $host:$port" }
    }

    private suspend fun upload(
        cmd: Int,
        data: ByteArray,
        md5: ByteArray,
        extendInfo: ByteArray,
        timeout: Long = 1200_000L // 1200 seconds
    ) {
        try {
            withTimeout(timeout) {
                val session = HttpSession(
                    client = client,
                    httpClient = httpClient,
                    highwayHost = highwayHost,
                    highwayPort = highwayPort,
                    sigSession = sigSession,
                    cmd = cmd,
                    data = data,
                    md5 = md5,
                    extendInfo = extendInfo
                )
                session.upload()
            }
        } catch (_: TimeoutCancellationException) {
            throw Exception("上传超时 (${timeout / 1000}s)")
        }
    }

    suspend fun uploadImage(
        image: ByteArray,
        imageMd5: ByteArray,
        imageSha1: ByteArray,
        uploadResp: UploadResp,
        messageScene: MessageScene,
    ) {
        val cmd = if (messageScene == MessageScene.FRIEND) 1003 else 1004
        upload(
            cmd = cmd,
            data = image,
            md5 = imageMd5,
            extendInfo = buildExtendInfo(uploadResp, imageSha1)
        )
    }

    suspend fun uploadRecord(
        record: ByteArray,
        recordMd5: ByteArray,
        recordSha1: ByteArray,
        uploadResp: UploadResp,
        messageScene: MessageScene,
    ) {
        upload(
            cmd = if (messageScene == MessageScene.FRIEND) 1007 else 1008,
            data = record,
            md5 = recordMd5,
            extendInfo = buildExtendInfo(uploadResp, recordSha1)
        )
    }

    suspend fun uploadVideo(
        video: ByteArray,
        videoMd5: ByteArray,
        videoSha1: ByteArray,
        uploadResp: UploadResp,
        messageScene: MessageScene,
    ) {
        upload(
            cmd = if (messageScene == MessageScene.FRIEND) 1001 else 1005,
            data = video,
            md5 = videoMd5,
            extendInfo = buildExtendInfo(uploadResp, videoSha1)
        )
    }

    suspend fun uploadVideoThumbnail(
        thumbnail: ByteArray,
        thumbnailMd5: ByteArray,
        thumbnailSha1: ByteArray,
        uploadResp: UploadResp,
        messageScene: MessageScene,
    ) {
        upload(
            cmd = if (messageScene == MessageScene.FRIEND) 1002 else 1006,
            data = thumbnail,
            md5 = thumbnailMd5,
            extendInfo = buildExtendInfo(uploadResp, thumbnailSha1, subFileInfoIdx = 0)
        )
    }

    private fun buildExtendInfo(
        uploadResp: UploadResp,
        sha1: ByteArray,
        subFileInfoIdx: Int? = null
    ): ByteArray {
        val msgInfoBodyList: List<MsgInfoBody> = uploadResp.msgInfo.msgInfoBody
        val index = msgInfoBodyList[0].index
        val subFileInfo = subFileInfoIdx?.let { uploadResp.subFileInfos.getOrNull(it) }
        val ipv4s = subFileInfo?.iPv4s ?: uploadResp.iPv4s

        return NTV2RichMediaHighwayExt(
            fileUuid = index.fileUuid,
            uKey = subFileInfo?.uKey ?: uploadResp.uKey,
            network = NTHighwayNetwork(
                iPv4s = ipv4s.map { ipv4 ->
                    NTHighwayIPv4(
                        domain = NTHighwayDomain(
                            isEnable = true,
                            iP = ipv4.outIP.toIpString(reverseEndian = true),
                        ),
                        port = ipv4.outPort,
                    )
                }
            ),
            msgInfoBody = msgInfoBodyList,
            blockSize = MAX_BLOCK_SIZE,
            hash = NTHighwayHash(
                fileSha1 = listOf(sha1)
            ),
        ).pbEncode()
    }

    suspend fun uploadAvatar(imageData: ByteArray) {
        val md5 = imageData.md5()
        upload(90, imageData, md5, ByteArray(0))
    }

    suspend fun uploadGroupAvatar(groupUin: Long, imageData: ByteArray) {
        val md5 = imageData.md5()
        val extra = GroupAvatarExtra(
            type = 101,
            groupUin = groupUin,
            field3 = GroupAvatarExtraField3(field1 = 1),
            field5 = 3,
            field6 = 1,
        ).pbEncode()
        upload(3000, imageData, md5, extra)
    }

    suspend fun uploadPrivateFile(
        receiverUin: Long,
        fileName: String,
        fileData: ByteArray,
        fileMd5: ByteArray,
        fileSha1: ByteArray,
        md510M: ByteArray,
        fileTriSha1: ByteArray,
        fileId: String,
        uploadKey: ByteArray,
        uploadIpAndPorts: List<Pair<String, Int>>
    ) {
        val ext = FileUploadExt(
            unknown1 = 100,
            unknown2 = 1,
            unknown3 = 0,
            entry = FileUploadEntry(
                busiBuff = ExcitingBusiInfo(
                    senderUin = client.sessionStore.uin,
                    receiverUin = receiverUin,
                ),
                fileEntry = ExcitingFileEntry(
                    fileSize = fileData.size.toLong(),
                    md5 = fileMd5,
                    checkKey = fileSha1,
                    md510M = md510M,
                    sha3 = fileTriSha1,
                    fileId = fileId,
                    uploadKey = uploadKey,
                ),
                clientInfo = ExcitingClientInfo(
                    clientType = 3,
                    appId = "100",
                    terminalType = 3,
                    clientVer = "1.1.1",
                    unknown = 4,
                ),
                fileNameInfo = ExcitingFileNameInfo(
                    fileName = fileName,
                ),
                host = ExcitingHostConfig(
                    hosts = uploadIpAndPorts.map { (uploadIp, uploadPort) ->
                        ExcitingHostInfo(
                            url = ExcitingUrlInfo(
                                unknown = 1,
                                host = uploadIp,
                            ),
                            port = uploadPort,
                        )
                    }
                )
            ),
            unknown200 = 1,
        ).pbEncode()

        upload(95, fileData, fileMd5, ext)
    }

    suspend fun uploadGroupFile(
        senderUin: Long,
        groupUin: Long,
        fileName: String,
        fileData: ByteArray,
        fileId: String,
        fileKey: ByteArray,
        checkKey: ByteArray,
        uploadIp: String,
        uploadPort: Int
    ) {
        // 计算前 10MB 的 MD5
        val md510M = fileData.copyOfRange(0, minOf(10 * 1024 * 1024, fileData.size)).md5()

        // 构建上传扩展信息
        val ext = FileUploadExt(
            unknown1 = 100,
            unknown2 = 1,
            entry = FileUploadEntry(
                busiBuff = ExcitingBusiInfo(
                    senderUin = senderUin,
                    receiverUin = groupUin,
                    groupCode = groupUin,
                ),
                fileEntry = ExcitingFileEntry(
                    fileSize = fileData.size.toLong(),
                    md5 = fileData.md5(),
                    checkKey = fileKey,
                    md510M = md510M,
                    fileId = fileId,
                    uploadKey = checkKey,
                ),
                clientInfo = ExcitingClientInfo(
                    clientType = 3,
                    appId = "100",
                    terminalType = 3,
                    clientVer = "1.1.1",
                    unknown = 4,
                ),
                fileNameInfo = ExcitingFileNameInfo(
                    fileName = fileName,
                ),
                host = ExcitingHostConfig(
                    hosts = listOf(
                        ExcitingHostInfo(
                            url = ExcitingUrlInfo(
                                unknown = 1,
                                host = uploadIp,
                            ),
                            port = uploadPort,
                        )
                    )
                )
            )
        ).pbEncode()

        val md5 = fileData.md5()
        upload(71, fileData, md5, ext)
    }

    private class HttpSession(
        private val client: LagrangeClient,
        private val httpClient: HttpClient,
        private val highwayHost: String,
        private val highwayPort: Int,
        private val sigSession: ByteArray,
        private val cmd: Int,
        private val data: ByteArray,
        private val md5: ByteArray,
        private val extendInfo: ByteArray
    ) {
        private val logger = client.createLogger(this)

        suspend fun upload() {
            var offset = 0
            while (offset < data.size) {
                val blockSize = minOf(MAX_BLOCK_SIZE, data.size - offset)
                val block = data.copyOfRange(offset, offset + blockSize)
                uploadBlock(block, offset)
                val progress = (offset.toLong() + blockSize.toLong()) * 100L / data.size.toLong()
                logger.d { "Highway 上传进度: $progress%" }
                offset += blockSize
            }
        }

        private suspend fun uploadBlock(block: ByteArray, offset: Int) {
            val blockMd5 = block.md5()
            val head = buildPicUpHead(offset, block.size, blockMd5)
            val frame = packFrame(head, block)

            val serverUrl =
                "http://$highwayHost:$highwayPort/cgi-bin/httpconn?htcmd=0x6FF0087&uin=${client.sessionStore.uin}"

            val response = httpClient.post(serverUrl) {
                headers {
                    append(HttpHeaders.Connection, "Keep-Alive")
                    append(HttpHeaders.AcceptEncoding, "identity")
                    append(HttpHeaders.UserAgent, "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2)")
                    append(HttpHeaders.ContentLength, frame.size.toString())
                }
                setBody(frame)
            }
            val (responseHead, _) = unpackFrame(response.readRawBytes())

            val headData = responseHead.pbDecode<RespDataHighwayHead>()
            val errorCode = headData.errorCode

            if (errorCode != 0) {
                throw Exception("[Highway] HTTP Upload failed with code $errorCode")
            }
        }

        private fun buildPicUpHead(offset: Int, bodyLength: Int, bodyMd5: ByteArray): ByteArray {
            return ReqDataHighwayHead(
                msgBaseHead = DataHighwayHead(
                    version = 1,
                    uin = client.sessionStore.uin.toString(),
                    command = "PicUp.DataUp",
                    seq = 0,
                    retryTimes = 0,
                    appId = 1600001604,
                    dataFlag = 16,
                    commandId = cmd,
                ),
                msgSegHead = SegHead(
                    serviceId = 0,
                    filesize = data.size.toLong(),
                    dataOffset = offset.toLong(),
                    dataLength = bodyLength,
                    serviceTicket = sigSession,
                    md5 = bodyMd5,
                    fileMd5 = this@HttpSession.md5,
                    cacheAddr = 0,
                    cachePort = 0,
                ),
                bytesReqExtendInfo = extendInfo,
                timestamp = 0L,
                msgLoginSigHead = LoginSigHead(
                    uint32LoginSigType = 8,
                    appId = 1600001604,
                ),
            ).pbEncode()
        }

        private fun packFrame(head: ByteArray, body: ByteArray): ByteArray {
            val totalLength = 9 + head.size + body.size + 1
            val buffer = ByteArray(totalLength)

            buffer[0] = 0x28
            buffer[1] = (head.size ushr 24).toByte()
            buffer[2] = (head.size ushr 16).toByte()
            buffer[3] = (head.size ushr 8).toByte()
            buffer[4] = head.size.toByte()

            buffer[5] = (body.size ushr 24).toByte()
            buffer[6] = (body.size ushr 16).toByte()
            buffer[7] = (body.size ushr 8).toByte()
            buffer[8] = body.size.toByte()

            head.copyInto(buffer, 9)
            body.copyInto(buffer, 9 + head.size)

            buffer[totalLength - 1] = 0x29

            return buffer
        }

        private fun unpackFrame(frame: ByteArray): Pair<ByteArray, ByteArray> {
            require(frame[0] == 0x28.toByte() && frame[frame.size - 1] == 0x29.toByte()) {
                "Invalid frame!"
            }

            val headLen =
                ((frame[1].toInt() and 0xFF) shl 24) or ((frame[2].toInt() and 0xFF) shl 16) or ((frame[3].toInt() and 0xFF) shl 8) or (frame[4].toInt() and 0xFF)

            val bodyLen =
                ((frame[5].toInt() and 0xFF) shl 24) or ((frame[6].toInt() and 0xFF) shl 16) or ((frame[7].toInt() and 0xFF) shl 8) or (frame[8].toInt() and 0xFF)

            val head = frame.copyOfRange(9, 9 + headLen)
            val body = frame.copyOfRange(9 + headLen, 9 + headLen + bodyLen)

            return Pair(head, body)
        }
    }
}