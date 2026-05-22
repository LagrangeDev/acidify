package org.ntqqrev.yogurt.scripting

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.ObjectBindingScope
import com.dokar.quickjs.binding.define
import io.ktor.server.application.*
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.milky.MilkyContext
import org.ntqqrev.acidify.milky.api.MilkyApiHandler
import org.ntqqrev.acidify.milky.api.apiHandlers
import org.ntqqrev.acidify.milky.mediaSourceScoped
import org.ntqqrev.ktfs.withFs
import org.ntqqrev.milky.milkyJsonModule
import org.ntqqrev.yogurt.scripting.stdlib.defineStdlibConsole
import org.ntqqrev.yogurt.scripting.stdlib.defineStdlibHttp
import org.ntqqrev.yogurt.scriptsPath
import kotlin.time.DurationUnit
import kotlin.time.measureTime

const val rootHandle = "yogurt"
const val apiHandle = "api"
const val eventHandle = "event"

const val internalApiHandle = "__yogurtInternal_api"
const val internalEventMapHandle = "__yogurtInternal_eventMap"
const val internalEmitHandle = "__yogurtInternal_emit"

class Script(
    val name: String,
    val content: String,
)

context(ctx: MilkyContext)
suspend fun Application.configureScripting() = QuickJs.create(jobDispatcher = Dispatchers.Default).apply {
    defineStdlibConsole()
    defineStdlibHttp()

    evaluate<Any?>(
        """
            const $rootHandle = {
                $apiHandle: {},
                $eventHandle: {},
            };
        """.trimIndent()
    )

    define(internalApiHandle) {
        context(this@apply, this) {
            apiHandlers.forEach {
                defineJsApi(it)
            }
        }
    }

    evaluate<Any?>(
        $$"""
            const $$internalEventMapHandle = new Map();
            
            $$rootHandle.$$eventHandle.on = (eventName, listener) => {
                if (!$$internalEventMapHandle.has(eventName)) {
                    $$internalEventMapHandle.set(eventName, []);
                }
                $$internalEventMapHandle.get(eventName).push(listener);
            };
            
            function $$internalEmitHandle(event) {
                const eventName = event.event_type;
                if ($$internalEventMapHandle.has(eventName)) {
                    Promise.all($$internalEventMapHandle.get(eventName).map(async (listener) => {
                        try {
                            await listener(event);
                        } catch (error) {
                            console.error(`Error in event listener for ${eventName}:`, error);
                        }
                    }));
                }
            }
        """.trimIndent()
    )

    monitor.subscribe(ApplicationStarted) {
        this@configureScripting.launch {
            loadScripts()
        }
    }
}

context(qjs: QuickJs, scope: ObjectBindingScope, ctx: MilkyContext)
private fun <T : Any, R : Any> defineJsApi(handler: MilkyApiHandler<T, R>) {
    val methodName = handler.path.removePrefix("/")

    runBlocking {
        qjs.evaluate<Any?>(
            """
                $rootHandle.$apiHandle.$methodName = async (payload) => {
                    const payloadStr = payload ? JSON.stringify(payload) : '{}';
                    const respStr = await $internalApiHandle.$methodName(payloadStr);
                    return JSON.parse(respStr);
                };
            """.trimIndent()
        )
    }

    scope.asyncFunction(methodName) { args ->
        require(args.size == 1)
        val payloadString = args[0] as? String
            ?: throw IllegalArgumentException("Expected argument to be a JSON string")
        val logger = ctx.bot.createLogger("Scripting")
        var resp: JsonElement
        try {
            val duration = measureTime {
                resp = mediaSourceScoped(
                    onDisposeFailure = { source, exception ->
                        logger.e(exception) {
                            "释放资源文件 $source 时出现错误"
                        }
                    }
                ) {
                    handler.handleRaw(Json.parseToJsonElement(payloadString))
                }
            }
            logger.i {
                "脚本调用 API ${handler.path}（成功 ${duration.toString(DurationUnit.MILLISECONDS)}）"
            }
            Json.encodeToString(resp)
        } catch (e: Exception) {
            logger.e(e) { "脚本调用 API ${handler.path}（失败 ${e::class.simpleName}）" }
            throw e
        }
    }
}

context(ctx: MilkyContext)
private suspend fun QuickJs.loadScripts() = withFs {
    val bot = ctx.application.dependencies.resolve<AbstractBot>()
    val logger = bot.createLogger("ScriptLoader")

    if (!scriptsPath.exists) {
        createDirectories(scriptsPath)
    }

    val scripts = scriptsPath.children
        .filter { it.name.endsWith(".yogurtx.js") }
        .map {
            ctx.async {
                Script(
                    name = it.name,
                    content = withContext(Dispatchers.IO) {
                        it.readText()
                    }
                )
            }
        }
        .awaitAll()
    if (scripts.isNotEmpty()) {
        scripts.forEach {
            evaluate<Any?>(
                code = it.content,
                filename = it.name,
                asModule = true,
            )
            logger.i { "脚本 ${it.name.removeSuffix(".yogurtx.js")} 加载完成" }
        }
        logger.i { "加载了 ${scripts.size} 个脚本" }
        ctx.eventFlow.collect {
            evaluate<Any?>("$internalEmitHandle(${milkyJsonModule.encodeToString(it)})")
        }
    }
}