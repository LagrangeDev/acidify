package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class Oidb0x6D7Req(
    @ProtoNumber(1) val createFolder: CreateFolder = CreateFolder(),
    @ProtoNumber(2) val deleteFolder: DeleteFolder = DeleteFolder(),
    @ProtoNumber(3) val renameFolder: RenameFolder = RenameFolder(),
) {
    @Serializable
    internal class CreateFolder(
        @ProtoNumber(1) val groupUin: Long = 0L,
        @ProtoNumber(3) val rootDirectory: String = "",
        @ProtoNumber(4) val folderName: String = "",
    )

    @Serializable
    internal class DeleteFolder(
        @ProtoNumber(1) val groupUin: Long = 0L,
        @ProtoNumber(3) val folderId: String = "",
    )

    @Serializable
    internal class RenameFolder(
        @ProtoNumber(1) val groupUin: Long = 0L,
        @ProtoNumber(3) val folderId: String = "",
        @ProtoNumber(4) val newFolderName: String = "",
    )
}

@Serializable
internal class Oidb0x6D7Resp(
    @ProtoNumber(1) val createFolder: CreateFolderResp = CreateFolderResp(),
    @ProtoNumber(2) val deleteFolder: SimpleResp = SimpleResp(),
    @ProtoNumber(3) val renameFolder: SimpleResp = SimpleResp(),
) {
    @Serializable
    internal class CreateFolderResp(
        @ProtoNumber(1) val retCode: Int = 0,
        @ProtoNumber(2) val retMsg: String = "",
        @ProtoNumber(3) val folderId: String = "",
    )

    @Serializable
    internal class SimpleResp(
        @ProtoNumber(1) val retCode: Int = 0,
        @ProtoNumber(2) val retMsg: String = "",
    )
}
