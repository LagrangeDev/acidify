package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class Oidb0x6D6Req(
    @ProtoNumber(1) val uploadFile: UploadFile = UploadFile(),
    @ProtoNumber(3) val downloadFile: DownloadFile = DownloadFile(),
    @ProtoNumber(4) val deleteFile: DeleteFile = DeleteFile(),
    @ProtoNumber(5) val renameFile: RenameFile = RenameFile(),
    @ProtoNumber(6) val moveFile: MoveFile = MoveFile(),
) {
    @Serializable
    internal class UploadFile(
        @ProtoNumber(1) val groupUin: Long = 0L,
        @ProtoNumber(2) val appId: Int = 0,
        @ProtoNumber(3) val busId: Int = 0,
        @ProtoNumber(4) val entrance: Int = 0,
        @ProtoNumber(5) val parentFolderId: String = "",
        @ProtoNumber(6) val fileName: String = "",
        @ProtoNumber(7) val localPath: String = "",
        @ProtoNumber(8) val fileSize: Long = 0L,
        @ProtoNumber(9) val sha: ByteArray = byteArrayOf(),
        @ProtoNumber(10) val sha3: ByteArray = byteArrayOf(),
        @ProtoNumber(11) val md5: ByteArray = byteArrayOf(),
        @ProtoNumber(12) val supportMultiUpload: Boolean = false,
    )

    @Serializable
    internal class DownloadFile(
        @ProtoNumber(1) val groupUin: Long = 0L,
        @ProtoNumber(2) val appId: Int = 0,
        @ProtoNumber(3) val busId: Int = 0,
        @ProtoNumber(4) val fileId: String = "",
    )

    @Serializable
    internal class DeleteFile(
        @ProtoNumber(1) val groupUin: Long = 0L,
        @ProtoNumber(2) val appId: Int = 0,
        @ProtoNumber(3) val busId: Int = 0,
        @ProtoNumber(4) val parentFolderId: String = "",
        @ProtoNumber(5) val fileId: String = "",
    )

    @Serializable
    internal class RenameFile(
        @ProtoNumber(1) val groupUin: Long = 0L,
        @ProtoNumber(2) val appId: Int = 0,
        @ProtoNumber(3) val busId: Int = 0,
        @ProtoNumber(4) val fileId: String = "",
        @ProtoNumber(5) val parentFolderId: String = "",
        @ProtoNumber(6) val newFileName: String = "",
    )

    @Serializable
    internal class MoveFile(
        @ProtoNumber(1) val groupUin: Long = 0L,
        @ProtoNumber(2) val appId: Int = 0,
        @ProtoNumber(3) val busId: Int = 0,
        @ProtoNumber(4) val fileId: String = "",
        @ProtoNumber(5) val parentFolderId: String = "",
        @ProtoNumber(6) val destFolderId: String = "",
    )
}

@Serializable
internal class Oidb0x6D6Resp(
    @ProtoNumber(1) val uploadFile: UploadFile = UploadFile(),
    @ProtoNumber(3) val downloadFile: DownloadFile = DownloadFile(),
    @ProtoNumber(4) val deleteFile: SimpleResp = SimpleResp(),
    @ProtoNumber(5) val renameFile: SimpleResp = SimpleResp(),
    @ProtoNumber(6) val moveFile: MoveFileResp = MoveFileResp(),
) {
    @Serializable
    internal class UploadFile(
        @ProtoNumber(1) val retCode: Int = 0,
        @ProtoNumber(2) val retMsg: String = "",
        @ProtoNumber(3) val clientWording: String = "",
        @ProtoNumber(4) val uploadIp: String = "",
        @ProtoNumber(5) val serverDns: String = "",
        @ProtoNumber(6) val busId: Int = 0,
        @ProtoNumber(7) val fileId: String = "",
        @ProtoNumber(8) val fileKey: ByteArray = byteArrayOf(),
        @ProtoNumber(9) val checkKey: ByteArray = byteArrayOf(),
        @ProtoNumber(10) val fileExist: Boolean = false,
        @ProtoNumber(12) val uploadIpLanV4: List<String> = emptyList(),
        @ProtoNumber(13) val uploadIpLanV6: List<String> = emptyList(),
        @ProtoNumber(14) val uploadPort: Int = 0,
    )

    @Serializable
    internal class DownloadFile(
        @ProtoNumber(5) val downloadDns: String = "",
        @ProtoNumber(6) val downloadUrl: ByteArray = byteArrayOf(),
    )

    @Serializable
    internal class SimpleResp(
        @ProtoNumber(1) val retCode: Int = 0,
        @ProtoNumber(2) val retMsg: String = "",
        @ProtoNumber(3) val clientWording: String = "",
    )

    @Serializable
    internal class MoveFileResp(
        @ProtoNumber(1) val retCode: Int = 0,
        @ProtoNumber(2) val retMsg: String = "",
        @ProtoNumber(3) val clientWording: String = "",
        @ProtoNumber(4) val parentFolderId: String = "",
    )
}
