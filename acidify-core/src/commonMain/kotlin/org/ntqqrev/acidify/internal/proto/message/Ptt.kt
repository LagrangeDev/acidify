package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class Ptt(
    @ProtoNumber(1) val fileType: Int = 0,
    @ProtoNumber(2) val srcUin: Long = 0L,
    @ProtoNumber(3) val fileUuid: ByteArray = byteArrayOf(),
    @ProtoNumber(4) val fileMd5: ByteArray = byteArrayOf(),
    @ProtoNumber(5) val fileName: String = "",
    @ProtoNumber(6) val fileSize: Int = 0,
    @ProtoNumber(7) val reserve: ByteArray = byteArrayOf(),
    @ProtoNumber(8) val fileId: Int = 0,
    @ProtoNumber(9) val serverIp: Int = 0,
    @ProtoNumber(10) val serverPort: Int = 0,
    @ProtoNumber(11) val valid: Boolean = false,
    @ProtoNumber(12) val signature: ByteArray = byteArrayOf(),
    @ProtoNumber(13) val shortcut: ByteArray = byteArrayOf(),
    @ProtoNumber(14) val fileKey: String = "",
    @ProtoNumber(15) val magicPttIndex: Int = 0,
    @ProtoNumber(16) val voiceSwitch: Int = 0,
    @ProtoNumber(17) val pttUrl: ByteArray = byteArrayOf(),
    @ProtoNumber(18) val groupFileKey: ByteArray = byteArrayOf(),
    @ProtoNumber(19) val time: Int = 0,
    @ProtoNumber(20) val downPara: ByteArray = byteArrayOf(),
    @ProtoNumber(29) val format: Int = 0,
    @ProtoNumber(30) val pbReserve: ByteArray = byteArrayOf(),
    @ProtoNumber(31) val pttUrls: List<ByteArray> = emptyList(),
    @ProtoNumber(32) val downloadFlag: Int = 0,
)
