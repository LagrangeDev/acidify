package org.ntqqrev.acidify

import kotlinx.coroutines.CoroutineScope
import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.SessionStore
import org.ntqqrev.acidify.common.SignProvider
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.logging.LogHandler
import org.ntqqrev.acidify.logging.LogLevel
import kotlin.js.JsStatic

/**
 * Acidify Bot 实例
 */
class Bot(
    val appInfo: AppInfo,
    val sessionStore: SessionStore,
    signProvider: SignProvider,
    scope: CoroutineScope,
    minLogLevel: LogLevel,
    logHandler: LogHandler,
) : AbstractBot(scope, minLogLevel, logHandler) {
    override val client = LagrangeClient(appInfo, sessionStore, signProvider, this::createLogger, scope)

    override val uin: Long
        get() = sessionStore.uin.takeIf { it != 0L }
            ?: throw IllegalStateException("用户尚未登录")

    override val uid: String
        get() = sessionStore.uid.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("用户尚未登录")

    companion object {
        @JsStatic
        @Deprecated(
            "请直接使用 Bot 构造器创建实例",
            ReplaceWith("Bot(appInfo = appInfo, sessionStore = sessionStore, signProvider = signProvider, scope = scope, minLogLevel = minLogLevel, logHandler = logHandler)")
        )
        fun create(
            appInfo: AppInfo,
            sessionStore: SessionStore,
            signProvider: SignProvider,
            scope: CoroutineScope,
            minLogLevel: LogLevel,
            logHandler: LogHandler,
        ): Bot = Bot(
            appInfo = appInfo,
            sessionStore = sessionStore,
            signProvider = signProvider,
            scope = scope,
            minLogLevel = minLogLevel,
            logHandler = logHandler,
        )
    }
}
