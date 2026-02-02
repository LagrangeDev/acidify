package org.ntqqrev.acidify.internal.proto.message.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class MsgInfo(
    @ProtoNumber(1) val msgInfoBody: List<MsgInfoBody> = emptyList(),
    @ProtoNumber(2) val extBizInfo: ExtBizInfo = ExtBizInfo(),
)

@Serializable
internal class MsgInfoBody(
    @ProtoNumber(1) val index: IndexNode = IndexNode(),
    @ProtoNumber(2) val picture: PictureInfo = PictureInfo(),
    @ProtoNumber(3) val video: VideoInfo = VideoInfo(),
    @ProtoNumber(4) val audio: AudioInfo = AudioInfo(),
    @ProtoNumber(5) val fileExist: Boolean = false,
    @ProtoNumber(6) val hashSum: HashSum = HashSum(),
)

@Serializable
internal class IndexNode(
    @ProtoNumber(1) val info: FileInfo = FileInfo(),
    @ProtoNumber(2) val fileUuid: String = "",
    @ProtoNumber(3) val storeId: Int = 0,
    @ProtoNumber(4) val uploadTime: Int = 0,
    @ProtoNumber(5) val ttl: Int = 0,
    @ProtoNumber(6) val subType: Int = 0,
)

@Serializable
internal class FileInfo(
    @ProtoNumber(1) val fileSize: Int = 0,
    @ProtoNumber(2) val fileHash: String = "",
    @ProtoNumber(3) val fileSha1: String = "",
    @ProtoNumber(4) val fileName: String = "",
    @ProtoNumber(5) val type: FileType = FileType(),
    @ProtoNumber(6) val width: Int = 0,
    @ProtoNumber(7) val height: Int = 0,
    @ProtoNumber(8) val time: Int = 0,
    @ProtoNumber(9) val original: Int = 0,
)

@Serializable
internal class FileType(
    @ProtoNumber(1) val type: Int = 0,
    @ProtoNumber(2) val picFormat: Int = 0,
    @ProtoNumber(3) val videoFormat: Int = 0,
    @ProtoNumber(4) val voiceFormat: Int = 0,
)

@Serializable
internal class PictureInfo(
    @ProtoNumber(1) val urlPath: String = "",
    @ProtoNumber(2) val ext: PicUrlExtInfo = PicUrlExtInfo(),
    @ProtoNumber(3) val domain: String = "",
)

@Serializable
internal class VideoInfo

@Serializable
internal class AudioInfo

@Serializable
internal class PicUrlExtInfo(
    @ProtoNumber(1) val originalParameter: String = "",
    @ProtoNumber(2) val bigParameter: String = "",
    @ProtoNumber(3) val thumbParameter: String = "",
)

@Serializable
internal class HashSum(
    @ProtoNumber(201) val c2c: C2cSource = C2cSource(),
    @ProtoNumber(202) val troop: TroopSource = TroopSource(),
)

@Serializable
internal class C2cSource(
    @ProtoNumber(2) val friendUid: String = "",
)

@Serializable
internal class TroopSource(
    @ProtoNumber(1) val groupUin: Int = 0,
)

@Serializable
internal class ExtBizInfo(
    @ProtoNumber(1) val pic: PicExtBizInfo = PicExtBizInfo(),
    @ProtoNumber(2) val video: VideoExtBizInfo = VideoExtBizInfo(),
    @ProtoNumber(3) val ptt: PttExtBizInfo = PttExtBizInfo(),
    @ProtoNumber(10) val busiType: Int = 0,
)

@Serializable
internal class PicExtBizInfo(
    @ProtoNumber(1) val bizType: Int = 0,
    @ProtoNumber(2) val textSummary: String = "",
    @ProtoNumber(11) val bytesPbReserveC2C: PbReserve = PbReserve(),
    @ProtoNumber(12) val bytesPbReserveTroop: PbReserve = PbReserve(),
) {
    @Serializable
    internal class PbReserve(
        @ProtoNumber(1) val subType: Int = 0,
    )
}

@Serializable
internal class VideoExtBizInfo(
    @ProtoNumber(1) val fromScene: Int = 0,
    @ProtoNumber(2) val toScene: Int = 0,
    @ProtoNumber(3) val bytesPbReserve: ByteArray = byteArrayOf(),
)

@Serializable
internal class PttExtBizInfo(
    @ProtoNumber(1) val srcUin: Long = 0L,
    @ProtoNumber(2) val pttScene: Int = 0,
    @ProtoNumber(3) val pttType: Int = 0,
    @ProtoNumber(4) val changeVoice: Int = 0,
    @ProtoNumber(5) val waveform: ByteArray = byteArrayOf(),
    @ProtoNumber(6) val autoConvertText: Int = 0,
    @ProtoNumber(11) val bytesReserve: ByteArray = byteArrayOf(),
    @ProtoNumber(12) val bytesPbReserve: ByteArray = byteArrayOf(),
    @ProtoNumber(13) val bytesGeneralFlags: ByteArray = byteArrayOf(),
)
