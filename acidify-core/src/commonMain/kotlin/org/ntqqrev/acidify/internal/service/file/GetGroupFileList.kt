package org.ntqqrev.acidify.internal.service.file

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.GroupFileListReq
import org.ntqqrev.acidify.internal.packet.oidb.GroupFileListReqBody
import org.ntqqrev.acidify.internal.packet.oidb.GroupFileListResp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService
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

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        GroupFileListReq {
            it[listReq] = GroupFileListReqBody {
                it[groupUin] = payload.groupUin
                it[appId] = 7
                it[targetDirectory] = payload.targetDirectory
                it[fileCount] = payload.batchSize
                it[sortBy] = 1
                it[startIndex] = payload.startIndex
                it[field17] = 2
                it[field18] = 0
            }
        }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray): Resp {
        val resp = GroupFileListResp(payload).get { listResp }
        val retCode = resp.get { retCode }

        if (retCode != 0) {
            throw Exception("获取群文件列表失败: $retCode")
        }

        val items = resp.get { items }
        val files = mutableListOf<BotGroupFileEntry>()
        val folders = mutableListOf<BotGroupFolderEntry>()

        items.forEach { item ->
            when (item.get { type }) {
                1 -> {
                    val fileInfo = item.get { this.fileInfo }
                    files.add(
                        BotGroupFileEntry(
                        fileId = fileInfo.get { this.fileId },
                        fileName = fileInfo.get { this.fileName },
                        parentFolderId = fileInfo.get { parentDirectory },
                        fileSize = fileInfo.get { fileSize },
                        expireTime = fileInfo.get { expireTime },
                        modifiedTime = fileInfo.get { modifiedTime },
                        uploaderUin = fileInfo.get { uploaderUin },
                        uploadedTime = fileInfo.get { uploadedTime },
                        downloadedTimes = fileInfo.get { downloadedTimes }
                    ))
                }

                2 -> {
                    val folderInfo = item.get { this.folderInfo }
                    folders.add(
                        BotGroupFolderEntry(
                        folderId = folderInfo.get { this.folderId },
                        parentFolderId = folderInfo.get { parentDirectoryId },
                        folderName = folderInfo.get { this.folderName },
                        createTime = folderInfo.get { this.createTime },
                        modifiedTime = folderInfo.get { this.modifiedTime },
                        creatorUin = folderInfo.get { creatorUin },
                        totalFileCount = folderInfo.get { totalFileCount }
                    ))
                }
            }
        }

        return Resp(
            files = files,
            folders = folders,
            isEnd = resp.get { isEnd }
        )
    }
}