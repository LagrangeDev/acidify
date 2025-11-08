package org.ntqqrev.acidify.logging

import kotlin.js.JsExport

/**
 * 不执行任何操作的日志处理器
 */
@JsExport
object NopLogHandler : LogHandler {
    override fun handleLog(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        // do nothing
    }
}