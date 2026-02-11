package org.ntqqrev.acidify.logging

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.ntqqrev.acidify.AbstractBot
import kotlin.js.JsName

typealias MessageSupplier = suspend () -> String

/**
 * 日志记录器，将日志消息发送到 Bot 的共享日志流
 * @property bot 关联的 Bot 实例
 * @property tag 日志标签，通常为完整类名
 */
class Logger internal constructor(private val bot: AbstractBot, val tag: String) {
    private fun MutableSharedFlow<LogMessage>.emitAsync(message: LogMessage) {
        bot.launch {
            emit(message)
        }
    }

    fun v(messageSupplier: MessageSupplier) = bot.sharedLogFlow.emitAsync(
        LogMessage(LogLevel.VERBOSE, tag, messageSupplier)
    )

    fun d(messageSupplier: MessageSupplier) = bot.sharedLogFlow.emitAsync(
        LogMessage(LogLevel.DEBUG, tag, messageSupplier)
    )

    fun i(messageSupplier: MessageSupplier) = bot.sharedLogFlow.emitAsync(
        LogMessage(LogLevel.INFO, tag, messageSupplier)
    )

    @JsName("wNoThrowable")
    fun w(messageSupplier: MessageSupplier) = bot.sharedLogFlow.emitAsync(
        LogMessage(LogLevel.WARN, tag, messageSupplier)
    )

    fun w(t: Throwable, messageSupplier: MessageSupplier) = bot.sharedLogFlow.emitAsync(
        LogMessage(LogLevel.WARN, tag, messageSupplier, t)
    )

    @JsName("eNoThrowable")
    fun e(messageSupplier: MessageSupplier) = bot.sharedLogFlow.emitAsync(
        LogMessage(LogLevel.ERROR, tag, messageSupplier)
    )

    fun e(t: Throwable, messageSupplier: MessageSupplier) = bot.sharedLogFlow.emitAsync(
        LogMessage(LogLevel.ERROR, tag, messageSupplier, t)
    )
}