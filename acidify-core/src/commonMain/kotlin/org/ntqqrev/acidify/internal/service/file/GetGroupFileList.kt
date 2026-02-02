package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.GroupFileListReq
import org.ntqqrev.acidify.internal.proto.oidb.GroupFileListReqBody
import org.ntqqrev.acidify.internal.proto.oidb.GroupFileListResp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.struct.BotGroupFileEntry
import org.ntqqrev.acidify.struct.BotGroupFolderEntry

internal object GetGroupFileList : OidbService<GetGroupFileList.Req, GetGroupFileList.Resp>(0x6d8, 1, true) {
    class Req(
        val groupUin: Long,
        val targetDirectory: String,
        val startIndex: Int,
        val batchSize: Int
    )

    class Resp(
        val files: List<BotGroupFileEntry>,
        val folders: List<BotGroupFolderEntry>,
        val isEnd: Boolean
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        GroupFileListReq(
            listReq = GroupFileListReqBody(
                groupUin = payload.groupUin,
                appId = 7,
                targetDirectory = payload.targetDirectory,
                fileCount = payload.batchSize,
                sortBy = 1,
                startIndex = payload.startIndex,
                field17 = 2,
                field18 = 0,
            )
        ).pbEncode()

    override fun parseOidb(client: LagrangeClient, payload: ByteArray): Resp {
        val resp = payload.pbDecode<GroupFileListResp>().listResp
        val retCode = resp.retCode

        if (retCode != 0) {
            throw Exception(retCode.toString())
        }

        val items = resp.items
        val files = mutableListOf<BotGroupFileEntry>()
        val folders = mutableListOf<BotGroupFolderEntry>()

        items.forEach { item ->
            when (item.type) {
                1 -> {
                    val fileInfo = item.fileInfo
                    files.add(
                        BotGroupFileEntry(
                            fileId = fileInfo.fileId,
                            fileName = fileInfo.fileName,
                            parentFolderId = fileInfo.parentDirectory,
                            fileSize = fileInfo.fileSize,
                            expireTime = fileInfo.expireTime,
                            modifiedTime = fileInfo.modifiedTime,
                            uploaderUin = fileInfo.uploaderUin,
                            uploadedTime = fileInfo.uploadedTime,
                            downloadedTimes = fileInfo.downloadedTimes
                        )
                    )
                }

                2 -> {
                    val folderInfo = item.folderInfo
                    folders.add(
                        BotGroupFolderEntry(
                            folderId = folderInfo.folderId,
                            parentFolderId = folderInfo.parentDirectoryId,
                            folderName = folderInfo.folderName,
                            createTime = folderInfo.createTime,
                            modifiedTime = folderInfo.modifiedTime,
                            creatorUin = folderInfo.creatorUin,
                            totalFileCount = folderInfo.totalFileCount
                        )
                    )
                }
            }
        }

        return Resp(
            files = files,
            folders = folders,
            isEnd = resp.isEnd
        )
    }
}
