package org.ntqqrev.acidify.logging

import kotlin.js.JsExport

/**
 * 简单的日志处理器，直接将日志输出到控制台
 */
@JsExport
object SimpleLogHandler : LogHandler {
    override fun handleLog(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        val logMessage = buildString {
            append("[${level.name}] ")
            append("[$tag] ")
            append(message)
            throwable?.let {
                append("\n")
                append(it.stackTraceToString())
            }
        }
        println(logMessage)
    }
}