package org.ntqqrev.acidify.js

import kotlinx.coroutines.await
import kotlinx.coroutines.promise
import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.SessionStore
import org.ntqqrev.acidify.logging.LogHandler
import org.ntqqrev.acidify.logging.LogLevel
import org.ntqqrev.acidify.login
import org.ntqqrev.acidify.qrCodeLogin
import org.ntqqrev.acidify.renewSession

@JsExport
@JsName("Bot")
@AcidifyJsWrapper
class JsBot internal constructor(override val bot: Bot) : JsAbstractBot(bot) {
    fun login(queryInterval: Long = 3000L, preloadContacts: Boolean = false) = promise {
        bot.login(queryInterval, preloadContacts)
    }

    fun qrCodeLogin(queryInterval: Long = 3000L, preloadContacts: Boolean = false) = promise {
        bot.qrCodeLogin(queryInterval, preloadContacts)
    }

    fun renewSession() = promise { bot.renewSession() }

    companion object {
        @JsStatic
        fun create(
            appInfo: AppInfo,
            sessionStore: SessionStore,
            signProvider: JsSignProvider,
            jsScope: JsCoroutineScope,
            minLogLevel: LogLevel,
            logHandler: LogHandler
        ) = JsBot(
            Bot(
                appInfo = appInfo,
                sessionStore = sessionStore,
                signProvider = { cmd, src, seq ->
                    signProvider.sign(cmd, src, seq).await()
                },
                scope = jsScope.value,
                minLogLevel = minLogLevel,
                logHandler = logHandler,
            )
        )
    }
}