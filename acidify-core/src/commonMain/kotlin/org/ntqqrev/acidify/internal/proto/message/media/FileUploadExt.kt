package org.ntqqrev.acidify.internal.proto.message.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FileUploadExt(
    @ProtoNumber(1) val unknown1: Int = 0,
    @ProtoNumber(2) val unknown2: Int = 0,
    @ProtoNumber(3) val unknown3: Int = 0,
    @ProtoNumber(100) val entry: FileUploadEntry = FileUploadEntry(),
    @ProtoNumber(200) val unknown200: Int = 0,
)

@Serializable
internal class FileUploadEntry(
    @ProtoNumber(100) val busiBuff: ExcitingBusiInfo = ExcitingBusiInfo(),
    @ProtoNumber(200) val fileEntry: ExcitingFileEntry = ExcitingFileEntry(),
    @ProtoNumber(300) val clientInfo: ExcitingClientInfo = ExcitingClientInfo(),
    @ProtoNumber(400) val fileNameInfo: ExcitingFileNameInfo = ExcitingFileNameInfo(),
    @ProtoNumber(500) val host: ExcitingHostConfig = ExcitingHostConfig(),
)

@Serializable
internal class ExcitingBusiInfo(
    @ProtoNumber(1) val busId: Int = 0,
    @ProtoNumber(100) val senderUin: Long = 0L,
    @ProtoNumber(200) val receiverUin: Long = 0L,
    @ProtoNumber(400) val groupCode: Long = 0L,
)

@Serializable
internal class ExcitingFileEntry(
    @ProtoNumber(100) val fileSize: Long = 0L,
    @ProtoNumber(200) val md5: ByteArray = byteArrayOf(),
    @ProtoNumber(300) val checkKey: ByteArray = byteArrayOf(),
    @ProtoNumber(400) val md510M: ByteArray = byteArrayOf(),
    @ProtoNumber(500) val sha3: ByteArray = byteArrayOf(),
    @ProtoNumber(600) val fileId: String = "",
    @ProtoNumber(700) val uploadKey: ByteArray = byteArrayOf(),
)

@Serializable
internal class ExcitingClientInfo(
    @ProtoNumber(100) val clientType: Int = 0,
    @ProtoNumber(200) val appId: String = "",
    @ProtoNumber(300) val terminalType: Int = 0,
    @ProtoNumber(400) val clientVer: String = "",
    @ProtoNumber(600) val unknown: Int = 0,
)

@Serializable
internal class ExcitingFileNameInfo(
    @ProtoNumber(100) val fileName: String = "",
)

@Serializable
internal class ExcitingHostConfig(
    @ProtoNumber(200) val hosts: List<ExcitingHostInfo> = emptyList(),
)

@Serializable
internal class ExcitingHostInfo(
    @ProtoNumber(1) val url: ExcitingUrlInfo = ExcitingUrlInfo(),
    @ProtoNumber(2) val port: Int = 0,
)

@Serializable
internal class ExcitingUrlInfo(
    @ProtoNumber(1) val unknown: Int = 0,
    @ProtoNumber(2) val host: String = "",
)
