package org.ntqqrev.acidify.struct

import kotlin.js.JsExport

/**
 * 群文件条目
 *
 * @property fileId 文件 ID
 * @property fileName 文件名
 * @property parentFolderId 父文件夹 ID
 * @property fileSize 文件大小（字节）
 * @property expireTime 过期时间（Unix 时间戳，秒）
 * @property modifiedTime 最后修改时间（Unix 时间戳，秒）
 * @property uploaderUin 上传者 QQ 号
 * @property uploadedTime 上传时间（Unix 时间戳，秒）
 * @property downloadedTimes 下载次数
 */
@JsExport
data class BotGroupFileEntry(
    val fileId: String,
    val fileName: String,
    val parentFolderId: String,
    val fileSize: Long,
    val expireTime: Long,
    val modifiedTime: Long,
    val uploaderUin: Long,
    val uploadedTime: Long,
    val downloadedTimes: Int
)