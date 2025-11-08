package org.ntqqrev.acidify.exception

import kotlin.js.JsExport

/**
 * 消息发送异常
 * @property resultCode 错误码
 * @property errorMessage 错误信息
 */
@JsExport
class MessageSendException(
    val resultCode: Int,
    val errorMessage: String
) : Exception("Message sending failed with code $resultCode: $errorMessage")