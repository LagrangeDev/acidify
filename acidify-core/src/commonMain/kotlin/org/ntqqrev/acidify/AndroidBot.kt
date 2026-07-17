package org.ntqqrev.acidify

import kotlinx.coroutines.CoroutineScope
import org.ntqqrev.acidify.common.android.AndroidAppInfo
import org.ntqqrev.acidify.common.android.AndroidSessionStore
import org.ntqqrev.acidify.common.android.AndroidSignProvider
import org.ntqqrev.acidify.internal.KuromeClient
import org.ntqqrev.acidify.logging.LogHandler
import org.ntqqrev.acidify.logging.LogLevel
import kotlin.js.JsStatic

class AndroidBot(
    val appInfo: AndroidAppInfo,
    val sessionStore: AndroidSessionStore,
    signProvider: AndroidSignProvider,
    scope: CoroutineScope,
    minLogLevel: LogLevel,
    logHandler: LogHandler
) : AbstractBot(scope, minLogLevel, logHandler) {
    override val client = KuromeClient(appInfo, sessionStore, signProvider, this::createLogger, scope)

    override val uin: Long
        get() = sessionStore.uin.takeIf { it != 0L }
            ?: throw IllegalStateException("用户尚未登录")

    override val uid: String
        get() = sessionStore.uid.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("用户尚未登录")

    companion object {
        @JsStatic
        @Deprecated(
            "请直接使用 AndroidBot 构造器创建实例",
            ReplaceWith("AndroidBot(appInfo = appInfo, sessionStore = sessionStore, signProvider = signProvider, scope = scope, minLogLevel = minLogLevel, logHandler = logHandler)")
        )
        fun create(
            appInfo: AndroidAppInfo,
            sessionStore: AndroidSessionStore,
            signProvider: AndroidSignProvider,
            scope: CoroutineScope,
            minLogLevel: LogLevel,
            logHandler: LogHandler,
        ): AndroidBot = AndroidBot(
            appInfo = appInfo,
            sessionStore = sessionStore,
            signProvider = signProvider,
            scope = scope,
            minLogLevel = minLogLevel,
            logHandler = logHandler,
        )
    }
}