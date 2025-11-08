package org.ntqqrev.acidify.struct

import kotlin.js.JsExport

/**
 * 群文件系统列表结果
 *
 * @property files 文件列表
 * @property folders 文件夹列表
 */
@JsExport
data class BotGroupFileSystemList(
    val files: List<BotGroupFileEntry>,
    val folders: List<BotGroupFolderEntry>
)