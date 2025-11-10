package org.ntqqrev.acidify

import kotlinx.coroutines.*
import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.SessionStore
import org.ntqqrev.acidify.common.SignProvider
import org.ntqqrev.acidify.logging.LogHandler
import org.ntqqrev.acidify.logging.LogLevel
import org.ntqqrev.acidify.message.BotOutgoingMessageBuilder
import kotlin.js.Promise
import kotlin.random.Random

@JsExport
class JsCoroutineScope(
    val isSupervised: Boolean = false
) {
    internal val value = CoroutineScope(
        Dispatchers.Default
                + if (isSupervised) SupervisorJob() else kotlinx.coroutines.Job()
    )

    fun cancel() {
        value.cancel()
    }
}

@JsExport
@JsName("Bot")
class JsBot internal constructor(private val bot: Bot) {
    fun sendFriendMessage(
        friendUin: Long,
        clientSequence: Long = Random.nextLong(),
        random: Int = Random.nextInt(),
        build: (BotOutgoingMessageBuilder) -> Promise<Unit>
    ) = bot.promise {
        bot.sendFriendMessage(friendUin, clientSequence, random) {
            build(this).await()
        }
    }

    fun sendGroupMessage(
        groupUin: Long,
        clientSequence: Long = Random.nextLong(),
        random: Int = Random.nextInt(),
        build: (BotOutgoingMessageBuilder) -> Promise<Unit>
    ) = bot.promise {
        bot.sendGroupMessage(groupUin, clientSequence, random) {
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