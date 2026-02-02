package org.ntqqrev.acidify.internal.proto.message.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoPacked

@Serializable
internal class NTV2RichMediaReq(
    @ProtoNumber(1) val reqHead: MultiMediaReqHead = MultiMediaReqHead(),
    @ProtoNumber(2) val upload: UploadReq = UploadReq(),
    @ProtoNumber(3) val download: DownloadReq = DownloadReq(),
    @ProtoNumber(4) val downloadRKey: DownloadRKeyReq = DownloadRKeyReq(),
    @ProtoNumber(5) val delete: DeleteReq = DeleteReq(),
    @ProtoNumber(6) val uploadCompleted: UploadCompletedReq = UploadCompletedReq(),
    @ProtoNumber(7) val msgInfoAuth: MsgInfoAuthReq = MsgInfoAuthReq(),
    @ProtoNumber(8) val uploadKeyRenewal: UploadKeyRenewalReq = UploadKeyRenewalReq(),
    @ProtoNumber(9) val downloadSafe: DownloadSafeReq = DownloadSafeReq(),
    @ProtoNumber(99) val extension: ByteArray = byteArrayOf(),
)

@Serializable
internal class MultiMediaReqHead(
    @ProtoNumber(1) val common: CommonHead = CommonHead(),
    @ProtoNumber(2) val scene: SceneInfo = SceneInfo(),
    @ProtoNumber(3) val client: ClientMeta = ClientMeta(),
)

@Serializable
internal class CommonHead(
    @ProtoNumber(1) val requestId: Int = 0,
    @ProtoNumber(2) val command: Int = 0,
)

@Serializable
internal class SceneInfo(
    @ProtoNumber(101) val requestType: Int = 0,
    @ProtoNumber(102) val businessType: Int = 0,
    @ProtoNumber(200) val sceneType: Int = 0,
    @ProtoNumber(201) val c2C: C2CUserInfo = C2CUserInfo(),
    @ProtoNumber(202) val group: GroupInfo = GroupInfo(),
)

@Serializable
internal class C2CUserInfo(
    @ProtoNumber(1) val accountType: Int = 0,
    @ProtoNumber(2) val targetUid: String = "",
)

@Serializable
internal class GroupInfo(
    @ProtoNumber(1) val groupUin: Long = 0L,
)

@Serializable
internal class ClientMeta(
    @ProtoNumber(1) val agentType: Int = 0,
)

@Serializable
internal class DownloadReq(
    @ProtoNumber(1) val node: IndexNode = IndexNode(),
    @ProtoNumber(2) val download: DownloadExt = DownloadExt(),
)

@Serializable
internal class DownloadExt(
    @ProtoNumber(1) val pic: PicDownloadExt = PicDownloadExt(),
    @ProtoNumber(2) val video: VideoDownloadExt = VideoDownloadExt(),
    @ProtoNumber(3) val ptt: PttDownloadExt = PttDownloadExt(),
)

@Serializable
internal class PicDownloadExt

@Serializable
internal class VideoDownloadExt(
    @ProtoNumber(1) val busiType: Int = 0,
    @ProtoNumber(2) val sceneType: Int = 0,
    @ProtoNumber(3) val subBusiType: Int = 0,
)

@Serializable
internal class PttDownloadExt

@Serializable
internal class MsgInfoAuthReq(
    @ProtoNumber(1) val msg: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val authTime: Long = 0L,
)

@Serializable
internal class UploadCompletedReq(
    @ProtoNumber(1) val srvSendMsg: Boolean = false,
    @ProtoNumber(2) val clientRandomId: Long = 0L,
    @ProtoNumber(3) val msgInfo: MsgInfo = MsgInfo(),
    @ProtoNumber(4) val clientSeq: Int = 0,
)

@Serializable
internal class DeleteReq(
    @ProtoNumber(1) val index: List<IndexNode> = emptyList(),
    @ProtoNumber(2) val needRecallMsg: Boolean = false,
    @ProtoNumber(3) val msgSeq: Long = 0L,
    @ProtoNumber(4) val msgRandom: Long = 0L,
    @ProtoNumber(5) val msgTime: Long = 0L,
)

@Serializable
internal class DownloadRKeyReq(
    @ProtoNumber(1) @ProtoPacked val types: List<Int> = emptyList(),
)

@Serializable
internal class UploadInfo(
    @ProtoNumber(1) val fileInfo: FileInfo = FileInfo(),
    @ProtoNumber(2) val subFileType: Int = 0,
)

@Serializable
internal class UploadReq(
    @ProtoNumber(1) val uploadInfo: List<UploadInfo> = emptyList(),
    @ProtoNumber(2) val tryFastUploadCompleted: Boolean = false,
    @ProtoNumber(3) val srvSendMsg: Boolean = false,
    @ProtoNumber(4) val clientRandomId: Long = 0L,
    @ProtoNumber(5) val compatQMsgSceneType: Int = 0,
    @ProtoNumber(6) val extBizInfo: ExtBizInfo = ExtBizInfo(),
    @ProtoNumber(7) val clientSeq: Int = 0,
    @ProtoNumber(8) val noNeedCompatMsg: Boolean = false,
)

@Serializable
internal class UploadKeyRenewalReq(
    @ProtoNumber(1) val oldUKey: String = "",
    @ProtoNumber(2) val subType: Int = 0,
)

@Serializable
internal class DownloadSafeReq(
    @ProtoNumber(1) val index: IndexNode = IndexNode(),
)
