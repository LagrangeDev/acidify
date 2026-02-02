package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.message.media.*
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.message.MessageScene
import kotlin.random.Random

internal abstract class RichMediaUpload<T>(
    oidbCommand: Int,
    oidbService: Int,
    val requestId: Int,
    val requestType: Int,
    val businessType: Int,
    val scene: MessageScene,
) : OidbService<T, UploadResp>(oidbCommand, oidbService, true) {
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
        client: LagrangeClient,
        uploadInfoList: List<UploadInfo>,
        compatQMsgSceneType: Int,
        extBizInfo: ExtBizInfo,
        groupUin: Long? = null
    ): ByteArray = NTV2RichMediaReq(
        reqHead = MultiMediaReqHead(
            common = CommonHead(
                requestId = requestId,
                command = oidbService,
            ),
            scene = SceneInfo(
                requestType = requestType,
                businessType = businessType,
                sceneType = when (scene) {
                    MessageScene.FRIEND -> 1
                    MessageScene.GROUP -> 2
                    else -> 0
                },
                c2C = if (scene == MessageScene.FRIEND) {
                    C2CUserInfo(
                        accountType = 2,
                        targetUid = client.sessionStore.uid,
                    )
                } else {
                    C2CUserInfo()
                },
                group = if (scene == MessageScene.GROUP) {
                    GroupInfo(groupUin = groupUin ?: 0L)
                } else {
                    GroupInfo()
                }
            ),
            client = ClientMeta(agentType = 2),
        ),
        upload = UploadReq(
            uploadInfo = uploadInfoList,
            tryFastUploadCompleted = true,
            srvSendMsg = false,
            clientRandomId = Random.nextLong(),
            compatQMsgSceneType = compatQMsgSceneType,
            extBizInfo = extBizInfo,
            noNeedCompatMsg = false,
        ),
    ).pbEncode()

    protected fun buildImageUploadInfo(payload: ImageUploadRequest): UploadInfo =
        UploadInfo(
            fileInfo = FileInfo(
                fileSize = payload.imageData.size,
                fileHash = payload.imageMd5,
                fileSha1 = payload.imageSha1,
                fileName = payload.imageMd5.uppercase() + payload.imageExt,
                type = FileType(
                    type = 1,
                    picFormat = payload.picFormat,
                    videoFormat = 0,
                    voiceFormat = 0,
                ),
                width = payload.width,
                height = payload.height,
                time = 0,
                original = 1,
            ),
            subFileType = 0,
        )

    protected fun buildImageExtBizInfo(scene: MessageScene, subType: Int, textSummary: String): ExtBizInfo =
        ExtBizInfo(
            pic = PicExtBizInfo(
                bizType = subType,
                textSummary = textSummary.ifEmpty { if (subType == 1) "[动画表情]" else "[图片]" },
                bytesPbReserveC2C = if (scene == MessageScene.FRIEND) {
                    PicExtBizInfo.PbReserve(subType = subType)
                } else {
                    PicExtBizInfo.PbReserve()
                },
                bytesPbReserveTroop = if (scene == MessageScene.GROUP) {
                    PicExtBizInfo.PbReserve(subType = subType)
                } else {
                    PicExtBizInfo.PbReserve()
                },
            )
        )

    protected fun buildRecordUploadInfo(payload: RecordUploadRequest): UploadInfo =
        UploadInfo(
            fileInfo = FileInfo(
                fileSize = payload.audioData.size,
                fileHash = payload.audioMd5,
                fileSha1 = payload.audioSha1,
                fileName = payload.audioMd5 + ".amr",
                type = FileType(
                    type = 3,
                    picFormat = 0,
                    videoFormat = 0,
                    voiceFormat = 1,
                ),
                width = 0,
                height = 0,
                time = payload.audioDuration,
                original = 0,
            ),
            subFileType = 0,
        )

    protected fun buildPrivateRecordExtBizInfo(): ExtBizInfo =
        ExtBizInfo(
            pic = PicExtBizInfo(textSummary = ""),
            video = VideoExtBizInfo(bytesPbReserve = ByteArray(0)),
            ptt = PttExtBizInfo(
                bytesReserve = byteArrayOf(0x08, 0x00, 0x38, 0x00),
                bytesPbReserve = ByteArray(0),
                bytesGeneralFlags = byteArrayOf(
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
            )
        )

    protected fun buildGroupRecordExtBizInfo(): ExtBizInfo =
        ExtBizInfo(
            pic = PicExtBizInfo(textSummary = ""),
            video = VideoExtBizInfo(bytesPbReserve = ByteArray(0)),
            ptt = PttExtBizInfo(
                bytesReserve = ByteArray(0),
                bytesPbReserve = byteArrayOf(0x08, 0x00, 0x38, 0x00),
                bytesGeneralFlags = byteArrayOf(
                    0x9a.toByte(),
                    0x01,
                    0x07,
                    0xaa.toByte(),
                    0x03,
                    0x04,
                    0x08,
                    0x08,
                    0x12,
                    0x00
                )
            )
        )

    protected fun buildVideoUploadInfoList(payload: VideoUploadRequest): List<UploadInfo> = listOf(
        UploadInfo(
            fileInfo = FileInfo(
                fileSize = payload.videoData.size,
                fileHash = payload.videoMd5,
                fileSha1 = payload.videoSha1,
                fileName = "video.mp4",
                type = FileType(
                    type = 2,
                    picFormat = 0,
                    videoFormat = 0,
                    voiceFormat = 0,
                ),
                width = payload.videoWidth,
                height = payload.videoHeight,
                time = payload.videoDuration,
                original = 0,
            ),
            subFileType = 0,
        ),
        UploadInfo(
            fileInfo = FileInfo(
                fileSize = payload.thumbnailData.size,
                fileHash = payload.thumbnailMd5,
                fileSha1 = payload.thumbnailSha1,
                fileName = "video." + payload.thumbnailExt,
                type = FileType(
                    type = 1,
                    picFormat = payload.thumbnailPicFormat,
                    videoFormat = 0,
                    voiceFormat = 0,
                ),
                width = payload.videoWidth,
                height = payload.videoHeight,
                time = 0,
                original = 0,
            ),
            subFileType = 100,
        )
    )

    protected fun buildVideoExtBizInfo(): ExtBizInfo =
        ExtBizInfo(
            pic = PicExtBizInfo(
                bizType = 0,
                textSummary = "",
            ),
            video = VideoExtBizInfo(
                bytesPbReserve = byteArrayOf(0x80.toByte(), 0x01, 0x00)
            ),
            ptt = PttExtBizInfo(
                bytesReserve = ByteArray(0),
                bytesPbReserve = ByteArray(0),
                bytesGeneralFlags = ByteArray(0),
            )
        )

    object PrivateImage : RichMediaUpload<ImageUploadRequest>(
        oidbCommand = 0x11c5,
        oidbService = 100,
        requestId = 1,
        requestType = 2,
        businessType = 1,
        scene = MessageScene.FRIEND
    ) {
        override fun buildOidb(client: LagrangeClient, payload: ImageUploadRequest): ByteArray {
            val uploadInfoList = listOf(buildImageUploadInfo(payload))
            val extBizInfo = buildImageExtBizInfo(MessageScene.FRIEND, payload.subType, payload.textSummary)
            return buildBaseUploadReq(client, uploadInfoList, 1, extBizInfo)
        }

        override fun parseOidb(client: LagrangeClient, payload: ByteArray): UploadResp =
            payload.pbDecode<NTV2RichMediaResp>().upload
    }

    object GroupImage : RichMediaUpload<ImageUploadRequest>(
        oidbCommand = 0x11c4,
        oidbService = 100,
        requestId = 1,
        requestType = 2,
        businessType = 1,
        scene = MessageScene.GROUP
    ) {
        override fun buildOidb(client: LagrangeClient, payload: ImageUploadRequest): ByteArray {
            val uploadInfoList = listOf(buildImageUploadInfo(payload))
            val extBizInfo = buildImageExtBizInfo(MessageScene.GROUP, payload.subType, payload.textSummary)
            return buildBaseUploadReq(client, uploadInfoList, 2, extBizInfo, payload.groupUin)
        }

        override fun parseOidb(client: LagrangeClient, payload: ByteArray): UploadResp =
            payload.pbDecode<NTV2RichMediaResp>().upload
    }

    object PrivateRecord : RichMediaUpload<RecordUploadRequest>(
        oidbCommand = 0x126d,
        oidbService = 100,
        requestId = 4,
        requestType = 2,
        businessType = 3,
        scene = MessageScene.FRIEND
    ) {
        override fun buildOidb(client: LagrangeClient, payload: RecordUploadRequest): ByteArray {
            val uploadInfoList = listOf(buildRecordUploadInfo(payload))
            val extBizInfo = buildPrivateRecordExtBizInfo()
            return buildBaseUploadReq(client, uploadInfoList, 1, extBizInfo)
        }

        override fun parseOidb(client: LagrangeClient, payload: ByteArray): UploadResp =
            payload.pbDecode<NTV2RichMediaResp>().upload
    }

    object GroupRecord : RichMediaUpload<RecordUploadRequest>(
        oidbCommand = 0x126e,
        oidbService = 100,
        requestId = 1,
        requestType = 2,
        businessType = 3,
        scene = MessageScene.GROUP
    ) {
        override fun buildOidb(client: LagrangeClient, payload: RecordUploadRequest): ByteArray {
            val uploadInfoList = listOf(buildRecordUploadInfo(payload))
            val extBizInfo = buildGroupRecordExtBizInfo()
            return buildBaseUploadReq(client, uploadInfoList, 2, extBizInfo, payload.groupUin)
        }

        override fun parseOidb(client: LagrangeClient, payload: ByteArray): UploadResp =
            payload.pbDecode<NTV2RichMediaResp>().upload
    }

    object PrivateVideo : RichMediaUpload<VideoUploadRequest>(
        oidbCommand = 0x11e9,
        oidbService = 100,
        requestId = 3,
        requestType = 2,
        businessType = 2,
        scene = MessageScene.FRIEND
    ) {
        override fun buildOidb(client: LagrangeClient, payload: VideoUploadRequest): ByteArray {
            val uploadInfoList = buildVideoUploadInfoList(payload)
            val extBizInfo = buildVideoExtBizInfo()
            return buildBaseUploadReq(client, uploadInfoList, 2, extBizInfo)
        }

        override fun parseOidb(client: LagrangeClient, payload: ByteArray): UploadResp =
            payload.pbDecode<NTV2RichMediaResp>().upload
    }

    object GroupVideo : RichMediaUpload<VideoUploadRequest>(
        oidbCommand = 0x11ea,
        oidbService = 100,
        requestId = 3,
        requestType = 2,
        businessType = 2,
        scene = MessageScene.GROUP
    ) {
        override fun buildOidb(client: LagrangeClient, payload: VideoUploadRequest): ByteArray {
            val uploadInfoList = buildVideoUploadInfoList(payload)
            val extBizInfo = buildVideoExtBizInfo()
            return buildBaseUploadReq(client, uploadInfoList, 2, extBizInfo, payload.groupUin)
        }

        override fun parseOidb(client: LagrangeClient, payload: ByteArray): UploadResp =
            payload.pbDecode<NTV2RichMediaResp>().upload
    }
}
