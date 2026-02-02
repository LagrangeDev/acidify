package org.ntqqrev.acidify.internal.proto.message.elem

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class CustomFace(
    @ProtoNumber(1) val guid: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val filePath: String = "",
    @ProtoNumber(3) val shortcut: String = "",
    @ProtoNumber(4) val buffer: ByteArray = byteArrayOf(),
    @ProtoNumber(5) val flag: ByteArray = byteArrayOf(),
    @ProtoNumber(6) val oldData: ByteArray = byteArrayOf(),
    @ProtoNumber(7) val fileId: Int = 0,
    @ProtoNumber(8) val serverIp: Int = 0,
    @ProtoNumber(9) val serverPort: Int = 0,
    @ProtoNumber(10) val fileType: Int = 0,
    @ProtoNumber(11) val signature: ByteArray = byteArrayOf(),
    @ProtoNumber(12) val useful: Int = 0,
    @ProtoNumber(13) val md5: ByteArray = byteArrayOf(),
    @ProtoNumber(14) val thumbUrl: String = "",
    @ProtoNumber(15) val bigUrl: String = "",
    @ProtoNumber(16) val origUrl: String = "",
    @ProtoNumber(17) val bizType: Int = 0,
    @ProtoNumber(18) val repeatIndex: Int = 0,
    @ProtoNumber(19) val repeatImage: Int = 0,
    @ProtoNumber(20) val imageType: Int = 0,
    @ProtoNumber(21) val index: Int = 0,
    @ProtoNumber(22) val width: Int = 0,
    @ProtoNumber(23) val height: Int = 0,
    @ProtoNumber(24) val source: Int = 0,
    @ProtoNumber(25) val size: Int = 0,
    @ProtoNumber(26) val origin: Int = 0,
    @ProtoNumber(27) val thumbWidth: Int = 0,
    @ProtoNumber(28) val thumbHeight: Int = 0,
    @ProtoNumber(29) val showLen: Int = 0,
    @ProtoNumber(30) val downloadLen: Int = 0,
    @ProtoNumber(31) val x400Url: String = "",
    @ProtoNumber(32) val x400Width: Int = 0,
    @ProtoNumber(33) val x400Height: Int = 0,
    @ProtoNumber(34) val pbReserve: PbReserve1 = PbReserve1(),
) {
    @Serializable
    internal class PbReserve1(
        @ProtoNumber(1) val subType: Int = 0,
        @ProtoNumber(3) val field3: Int = 0,
        @ProtoNumber(4) val field4: Int = 0,
        @ProtoNumber(9) val summary: String = "",
        @ProtoNumber(10) val field10: Int = 0,
        @ProtoNumber(21) val field21: PbReserve2 = PbReserve2(),
        @ProtoNumber(31) val field31: String = "",
    ) {
        @Serializable
        internal class PbReserve2(
            @ProtoNumber(1) val field1: Int = 0,
            @ProtoNumber(2) val field2: String = "",
            @ProtoNumber(3) val field3: Int = 0,
            @ProtoNumber(4) val field4: Int = 0,
            @ProtoNumber(5) val field5: Int = 0,
            @ProtoNumber(7) val md5Str: String = "",
        )
    }
}
