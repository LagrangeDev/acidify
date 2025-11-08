package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 群文件上传事件
 * @property groupUin 群号
 * @property userUin 发送者 QQ 号
 * @property userUid 发送者 uid
 * @property fileId 文件 ID
 * @property fileName 文件名称
 * @property fileSize 文件大小（字节）
 */
@JsExport
class GroupFileUploadEvent(
    val groupUin: Long,
    val userUin: Long,
    val userUid: String,
    val fileId: String,
    val fileName: String,
    val fileSize: Long
) : AcidifyEvent