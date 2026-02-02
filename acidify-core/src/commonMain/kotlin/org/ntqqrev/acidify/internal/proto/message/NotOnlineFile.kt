package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class NotOnlineFile(
    @ProtoNumber(1) val fileType: Int = 0,
    @ProtoNumber(2) val sig: ByteArray = byteArrayOf(),
    @ProtoNumber(3) val fileUuid: String = "",
    @ProtoNumber(4) val fileMd5: ByteArray = byteArrayOf(),
    @ProtoNumber(5) val fileName: String = "",
    @ProtoNumber(6) val fileSize: Long = 0L,
    @ProtoNumber(7) val note: ByteArray = byteArrayOf(),
    @ProtoNumber(8) val reserved: Int = 0,
    @ProtoNumber(9) val subCmd: Int = 0,
    @ProtoNumber(10) val microCloud: Int = 0,
    @ProtoNumber(11) val fileUrls: List<ByteArray> = emptyList(),
    @ProtoNumber(12) val downloadFlag: Int = 0,
    @ProtoNumber(50) val dangerLevel: Int = 0,
    @ProtoNumber(51) val lifeTime: Int = 0,
    @ProtoNumber(52) val uploadTime: Int = 0,
    @ProtoNumber(53) val absFileType: Int = 0,
    @ProtoNumber(54) val clientType: Int = 0,
    @ProtoNumber(55) val expireTime: Long = 0L,
    @ProtoNumber(56) val pbReserve: ByteArray = byteArrayOf(),
    @ProtoNumber(57) val fileIdCrcMedia: String = "",
)
