package org.ntqqrev.acidify.milky.api.handler

import org.ntqqrev.acidify.*
import org.ntqqrev.acidify.milky.api.MilkyApiException
import org.ntqqrev.acidify.milky.api.define
import org.ntqqrev.acidify.milky.transform.toMilkyEntity
import org.ntqqrev.milky.*

val UploadPrivateFile = ApiEndpoint.UploadPrivateFile.define {
    val fileId = bot.uploadPrivateFile(
        friendUin = it.userId,
        fileName = it.fileName,
        fileSource = resolveUri(it.fileUri)
    )
    UploadPrivateFileOutput(fileId)
}

val UploadGroupFile = ApiEndpoint.UploadGroupFile.define {
    bot.getGroup(it.groupId) ?: throw MilkyApiException(-404, "Group not found")
    val fileId = bot.uploadGroupFile(
        groupUin = it.groupId,
        fileName = it.fileName,
        fileSource = resolveUri(it.fileUri),
        parentFolderId = it.parentFolderId
    )
    UploadGroupFileOutput(fileId)
}

val GetPrivateFileDownloadUrl = ApiEndpoint.GetPrivateFileDownloadUrl.define {
    val url = bot.getPrivateFileDownloadUrl(it.userId, it.fileId, it.fileHash)
    GetPrivateFileDownloadUrlOutput(url)
}

val GetGroupFileDownloadUrl = ApiEndpoint.GetGroupFileDownloadUrl.define {
    val url = bot.getGroupFileDownloadUrl(it.groupId, it.fileId)
    GetGroupFileDownloadUrlOutput(url)
}

val GetGroupFiles = ApiEndpoint.GetGroupFiles.define {
    val result = bot.getGroupFileList(it.groupId, it.parentFolderId, 0)
    GetGroupFilesOutput(
        files = result.files.map { file -> file.toMilkyEntity(it.groupId) },
        folders = result.folders.map { folder -> folder.toMilkyEntity(it.groupId) }
    )
}

val MoveGroupFile = ApiEndpoint.MoveGroupFile.define {
    bot.moveGroupFile(it.groupId, it.fileId, it.parentFolderId, it.targetFolderId)
    MoveGroupFileOutput()
}

val RenameGroupFile = ApiEndpoint.RenameGroupFile.define {
    bot.renameGroupFile(it.groupId, it.fileId, it.parentFolderId, it.newFileName)
    RenameGroupFileOutput()
}

val DeleteGroupFile = ApiEndpoint.DeleteGroupFile.define {
    bot.deleteGroupFile(it.groupId, it.fileId)
    DeleteGroupFileOutput()
}

val CreateGroupFolder = ApiEndpoint.CreateGroupFolder.define {
    val folderId = bot.createGroupFolder(it.groupId, it.folderName)
    CreateGroupFolderOutput(folderId)
}

val RenameGroupFolder = ApiEndpoint.RenameGroupFolder.define {
    bot.renameGroupFolder(it.groupId, it.folderId, it.newFolderName)
    RenameGroupFolderOutput()
}

val DeleteGroupFolder = ApiEndpoint.DeleteGroupFolder.define {
    bot.deleteGroupFolder(it.groupId, it.folderId)
    DeleteGroupFolderOutput()
}
