package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.message.media.*
import org.ntqqrev.acidify.internal.protobuf.PbObject
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.message.MessageScene
import kotlin.random.Random

internal abstract class RichMediaUpload<T>(
    oidbCommand: Int,
    oidbService: Int,
    val requestId: Int,
    val requestType: Int,
    val businessType: Int,
    val scene: MessageScene,
) : OidbService<T, PbObject<UploadResp>>(oidbCommand, oidbService, true) {
    class ImageUploadRequest(
        val imageData: ByteArray,
        val imageMd5: String,
        val imageSha1: String,
        val imageExt: String,
        val width: Int,
        val height: Int,
        val picFormat: Int,
        val subType: Int,
        val textSummary: String,
        val groupUin: Long? = null
    )

    class RecordUploadRequest(
        val audioData: ByteArray,
        val audioMd5: String,
        val audioSha1: String,
        val audioDuration: Int,
        val groupUin: Long? = null
    )

    class VideoUploadRequest(
        val videoData: ByteArray,
        val videoMd5: String,
        val videoSha1: String,
        val videoWidth: Int,
        val videoHeight: Int,
        val videoDuration: Int,
        val thumbnailData: ByteArray,
        val thumbnailMd5: String,
        val thumbnailSha1: String,
        val thumbnailExt: String,
        val thumbnailPicFormat: Int,
        val groupUin: Long? = null
    )

    protected fun buildBaseUploadReq(
        client: IClient,
        uploadInfoList: List<PbObject<UploadInfo>>,
        compatQMsgSceneType: Int,
        extBizInfo: PbObject<ExtBizInfo>,
        groupUin: Long? = null
    ): ByteArray = NTV2RichMediaReq {
        it[reqHead] = MultiMediaReqHead {
            it[common] = CommonHead {
                it[requestId] = this@RichMediaUpload.requestId
                it[command] = oidbService
            }
            it[scene] = SceneInfo {
                it[requestType] = this@RichMediaUpload.requestType
                it[businessType] = this@RichMediaUpload.businessType
                when (this@RichMediaUpload.scene) {
                    MessageScene.FRIEND -> {
                        it[sceneType] = 1
                        it[c2C] = C2CUserInfo {
                            it[accountType] = 2
                            it[targetUid] = client.uid
                        }
                    }

                    MessageScene.GROUP -> {
                        it[sceneType] = 2
                        it[group] = GroupInfo {
                            it[this.groupUin] = groupUin!!
                        }
                    }

                    else -> {}
                }
            }
            it[this.client] = ClientMeta {
                it[agentType] = 2
            }
        }
        it[upload] = UploadReq {
            it[uploadInfo] = uploadInfoList
            it[tryFastUploadCompleted] = true
            it[srvSendMsg] = false
            it[clientRandomId] = Random.nextLong()
            it[this.compatQMsgSceneType] = compatQMsgSceneType
            it[this.extBizInfo] = extBizInfo
            it[noNeedCompatMsg] = false
        }
    }.toByteArray()

    protected fun buildImageUploadInfo(payload: ImageUploadRequest): PbObject<UploadInfo> =
        UploadInfo {
            it[fileInfo] = FileInfo {
                it[fileSize] = payload.imageData.size
                it[fileHash] = payload.imageMd5
                it[fileSha1] = payload.imageSha1
                it[fileName] = payload.imageMd5.uppercase() + payload.imageExt
                it[type] = FileType {
                    it[type] = 1
                    it[picFormat] = payload.picFormat
                    it[videoFormat] = 0
                    it[voiceFormat] = 0
                }
                it[width] = payload.width
                it[height] = payload.height
                it[time] = 0
                it[original] = 1
            }
            it[subFileType] = 0
        }

    protected fun buildImageExtBizInfo(scene: MessageScene, subType: Int, textSummary: String): PbObject<ExtBizInfo> =
        ExtBizInfo {
            it[pic] = PicExtBizInfo {
                it[bizType] = subType
                it[when (scene) {
                    MessageScene.FRIEND -> bytesPbReserveC2C
                    MessageScene.GROUP -> bytesPbReserveTroop
                    else -> bytesPbReserveTroop
                }] = PicExtBizInfo.PbReserve {
                    it[this.subType] = subType
                }
                it[this.textSummary] = textSummary.ifEmpty { if (subType == 1) "[动画表情]" else "[图片]" }
            }
        }

    protected fun buildRecordUploadInfo(payload: RecordUploadRequest): PbObject<UploadInfo> =
        UploadInfo {
            it[fileInfo] = FileInfo {
                it[fileSize] = payload.audioData.size
                it[fileHash] = payload.audioMd5
                it[fileSha1] = payload.audioSha1
                it[fileName] = payload.audioMd5 + ".amr"
                it[type] = FileType {
                    it[type] = 3
                    it[picFormat] = 0
                    it[videoFormat] = 0
                    it[voiceFormat] = 1
                }
                it[width] = 0
                it[height] = 0
                it[time] = payload.audioDuration
                it[original] = 0
            }
            it[subFileType] = 0
        }

    protected fun buildPrivateRecordExtBizInfo(): PbObject<ExtBizInfo> =
        ExtBizInfo {
            it[pic] = PicExtBizInfo {
                it[textSummary] = ""
            }
            it[video] = VideoExtBizInfo {
                it[bytesPbReserve] = ByteArray(0)
            }
            it[ptt] = PttExtBizInfo {
                it[bytesReserve] = byteArrayOf(0x08, 0x00, 0x38, 0x00)
                it[bytesPbReserve] = ByteArray(0)
                it[bytesGeneralFlags] = byteArrayOf(
                    0x9a.toByte(),
                    0x01,
                    0x0b,
                    0xaa.toByte(),
                    0x03,
                    0x08,
                    0x08,
                    0x04,
                    0x12,
                    0x04,
                    0x00,
                    0x00,
                    0x00,
                    0x00
                )
            }
        }

    protected fun buildGroupRecordExtBizInfo(): PbObject<ExtBizInfo> =
        ExtBizInfo {
            it[pic] = PicExtBizInfo {
                it[textSummary] = ""
            }
            it[video] = VideoExtBizInfo {
                it[bytesPbReserve] = ByteArray(0)
            }
            it[ptt] = PttExtBizInfo {
                it[bytesReserve] = ByteArray(0)
                it[bytesPbReserve] = byteArrayOf(0x08, 0x00, 0x38, 0x00)
                it[bytesGeneralFlags] =
                    byteArrayOf(0x9a.toByte(), 0x01, 0x07, 0xaa.toByte(), 0x03, 0x04, 0x08, 0x08, 0x12, 0x00)
            }
        }

    protected fun buildVideoUploadInfoList(payload: VideoUploadRequest): List<PbObject<UploadInfo>> = listOf(
        UploadInfo {
            it[fileInfo] = FileInfo {
                it[fileSize] = payload.videoData.size
                it[fileHash] = payload.videoMd5
                it[fileSha1] = payload.videoSha1
                it[fileName] = "video.mp4"
                it[type] = FileType {
                    it[type] = 2
                    it[picFormat] = 0
                    it[videoFormat] = 0
                    it[voiceFormat] = 0
                }
                it[width] = payload.videoWidth
                it[height] = payload.videoHeight
                it[time] = payload.videoDuration
                it[original] = 0
            }
            it[subFileType] = 0
        },
        UploadInfo {
            it[fileInfo] = FileInfo {
                it[fileSize] = payload.thumbnailData.size
                it[fileHash] = payload.thumbnailMd5
                it[fileSha1] = payload.thumbnailSha1
                it[fileName] = "video." + payload.thumbnailExt
                it[type] = FileType {
                    it[type] = 1
                    it[picFormat] = payload.thumbnailPicFormat
                    it[videoFormat] = 0
                    it[voiceFormat] = 0
                }
                it[width] = payload.videoWidth
                it[height] = payload.videoHeight
                it[time] = 0
                it[original] = 0
            }
            it[subFileType] = 100
        }
    )

    protected fun buildVideoExtBizInfo(): PbObject<ExtBizInfo> =
        ExtBizInfo {
            it[pic] = PicExtBizInfo {
                it[bizType] = 0
                it[textSummary] = ""
            }
            it[video] = VideoExtBizInfo {
                it[bytesPbReserve] = byteArrayOf(0x80.toByte(), 0x01, 0x00)
            }
            it[ptt] = PttExtBizInfo {
                it[bytesReserve] = ByteArray(0)
                it[bytesPbReserve] = ByteArray(0)
                it[bytesGeneralFlags] = ByteArray(0)
            }
        }

    object PrivateImage : RichMediaUpload<ImageUploadRequest>(
        oidbCommand = 0x11c5,
        oidbService = 100,
        requestId = 1,
        requestType = 2,
        businessType = 1,
        scene = MessageScene.FRIEND
    ) {
        override fun buildOidb(client: IClient, payload: ImageUploadRequest): ByteArray {
            val uploadInfoList = listOf(buildImageUploadInfo(payload))
            val extBizInfo = buildImageExtBizInfo(MessageScene.FRIEND, payload.subType, payload.textSummary)
            return buildBaseUploadReq(client, uploadInfoList, 1, extBizInfo)
        }

        override fun parseOidb(client: IClient, payload: ByteArray): PbObject<UploadResp> =
            NTV2RichMediaResp(payload).get { upload }
    }

    object GroupImage : RichMediaUpload<ImageUploadRequest>(
        oidbCommand = 0x11c4,
        oidbService = 100,
        requestId = 1,
        requestType = 2,
        businessType = 1,
        scene = MessageScene.GROUP
    ) {
        override fun buildOidb(client: IClient, payload: ImageUploadRequest): ByteArray {
            val uploadInfoList = listOf(buildImageUploadInfo(payload))
            val extBizInfo = buildImageExtBizInfo(MessageScene.GROUP, payload.subType, payload.textSummary)
            return buildBaseUploadReq(client, uploadInfoList, 2, extBizInfo, payload.groupUin)
        }

        override fun parseOidb(client: IClient, payload: ByteArray): PbObject<UploadResp> =
            NTV2RichMediaResp(payload).get { upload }
    }

    object PrivateRecord : RichMediaUpload<RecordUploadRequest>(
        oidbCommand = 0x126d,
        oidbService = 100,
        requestId = 4,
        requestType = 2,
        businessType = 3,
        scene = MessageScene.FRIEND
    ) {
        override fun buildOidb(client: IClient, payload: RecordUploadRequest): ByteArray {
            val uploadInfoList = listOf(buildRecordUploadInfo(payload))
            val extBizInfo = buildPrivateRecordExtBizInfo()
            return buildBaseUploadReq(client, uploadInfoList, 1, extBizInfo)
        }

        override fun parseOidb(client: IClient, payload: ByteArray): PbObject<UploadResp> =
            NTV2RichMediaResp(payload).get { upload }
    }

    object GroupRecord : RichMediaUpload<RecordUploadRequest>(
        oidbCommand = 0x126e,
        oidbService = 100,
        requestId = 1,
        requestType = 2,
        businessType = 3,
        scene = MessageScene.GROUP
    ) {
        override fun buildOidb(client: IClient, payload: RecordUploadRequest): ByteArray {
            val uploadInfoList = listOf(buildRecordUploadInfo(payload))
            val extBizInfo = buildGroupRecordExtBizInfo()
            return buildBaseUploadReq(client, uploadInfoList, 2, extBizInfo, payload.groupUin)
        }

        override fun parseOidb(client: IClient, payload: ByteArray): PbObject<UploadResp> =
            NTV2RichMediaResp(payload).get { upload }
    }

    object PrivateVideo : RichMediaUpload<VideoUploadRequest>(
        oidbCommand = 0x11e9,
        oidbService = 100,
        requestId = 3,
        requestType = 2,
        businessType = 2,
        scene = MessageScene.FRIEND
    ) {
        override fun buildOidb(client: IClient, payload: VideoUploadRequest): ByteArray {
            val uploadInfoList = buildVideoUploadInfoList(payload)
            val extBizInfo = buildVideoExtBizInfo()
            return buildBaseUploadReq(client, uploadInfoList, 2, extBizInfo)
        }

        override fun parseOidb(client: IClient, payload: ByteArray): PbObject<UploadResp> =
            NTV2RichMediaResp(payload).get { upload }
    }

    object GroupVideo : RichMediaUpload<VideoUploadRequest>(
        oidbCommand = 0x11ea,
        oidbService = 100,
        requestId = 3,
        requestType = 2,
        businessType = 2,
        scene = MessageScene.GROUP
    ) {
        override fun buildOidb(client: IClient, payload: VideoUploadRequest): ByteArray {
            val uploadInfoList = buildVideoUploadInfoList(payload)
            val extBizInfo = buildVideoExtBizInfo()
            return buildBaseUploadReq(client, uploadInfoList, 2, extBizInfo, payload.groupUin)
        }

        override fun parseOidb(client: IClient, payload: ByteArray): PbObject<UploadResp> =
            NTV2RichMediaResp(payload).get { upload }
    }
}
