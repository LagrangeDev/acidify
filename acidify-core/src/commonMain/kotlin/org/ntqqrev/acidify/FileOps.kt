package org.ntqqrev.acidify

import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.acidify.common.MediaSource.Companion.toMediaSource
import org.ntqqrev.acidify.internal.service.file.*
import org.ntqqrev.acidify.internal.util.MediaSourceMetadata
import org.ntqqrev.acidify.struct.BotGroupFileEntry
import org.ntqqrev.acidify.struct.BotGroupFileSystemList
import org.ntqqrev.acidify.struct.BotGroupFolderEntry


/**
 * 上传群文件
 * @param groupUin 群号
 * @param fileName 文件名
 * @param fileSource 文件数据源
 * @param parentFolderId 父文件夹 ID，默认为根目录 `/`
 * @return 文件 ID
 */
suspend fun AbstractBot.uploadGroupFile(
    groupUin: Long,
    fileName: String,
    fileSource: MediaSource,
    parentFolderId: String = "/"
): String {
    val metadata = MediaSourceMetadata.from(fileSource)
    val uploadResp = client.callService(
        UploadGroupFile, UploadGroupFile.Req(
            groupUin = groupUin,
            fileName = fileName,
            fileSize = metadata.size,
            fileMd5 = metadata.md5,
            fileSha1 = metadata.sha1,
            fileTriSha1 = metadata.triSha1,
            parentFolderId = parentFolderId
        )
    )

    if (!uploadResp.fileExist) {
        client.highwayContext.uploadGroupFile(
            senderUin = uin,
            groupUin = groupUin,
            fileName = fileName,
            fileSource = fileSource,
            fileSize = metadata.size,
            fileMd5 = metadata.md5,
            md510M = metadata.md510M,
            fileId = uploadResp.fileId,
            fileKey = uploadResp.fileKey,
            checkKey = uploadResp.checkKey,
            uploadIp = uploadResp.uploadIp,
            uploadPort = uploadResp.uploadPort
        )
    }

    client.callService(
        BroadcastGroupFile, BroadcastGroupFile.Req(
            groupUin = groupUin, fileId = uploadResp.fileId
        )
    )

    return uploadResp.fileId
}

/**
 * 上传群文件
 * @param groupUin 群号
 * @param fileName 文件名
 * @param fileData 文件原始字节数据
 * @param parentFolderId 父文件夹 ID，默认为根目录 `/`
 * @return 文件 ID
 */
suspend fun AbstractBot.uploadGroupFile(
    groupUin: Long,
    fileName: String,
    fileData: ByteArray,
    parentFolderId: String = "/"
): String = uploadGroupFile(
    groupUin = groupUin,
    fileName = fileName,
    fileSource = fileData.toMediaSource(),
    parentFolderId = parentFolderId,
)

/**
 * 上传私聊文件
 * @param friendUin 好友 QQ 号
 * @param fileName 文件名
 * @param fileSource 文件数据源
 * @return 文件 ID
 */
suspend fun AbstractBot.uploadPrivateFile(
    friendUin: Long,
    fileName: String,
    fileSource: MediaSource
): String {
    val friendUid = getUidByUin(friendUin)
    val metadata = MediaSourceMetadata.from(fileSource)
    val md510M = metadata.md510M

    val uploadResp = client.callService(
        UploadPrivateFile,
        UploadPrivateFile.Req(
            senderUid = uid,
            receiverUid = friendUid,
            fileName = fileName,
            fileSize = metadata.size,
            fileMd5 = metadata.md5,
            fileSha1 = metadata.sha1,
            md510M = md510M,
            fileTriSha1 = metadata.triSha1
        )
    )

    if (!uploadResp.fileExist) {
        client.highwayContext.uploadPrivateFile(
            receiverUin = friendUin,
            fileName = fileName,
            fileSource = fileSource,
            fileSize = metadata.size,
            fileMd5 = metadata.md5,
            fileSha1 = metadata.sha1,
            md510M = md510M,
            fileTriSha1 = metadata.triSha1,
            fileId = uploadResp.fileId,
            uploadKey = uploadResp.uploadKey,
            uploadIpAndPorts = uploadResp.ipAndPorts
        )
    }

    client.callService(
        BroadcastPrivateFile,
        BroadcastPrivateFile.Req(
            friendUin = friendUin,
            friendUid = friendUid,
            fileId = uploadResp.fileId,
            fileMd510M = md510M,
            fileName = fileName,
            fileSize = metadata.size,
            crcMedia = uploadResp.fileCrcMedia
        )
    )

    return uploadResp.fileId
}

/**
 * 上传私聊文件
 * @param friendUin 好友 QQ 号
 * @param fileName 文件名
 * @param fileData 文件完整字节数据
 * @return 文件 ID
 */
suspend fun AbstractBot.uploadPrivateFile(
    friendUin: Long,
    fileName: String,
    fileData: ByteArray
): String = uploadPrivateFile(
    friendUin = friendUin,
    fileName = fileName,
    fileSource = fileData.toMediaSource(),
)

