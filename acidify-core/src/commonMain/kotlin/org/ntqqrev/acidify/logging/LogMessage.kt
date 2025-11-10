package org.ntqqrev.acidify.logging

/**
 * 日志消息
 * @property level 日志级别
 * @property tag 日志标签
 * @property messageSupplier 延迟提供的日志消息
 * @property throwable 可选的异常对象，仅在 WARN 或 ERROR 级别时提供
 */
class LogMessage(
    val level: LogLevel,
    val tag: String,
    val messageSupplier: suspend () -> String,
    val throwable: Throwable? = null,
) {
    init {
        require(throwable == null || level >= LogLevel.WARN) {
            "Throwable should only be provided for WARN or ERROR level logs"
        }
    }
}