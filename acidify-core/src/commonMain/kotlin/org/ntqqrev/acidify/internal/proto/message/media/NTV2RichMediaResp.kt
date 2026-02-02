package org.ntqqrev.acidify.internal.proto.message.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class NTV2RichMediaResp(
    @ProtoNumber(1) val respHead: MultiMediaRespHead = MultiMediaRespHead(),
    @ProtoNumber(2) val upload: UploadResp = UploadResp(),
    @ProtoNumber(3) val download: DownloadResp = DownloadResp(),
    @ProtoNumber(4) val downloadRKey: DownloadRKeyResp = DownloadRKeyResp(),
    @ProtoNumber(5) val delete: DeleteResp = DeleteResp(),
    @ProtoNumber(6) val uploadCompleted: UploadCompletedResp = UploadCompletedResp(),
    @ProtoNumber(7) val msgInfoAuth: MsgInfoAuthResp = MsgInfoAuthResp(),
    @ProtoNumber(8) val uploadKeyRenewal: UploadKeyRenewalResp = UploadKeyRenewalResp(),
    @ProtoNumber(9) val downloadSafe: DownloadSafeResp = DownloadSafeResp(),
    @ProtoNumber(99) val extension: ByteArray = byteArrayOf(),
)

@Serializable
internal class MultiMediaRespHead(
    @ProtoNumber(1) val common: CommonHead = CommonHead(),
    @ProtoNumber(2) val retCode: Int = 0,
    @ProtoNumber(3) val message: String = "",
)

@Serializable
internal class DownloadResp(
    @ProtoNumber(1) val rKeyParam: String = "",
    @ProtoNumber(2) val rKeyTtlSecond: Int = 0,
    @ProtoNumber(3) val info: DownloadInfo = DownloadInfo(),
    @ProtoNumber(4) val rKeyCreateTime: Int = 0,
)

@Serializable
internal class DownloadInfo(
    @ProtoNumber(1) val domain: String = "",
    @ProtoNumber(2) val urlPath: String = "",
    @ProtoNumber(3) val httpsPort: Int = 0,
    @ProtoNumber(4) val iPv4s: List<IPv4> = emptyList(),
    @ProtoNumber(5) val iPv6s: List<IPv6> = emptyList(),
    @ProtoNumber(6) val picUrlExtInfo: PicUrlExtInfo = PicUrlExtInfo(),
    @ProtoNumber(7) val videoExtInfo: VideoExtInfo = VideoExtInfo(),
)

@Serializable
internal class VideoExtInfo(
    @ProtoNumber(1) val videoCodecFormat: Int = 0,
)

@Serializable
internal class IPv4(
    @ProtoNumber(1) val outIP: Int = 0,
    @ProtoNumber(2) val outPort: Int = 0,
    @ProtoNumber(3) val inIP: Int = 0,
    @ProtoNumber(4) val inPort: Int = 0,
    @ProtoNumber(5) val iPType: Int = 0,
)

@Serializable
internal class IPv6(
    @ProtoNumber(1) val outIP: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val outPort: Int = 0,
    @ProtoNumber(3) val inIP: ByteArray = byteArrayOf(),
    @ProtoNumber(4) val inPort: Int = 0,
    @ProtoNumber(5) val iPType: Int = 0,
)

@Serializable
internal class UploadResp(
    @ProtoNumber(1) val uKey: String = "",
    @ProtoNumber(2) val uKeyTtlSecond: Int = 0,
    @ProtoNumber(3) val iPv4s: List<IPv4> = emptyList(),
    @ProtoNumber(4) val iPv6s: List<IPv6> = emptyList(),
    @ProtoNumber(5) val msgSeq: Long = 0L,
    @ProtoNumber(6) val msgInfo: MsgInfo = MsgInfo(),
    @ProtoNumber(7) val ext: List<RichMediaStorageTransInfo> = emptyList(),
    @ProtoNumber(8) val compatQMsg: ByteArray = byteArrayOf(),
    @ProtoNumber(10) val subFileInfos: List<SubFileInfo> = emptyList(),
)

@Serializable
internal class RichMediaStorageTransInfo(
    @ProtoNumber(1) val subType: Int = 0,
    @ProtoNumber(2) val extType: Int = 0,
    @ProtoNumber(3) val extValue: ByteArray = byteArrayOf(),
)

@Serializable
internal class SubFileInfo(
    @ProtoNumber(1) val subType: Int = 0,
    @ProtoNumber(2) val uKey: String = "",
    @ProtoNumber(3) val uKeyTtlSecond: Int = 0,
    @ProtoNumber(4) val iPv4s: List<IPv4> = emptyList(),
    @ProtoNumber(5) val iPv6s: List<IPv6> = emptyList(),
)

@Serializable
internal class DownloadRKeyResp(
    @ProtoNumber(1) val rKeys: List<RKeyInfo> = emptyList(),
)

@Serializable
internal class RKeyInfo(
    @ProtoNumber(1) val rkey: String = "",
    @ProtoNumber(2) val rkeyTtlSec: Long = 0L,
    @ProtoNumber(3) val storeId: Int = 0,
    @ProtoNumber(4) val rkeyCreateTime: Int = 0,
    @ProtoNumber(5) val type: Int = 0,
)

@Serializable
internal class DeleteResp

@Serializable
internal class UploadCompletedResp(
    @ProtoNumber(1) val msgSeq: Long = 0L,
)

@Serializable
internal class MsgInfoAuthResp(
    @ProtoNumber(1) val authCode: Int = 0,
    @ProtoNumber(2) val msg: ByteArray = byteArrayOf(),
    @ProtoNumber(3) val resultTime: Long = 0L,
)

@Serializable
internal class UploadKeyRenewalResp(
    @ProtoNumber(1) val ukey: String = "",
    @ProtoNumber(2) val ukeyTtlSec: Long = 0L,
)

@Serializable
internal class DownloadSafeResp