/**
 * 获取私聊文件下载链接
 * @param friendUin 好友 QQ 号
 * @param fileId 文件 ID
 * @param fileHash 文件的 TriSHA1 哈希值
 * @return 文件下载链接
 */
suspend fun AbstractBot.getPrivateFileDownloadUrl(
    friendUin: Long,
    fileId: String,
    fileHash: String
): String = client.callService(
    GetPrivateFileDownloadUrl,
    GetPrivateFileDownloadUrl.Req(
        receiverUid = getUidByUin(friendUin),
        fileUuid = fileId,
        fileHash = fileHash
    )
)

/**
 * 获取群文件下载链接
 * @param groupUin 群号
 * @param fileId 文件 ID
 * @return 文件下载链接
 */
suspend fun AbstractBot.getGroupFileDownloadUrl(
    groupUin: Long,
    fileId: String
): String = client.callService(
    GetGroupFileDownloadUrl,
    GetGroupFileDownloadUrl.Req(groupUin, fileId)
)

/**
 * 获取群文件/文件夹列表
 * @param groupUin 群号
 * @param targetDirectory 目标目录路径，默认为根目录 "/"
 * @param startIndex 起始索引，用于分页，默认为 0
 * @return 文件系统列表，包含文件列表、文件夹列表和是否到达末尾的标志
 */
suspend fun AbstractBot.getGroupFileList(
    groupUin: Long,
    targetDirectory: String = "/",
    startIndex: Int = 0
): BotGroupFileSystemList {
    var isEnd = false
    var currentIndex = startIndex
    val batchSize = 100
    val allFiles = mutableListOf<BotGroupFileEntry>()
    val allFolders = mutableListOf<BotGroupFolderEntry>()
    while (!isEnd) {
        val resp = client.callService(
            GetGroupFileList,
            GetGroupFileList.Req(
                groupUin = groupUin,
                targetDirectory = targetDirectory,
                startIndex = currentIndex,
                batchSize = batchSize
            )
        )
        allFiles.addAll(resp.files)
        allFolders.addAll(resp.folders)
        isEnd = resp.isEnd
        currentIndex += batchSize
    }
    return BotGroupFileSystemList(
        files = allFiles,
        folders = allFolders
    )
}

/**
 * 重命名群文件
 * @param groupUin 群号
 * @param fileId 文件 ID
 * @param parentFolderId 父文件夹 ID
 * @param newFileName 新文件名
 */
suspend fun AbstractBot.renameGroupFile(
    groupUin: Long,
    fileId: String,
    parentFolderId: String,
    newFileName: String
) = client.callService(
    RenameGroupFile,
    RenameGroupFile.Req(
        groupUin = groupUin,
        fileId = fileId,
        parentFolderId = parentFolderId,
        newFileName = newFileName
    )
)

/**
 * 移动群文件
 * @param groupUin 群号
 * @param fileId 文件 ID
 * @param parentFolderId 父文件夹 ID
 * @param targetFolderId 目标文件夹 ID
 */
suspend fun AbstractBot.moveGroupFile(
    groupUin: Long,
    fileId: String,
    parentFolderId: String,
    targetFolderId: String
) = client.callService(
    MoveGroupFile,
    MoveGroupFile.Req(
        groupUin = groupUin,
        fileId = fileId,
        parentFolderId = parentFolderId,
        targetFolderId = targetFolderId
    )
)

/**
 * 删除群文件
 * @param groupUin 群号
 * @param fileId 文件 ID
 */
suspend fun AbstractBot.deleteGroupFile(
    groupUin: Long,
    fileId: String
) = client.callService(
    DeleteGroupFile,
    DeleteGroupFile.Req(groupUin, fileId)
)

/**
 * 创建群文件夹
 * @param groupUin 群号
 * @param folderName 文件夹名称
 * @return 文件夹 ID
 */
suspend fun AbstractBot.createGroupFolder(
    groupUin: Long,
    folderName: String
): String = client.callService(
    CreateGroupFolder,
    CreateGroupFolder.Req(groupUin, folderName)
).folderId

/**
 * 重命名群文件夹
 * @param groupUin 群号
 * @param folderId 文件夹 ID
 * @param newFolderName 新文件夹名称
 */
suspend fun AbstractBot.renameGroupFolder(
    groupUin: Long,
    folderId: String,
    newFolderName: String
) = client.callService(
    RenameGroupFolder,
    RenameGroupFolder.Req(groupUin, folderId, newFolderName)
)

/**
 * 删除群文件夹
 * @param groupUin 群号
 * @param folderId 文件夹 ID
 */
suspend fun AbstractBot.deleteGroupFolder(
    groupUin: Long,
    folderId: String
) = client.callService(
    DeleteGroupFolder,
    DeleteGroupFolder.Req(groupUin, folderId)
)
