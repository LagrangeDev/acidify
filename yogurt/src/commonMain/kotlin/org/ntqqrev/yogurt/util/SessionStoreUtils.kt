package org.ntqqrev.yogurt.util

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.event.AndroidSessionStoreUpdatedEvent
import org.ntqqrev.acidify.event.SessionStoreUpdatedEvent
import org.ntqqrev.yogurt.androidSessionStorePath
import org.ntqqrev.yogurt.fs.withFs
import org.ntqqrev.yogurt.sessionStorePath

fun Application.configureSessionStoreAutoSave() = launch {
    val bot = dependencies.resolve<AbstractBot>()
    val logger = bot.createLogger("SessionStoreUtils")
    if (isPC) {
        bot.eventFlow.filterIsInstance<SessionStoreUpdatedEvent>().collect {
            logger.i { "SessionStore 已更新，正在保存至文件..." }
            withFs { sessionStorePath.write(it.sessionStore.toJson()) }
        }
    } else if (isAndroid) {
        bot.eventFlow.filterIsInstance<AndroidSessionStoreUpdatedEvent>().collect {
            logger.i { "Android SessionStore 已更新，正在保存至文件..." }
            withFs { androidSessionStorePath.write(it.sessionStore.toJson()) }
        }
    }
}