package org.ntqqrev.acidify.internal.proto.message.elem

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class VideoFile(
    @ProtoNumber(1) val fileUuid: String = "",
    @ProtoNumber(2) val fileMd5: ByteArray = byteArrayOf(),
    @ProtoNumber(3) val fileName: String = "",
    @ProtoNumber(4) val fileFormat: Int = 0,
    @ProtoNumber(5) val fileTime: Int = 0,
    @ProtoNumber(6) val fileSize: Int = 0,
    @ProtoNumber(7) val thumbWidth: Int = 0,
    @ProtoNumber(8) val thumbHeight: Int = 0,
    @ProtoNumber(9) val thumbFileMd5: ByteArray = byteArrayOf(),
    @ProtoNumber(10) val source: ByteArray = byteArrayOf(),
    @ProtoNumber(11) val thumbFileSize: Int = 0,
    @ProtoNumber(12) val busiType: Int = 0,
    @ProtoNumber(13) val fromChatType: Int = 0,
    @ProtoNumber(14) val toChatType: Int = 0,
    @ProtoNumber(15) val boolSupportProgressive: Boolean = false,
    @ProtoNumber(16) val fileWidth: Int = 0,
    @ProtoNumber(17) val fileHeight: Int = 0,
    @ProtoNumber(18) val subBusiType: Int = 0,
    @ProtoNumber(19) val videoAttr: Int = 0,
    @ProtoNumber(20) val bytesThumbFileUrls: List<ByteArray> = emptyList(),
    @ProtoNumber(21) val bytesVideoFileUrls: List<ByteArray> = emptyList(),
    @ProtoNumber(22) val thumbDownloadFlag: Int = 0,
    @ProtoNumber(23) val videoDownloadFlag: Int = 0,
    @ProtoNumber(24) val pbReserve: ByteArray = byteArrayOf(),
)
