package org.ntqqrev.acidify.logging

import kotlin.js.JsExport

/**
 * 日志等级枚举
 */
@JsExport
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
}