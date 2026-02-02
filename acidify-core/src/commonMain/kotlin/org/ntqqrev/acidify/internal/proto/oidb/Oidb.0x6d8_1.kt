package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GroupFileListReq(
    @ProtoNumber(2) val listReq: GroupFileListReqBody = GroupFileListReqBody(),
)

@Serializable
internal class GroupFileListReqBody(
    @ProtoNumber(1) val groupUin: Long = 0L,
    @ProtoNumber(2) val appId: Int = 0,
    @ProtoNumber(3) val targetDirectory: String = "",
    @ProtoNumber(5) val fileCount: Int = 0,
    @ProtoNumber(9) val sortBy: Int = 0,
    @ProtoNumber(13) val startIndex: Int = 0,
    @ProtoNumber(17) val field17: Int = 0,
    @ProtoNumber(18) val field18: Int = 0,
)

@Serializable
internal class GroupFileListResp(
    @ProtoNumber(2) val listResp: GroupFileListRespBody = GroupFileListRespBody(),
)

@Serializable
internal class GroupFileListRespBody(
    @ProtoNumber(1) val retCode: Int = 0,
    @ProtoNumber(4) val isEnd: Boolean = false,
    @ProtoNumber(5) val items: List<GroupFileListItem> = emptyList(),
)

@Serializable
internal class GroupFileListItem(
    @ProtoNumber(1) val type: Int = 0,
    @ProtoNumber(2) val folderInfo: GroupFolderInfo = GroupFolderInfo(),
    @ProtoNumber(3) val fileInfo: GroupFileInfo = GroupFileInfo(),
)

@Serializable
internal class GroupFileInfo(
    @ProtoNumber(1) val fileId: String = "",
    @ProtoNumber(2) val fileName: String = "",
    @ProtoNumber(3) val fileSize: Long = 0L,
    @ProtoNumber(4) val busId: Int = 0,
    @ProtoNumber(5) val uploadedSize: Long = 0L,
    @ProtoNumber(6) val uploadedTime: Long = 0L,
    @ProtoNumber(7) val expireTime: Long = 0L,
    @ProtoNumber(8) val modifiedTime: Long = 0L,
    @ProtoNumber(9) val downloadedTimes: Int = 0,
    @ProtoNumber(10) val fileSha1: ByteArray = byteArrayOf(),
    @ProtoNumber(12) val fileMd5: ByteArray = byteArrayOf(),
    @ProtoNumber(14) val uploaderName: String = "",
    @ProtoNumber(15) val uploaderUin: Long = 0L,
    @ProtoNumber(16) val parentDirectory: String = "",
    @ProtoNumber(17) val field17: Int = 0,
    @ProtoNumber(22) val field22: String = "",
)

@Serializable
internal class GroupFolderInfo(
    @ProtoNumber(1) val folderId: String = "",
    @ProtoNumber(2) val parentDirectoryId: String = "",
    @ProtoNumber(3) val folderName: String = "",
    @ProtoNumber(4) val createTime: Long = 0L,
    @ProtoNumber(5) val modifiedTime: Long = 0L,
    @ProtoNumber(6) val creatorUin: Long = 0L,
    @ProtoNumber(7) val creatorName: String = "",
    @ProtoNumber(8) val totalFileCount: Int = 0,
)
