@file:OptIn(ExperimentalWasmJsInterop::class)

package org.ntqqrev.acidify

import kotlinx.coroutines.*
import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.SessionStore
import org.ntqqrev.acidify.common.SignProvider
import org.ntqqrev.acidify.logging.LogHandler
import org.ntqqrev.acidify.logging.LogLevel
import org.ntqqrev.acidify.message.BotOutgoingMessageBuilder
import kotlin.js.Promise

@JsExport
class JsCoroutineScope(
    val isSupervised: Boolean = false
) {
    internal val value = CoroutineScope(
        Dispatchers.Default
                + if (isSupervised) SupervisorJob() else Job()
    )

    fun cancel() {
        value.cancel()
    }
}

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
}

@JsExport
@JsName("createBot")
fun JsBot(
    appInfo: AppInfo,
    sessionStore: SessionStore,
    signProvider: SignProvider,
    jsScope: JsCoroutineScope,
    minLogLevel: LogLevel,
    logHandler: LogHandler
): Promise<JsBot> = jsScope.value.promise {
    JsBot(Bot.create(appInfo, sessionStore, signProvider, jsScope.value, minLogLevel, logHandler))
}