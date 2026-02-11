package org.ntqqrev.acidify

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.ntqqrev.acidify.common.android.AndroidAppInfo
import org.ntqqrev.acidify.common.android.AndroidSessionStore
import org.ntqqrev.acidify.common.android.AndroidSignProvider
import org.ntqqrev.acidify.internal.KuromeClient
import org.ntqqrev.acidify.logging.LogHandler
import org.ntqqrev.acidify.logging.LogLevel
import kotlin.js.JsStatic

class AndroidBot internal constructor(
    val appInfo: AndroidAppInfo,
    val sessionStore: AndroidSessionStore,
    signProvider: AndroidSignProvider,
    scope: CoroutineScope,
) : AbstractBot(scope) {
    override val client = KuromeClient(appInfo, sessionStore, signProvider, this::createLogger, scope)

    override val uin: Long
        get() = sessionStore.uin.takeIf { it != 0L }
            ?: throw IllegalStateException("用户尚未登录")

    override val uid: String
        get() = sessionStore.uid.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("用户尚未登录")

    companion object {
        @JsStatic
        suspend fun create(
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
        ).apply {
            launch {
                sharedLogFlow
                    .filter { it.level >= minLogLevel }
                    .collect {
                        logHandler.handleLog(
                            it.level,
                            it.tag,
                            it.messageSupplier(),
                            it.throwable
                        )
                    }
            }
            client.packetContext.startConnectLoop()
        }
    }
}