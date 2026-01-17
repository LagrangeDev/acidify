package org.ntqqrev.acidify.internal.context

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.message.media.*
import org.ntqqrev.acidify.internal.protobuf.PbObject
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.system.FetchHighwayInfo
import org.ntqqrev.acidify.internal.util.md5
import org.ntqqrev.acidify.internal.util.toIpString
import org.ntqqrev.acidify.message.MessageScene

internal class HighwayContext(client: IClient) : AbstractContext(client) {
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
        uploadResp: PbObject<UploadResp>,
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
        uploadResp: PbObject<UploadResp>,
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
        uploadResp: PbObject<UploadResp>,
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
        uploadResp: PbObject<UploadResp>,
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
        uploadResp: PbObject<UploadResp>,
        sha1: ByteArray,
        subFileInfoIdx: Int? = null
    ): ByteArray {
        val msgInfoBodyList = uploadResp.get { msgInfo }.get { msgInfoBody }
        val index = msgInfoBodyList[0].get { index }

        return NTV2RichMediaHighwayExt {
            it[fileUuid] = index.get { fileUuid }
            it[uKey] = if (subFileInfoIdx != null) {
                uploadResp.get { subFileInfos }[subFileInfoIdx].get { uKey }
            } else {
                uploadResp.get { uKey }
            }
            it[network] = NTHighwayNetwork {
                it[iPv4s] = (if (subFileInfoIdx != null) {
                    uploadResp.get { subFileInfos }[subFileInfoIdx].get { iPv4s }
                } else {
                    uploadResp.get { iPv4s }
                }).map { ipv4 ->
                    NTHighwayIPv4 {
                        it[domain] = NTHighwayDomain {
                            it[isEnable] = true
                            it[iP] = ipv4.get { outIP }.toIpString(reverseEndian = true)
                        }
                        it[port] = ipv4.get { outPort }
                    }
                }
            }
            it[msgInfoBody] = msgInfoBodyList
            it[blockSize] = MAX_BLOCK_SIZE
            it[hash] = NTHighwayHash {
                it[fileSha1] = listOf(sha1)
            }
        }.toByteArray()
    }

    suspend fun uploadAvatar(imageData: ByteArray) {
        val md5 = imageData.md5()
        upload(90, imageData, md5, ByteArray(0))
    }

    suspend fun uploadGroupAvatar(groupUin: Long, imageData: ByteArray) {
        val md5 = imageData.md5()
        val extra = GroupAvatarExtra {
            it[type] = 101
            it[this.groupUin] = groupUin
            it[field3] = GroupAvatarExtraField3 {
                it[field1] = 1
            }
            it[field5] = 3
            it[field6] = 1
        }.toByteArray()
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
        val ext = FileUploadExt {
            it[unknown1] = 100
            it[unknown2] = 1
            it[unknown3] = 0
            it[entry] = FileUploadEntry {
                it[busiBuff] = ExcitingBusiInfo {
                    it[senderUin] = client.uin
                    it[this.receiverUin] = receiverUin
                }
                it[fileEntry] = ExcitingFileEntry {
                    it[fileSize] = fileData.size.toLong()
                    it[md5] = fileMd5
                    it[checkKey] = fileSha1
                    it[this.md510M] = md510M
                    it[sha3] = fileTriSha1
                    it[this.fileId] = fileId
                    it[this.uploadKey] = uploadKey
                }
                it[clientInfo] = ExcitingClientInfo {
                    it[clientType] = 3
                    it[appId] = "100"
                    it[terminalType] = 3
                    it[clientVer] = "1.1.1"
                    it[unknown] = 4
                }
                it[fileNameInfo] = ExcitingFileNameInfo {
                    it[this.fileName] = fileName
                }
                it[host] = ExcitingHostConfig {
                    it[hosts] = uploadIpAndPorts.map { (uploadIp, uploadPort) ->
                        ExcitingHostInfo {
                            it[url] = ExcitingUrlInfo {
                                it[unknown] = 1
                                it[host] = uploadIp
                            }
                            it[port] = uploadPort
                        }
                    }
                }
            }
            it[unknown200] = 1
        }.toByteArray()

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
        val ext = FileUploadExt {
            it[unknown1] = 100
            it[unknown2] = 1
            it[entry] = FileUploadEntry {
                it[busiBuff] = ExcitingBusiInfo {
                    it[this.senderUin] = senderUin
                    it[receiverUin] = groupUin
                    it[groupCode] = groupUin
                }
                it[fileEntry] = ExcitingFileEntry {
                    it[fileSize] = fileData.size.toLong()
                    it[md5] = fileData.md5()
                    it[this.checkKey] = fileKey
                    it[this.md510M] = md510M
                    it[this.fileId] = fileId
                    it[uploadKey] = checkKey
                }
                it[clientInfo] = ExcitingClientInfo {
                    it[clientType] = 3
                    it[appId] = "100"
                    it[terminalType] = 3
                    it[clientVer] = "1.1.1"
                    it[unknown] = 4
                }
                it[fileNameInfo] = ExcitingFileNameInfo {
                    it[this.fileName] = fileName
                }
                it[host] = ExcitingHostConfig {
                    it[hosts] = listOf(
                        ExcitingHostInfo {
                            it[url] = ExcitingUrlInfo {
                                it[unknown] = 1
                                it[host] = uploadIp
                            }
                            it[port] = uploadPort
                        }
                    )
                }
            }
        }.toByteArray()

        val md5 = fileData.md5()
        upload(71, fileData, md5, ext)
    }

    private class HttpSession(
        private val client: IClient,
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
                "http://$highwayHost:$highwayPort/cgi-bin/httpconn?htcmd=0x6FF0087&uin=${client.uin}"

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

            val headData = RespDataHighwayHead(responseHead)
            val errorCode = headData.get { errorCode }

            if (errorCode != 0) {
                throw Exception("[Highway] HTTP Upload failed with code $errorCode")
            }
        }

        private fun buildPicUpHead(offset: Int, bodyLength: Int, bodyMd5: ByteArray): ByteArray {
            return ReqDataHighwayHead {
                it[msgBaseHead] = DataHighwayHead {
                    it[version] = 1
                    it[uin] = client.uin.toString()
                    it[command] = "PicUp.DataUp"
                    it[seq] = 0
                    it[retryTimes] = 0
                    it[appId] = 1600001604
                    it[dataFlag] = 16
                    it[commandId] = cmd
                }
                it[msgSegHead] = SegHead {
                    it[serviceId] = 0
                    it[filesize] = data.size.toLong()
                    it[dataOffset] = offset.toLong()
                    it[dataLength] = bodyLength
                    it[serviceTicket] = sigSession
                    it[md5] = bodyMd5
                    it[fileMd5] = this@HttpSession.md5
                    it[cacheAddr] = 0
                    it[cachePort] = 0
                }
                it[bytesReqExtendInfo] = extendInfo
                it[timestamp] = 0L
                it[msgLoginSigHead] = LoginSigHead {
                    it[uint32LoginSigType] = 8
                    it[appId] = 1600001604
                }
            }.toByteArray()
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