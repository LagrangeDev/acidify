package org.ntqqrev.acidify.js

import kotlinx.coroutines.await
import kotlinx.coroutines.promise
import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.SessionStore
import org.ntqqrev.acidify.common.SignProvider
import org.ntqqrev.acidify.logging.LogHandler
import org.ntqqrev.acidify.logging.LogLevel
import org.ntqqrev.acidify.message.BotOutgoingMessageBuilder
import kotlin.js.Promise

@JsExport
@JsName("Bot")
class JsBot internal constructor(private val bot: Bot) {
    fun sendFriendMessage(
        friendUin: JsNumber,
        build: (BotOutgoingMessageBuilder) -> Promise<Unit>
    ) = bot.promise {
        bot.sendFriendMessage(friendUin.toLong()) {
            build(this).await()
        }
    }

    fun sendFriendMessageRich(
        friendUin: JsNumber,
        clientSequence: JsNumber,
        random: Int,
        build: (BotOutgoingMessageBuilder) -> Promise<Unit>
    ) = bot.promise {
        bot.sendFriendMessage(friendUin.toLong(), clientSequence.toLong(), random) {
            build(this).await()
        }
    }

    fun sendGroupMessage(
        groupUin: JsNumber,
        build: (BotOutgoingMessageBuilder) -> Promise<Unit>
    ) = bot.promise {
        bot.sendGroupMessage(groupUin.toLong()) {
            build(this).await()
        }
    }

    fun sendGroupMessageRich(
        groupUin: JsNumber,
        clientSequence: JsNumber,
        random: Int,
        build: (BotOutgoingMessageBuilder) -> Promise<Unit>
    ) = bot.promise {
        bot.sendGroupMessage(groupUin.toLong(), clientSequence.toLong(), random) {
            build(this).await()
        }
    }

    companion object {
        @JsStatic
        fun create(
            appInfo: AppInfo,
            sessionStore: SessionStore,
            signProvider: JsSignProvider,
            jsScope: JsCoroutineScope,
            minLogLevel: LogLevel,
            logHandler: LogHandler
        ): Promise<JsBot> = jsScope.value.promise {
            JsBot(
                Bot.create(
                    appInfo = appInfo,
                    sessionStore = sessionStore,
                    signProvider = { cmd, src, seq ->
                        signProvider.sign(cmd, src, seq).await()
                    },
                    scope = jsScope.value,
                    minLogLevel = minLogLevel,
                    logHandler = logHandler
                )
            )
        }
    }
}