package org.ntqqrev.acidify.struct

import kotlin.js.JsExport

/**
 * 群文件夹条目
 *
 * @property folderId 文件夹 ID
 * @property parentFolderId 父文件夹 ID
 * @property folderName 文件夹名称
 * @property createTime 创建时间（Unix 时间戳，秒）
 * @property modifiedTime 最后修改时间（Unix 时间戳，秒）
 * @property creatorUin 创建者 QQ 号
 * @property totalFileCount 文件总数
 */
@JsExport
data class BotGroupFolderEntry(
    val folderId: String,
    val parentFolderId: String,
    val folderName: String,
    val createTime: Long,
    val modifiedTime: Long,
    val creatorUin: Long,
    val totalFileCount: Int
)