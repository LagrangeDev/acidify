package org.ntqqrev.acidify.internal.proto.message.elem

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class NotOnlineImage(
    @ProtoNumber(1) val filePath: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val fileLen: Int = 0,
    @ProtoNumber(3) val downloadPath: ByteArray = byteArrayOf(),
    @ProtoNumber(4) val oldVerSendFile: ByteArray = byteArrayOf(),
    @ProtoNumber(5) val imgType: Int = 0,
    @ProtoNumber(6) val previewsImage: ByteArray = byteArrayOf(),
    @ProtoNumber(7) val picMd5: ByteArray = byteArrayOf(),
    @ProtoNumber(8) val picHeight: Int = 0,
    @ProtoNumber(9) val picWidth: Int = 0,
    @ProtoNumber(10) val resId: ByteArray = byteArrayOf(),
    @ProtoNumber(11) val flag: ByteArray = byteArrayOf(),
    @ProtoNumber(12) val thumbUrl: String = "",
    @ProtoNumber(13) val original: Int = 0,
    @ProtoNumber(14) val bigUrl: String = "",
    @ProtoNumber(15) val origUrl: String = "",
    @ProtoNumber(16) val bizType: Int = 0,
    @ProtoNumber(17) val result: Int = 0,
    @ProtoNumber(18) val index: Int = 0,
    @ProtoNumber(19) val opFaceBuf: ByteArray = byteArrayOf(),
    @ProtoNumber(20) val oldPicMd5: Boolean = false,
    @ProtoNumber(21) val thumbWidth: Int = 0,
    @ProtoNumber(22) val thumbHeight: Int = 0,
    @ProtoNumber(23) val fileId: Int = 0,
    @ProtoNumber(24) val showLen: Int = 0,
    @ProtoNumber(25) val downloadLen: Int = 0,
    @ProtoNumber(26) val url400: String = "",
    @ProtoNumber(27) val width400: Int = 0,
    @ProtoNumber(28) val height400: Int = 0,
    @ProtoNumber(29) val pbReserve: ByteArray = byteArrayOf(),
)
