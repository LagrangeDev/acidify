package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import org.ntqqrev.acidify.internal.proto.message.media.IPv4

@Serializable
internal class Oidb0xE37Req(
    @ProtoNumber(1) val subCommand: Int = 0,
    @ProtoNumber(2) val seq: Int = 0,
    @ProtoNumber(14) val downloadBody: DownloadBody = DownloadBody(),
    @ProtoNumber(19) val uploadBody: UploadBody = UploadBody(),
    @ProtoNumber(101) val field101: Int = 0,
    @ProtoNumber(102) val field102: Int = 0,
    @ProtoNumber(200) val field200: Int = 0,
    @ProtoNumber(99999) val field99999: ByteArray = byteArrayOf(),
) {
    @Serializable
    internal class DownloadBody(
        @ProtoNumber(10) val receiverUid: String = "",
        @ProtoNumber(20) val fileUuid: String = "",
        @ProtoNumber(30) val type: Int = 0,
        @ProtoNumber(60) val fileHash: String = "",
        @ProtoNumber(601) val t2: Int = 0,
    )

    @Serializable
    internal class UploadBody(
        @ProtoNumber(10) val senderUid: String = "",
        @ProtoNumber(20) val receiverUid: String = "",
        @ProtoNumber(30) val fileSize: Int = 0,
        @ProtoNumber(40) val fileName: String = "",
        @ProtoNumber(50) val md510MCheckSum: ByteArray = byteArrayOf(),
        @ProtoNumber(60) val sha1CheckSum: ByteArray = byteArrayOf(),
        @ProtoNumber(70) val localPath: String = "",
        @ProtoNumber(110) val md5CheckSum: ByteArray = byteArrayOf(),
        @ProtoNumber(120) val sha3CheckSum: ByteArray = byteArrayOf(),
    )
}

@Serializable
internal class Oidb0xE37Resp(
    @ProtoNumber(1) val command: Int = 0,
    @ProtoNumber(2) val subCommand: Int = 0,
    @ProtoNumber(14) val downloadBody: DownloadBody = DownloadBody(),
    @ProtoNumber(19) val uploadBody: UploadBody = UploadBody(),
    @ProtoNumber(50) val field50: Int = 0,
    @ProtoNumber(101) val field101: Int = 0,
    @ProtoNumber(102) val field102: Int = 0,
    @ProtoNumber(200) val field200: Int = 0,
    @ProtoNumber(99999) val field99999: ByteArray = byteArrayOf(),
) {
    @Serializable
    internal class DownloadBody(
        @ProtoNumber(10) val field10: Int = 0,
        @ProtoNumber(20) val state: String = "",
        @ProtoNumber(30) val result: DownloadResult = DownloadResult(),
    ) {
        @Serializable
        internal class DownloadResult(
            @ProtoNumber(20) val server: String = "",
            @ProtoNumber(40) val port: Int = 0,
            @ProtoNumber(50) val url: String = "",
        )
    }

    @Serializable
    internal class UploadBody(
        @ProtoNumber(10) val retCode: Int = 0,
        @ProtoNumber(20) val retMsg: String = "",
        @ProtoNumber(30) val totalSpace: Long = 0L,
        @ProtoNumber(40) val usedSpace: Long = 0L,
        @ProtoNumber(50) val uploadedSize: Long = 0L,
        @ProtoNumber(60) val uploadIp: String = "",
        @ProtoNumber(70) val uploadDomain: String = "",
        @ProtoNumber(80) val uploadPort: Int = 0,
        @ProtoNumber(90) val uuid: String = "",
        @ProtoNumber(100) val uploadKey: ByteArray = byteArrayOf(),
        @ProtoNumber(110) val boolFileExist: Boolean = false,
        @ProtoNumber(120) val packSize: Int = 0,
        @ProtoNumber(130) val uploadIpList: List<String> = emptyList(),
        @ProtoNumber(140) val uploadHttpsPort: Int = 0,
        @ProtoNumber(150) val uploadHttpsDomain: String = "",
        @ProtoNumber(160) val uploadDns: String = "",
        @ProtoNumber(170) val uploadLanip: String = "",
        @ProtoNumber(200) val fileIdCrc: String = "",
        @ProtoNumber(210) val rtpMediaPlatformUploadAddress: List<IPv4> = emptyList(),
        @ProtoNumber(220) val mediaPlatformUploadKey: ByteArray = byteArrayOf(),
    )
}
