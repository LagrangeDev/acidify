package org.ntqqrev.acidify.logging

import kotlin.js.JsExport

/**
 * 日志处理器接口，用于自定义日志记录行为
 */
@JsExport
fun interface LogHandler {
    fun handleLog(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}