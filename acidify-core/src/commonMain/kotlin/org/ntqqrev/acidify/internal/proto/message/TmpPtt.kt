package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class TmpPtt(
    @ProtoNumber(1) val fileType: Int = 0,
    @ProtoNumber(2) val fileUuid: ByteArray = byteArrayOf(),
    @ProtoNumber(3) val fileMd5: ByteArray = byteArrayOf(),
    @ProtoNumber(4) val fileName: ByteArray = byteArrayOf(),
    @ProtoNumber(5) val fileSize: Int = 0,
    @ProtoNumber(6) val pttTimes: Int = 0,
    @ProtoNumber(7) val userType: Int = 0,
    @ProtoNumber(8) val pttTransFlag: Int = 0,
    @ProtoNumber(9) val busiType: Int = 0,
    @ProtoNumber(10) val msgId: Long = 0L,
    @ProtoNumber(30) val pbReserve: ByteArray = byteArrayOf(),
    @ProtoNumber(31) val pttEncodeData: ByteArray = byteArrayOf(),
)
