package org.ntqqrev.yogurt.script

import io.ktor.server.application.*
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.flow.SharedFlow
import org.ntqqrev.acidify.Bot
import org.ntqqrev.milky.Event
import org.ntqqrev.milky.milkyJsonModule

suspend fun Application.loadScripts(scripts: List<Script>) {
    val bot = dependencies.resolve<Bot>()
    val logger = bot.createLogger("ScriptLoader")
    val qjs = createScriptEnvironment()
    scripts.forEach {
        qjs.evaluate<Any?>(
            code = it.content,
            filename = it.name,
            asModule = true,
        )
        logger.i { "脚本 ${it.name.removeSuffix(".yogurtx.js")} 加载完成" }
    }
    logger.i { "加载了 ${scripts.size} 个脚本" }
    val flow = dependencies.resolve<SharedFlow<Event>>()
    flow.collect {
        qjs.evaluate<Any?>("$internalEmitHandle(${milkyJsonModule.encodeToString(it)})")
    }
}