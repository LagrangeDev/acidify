package org.ntqqrev.yogurt.util

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.ObjectBindingScope
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import org.ntqqrev.acidify.Bot
import org.ntqqrev.milky.milkyJsonModule
import org.ntqqrev.yogurt.api.MilkyApiContext
import org.ntqqrev.yogurt.api.MilkyApiHandler
import org.ntqqrev.yogurt.script.apiHandle
import org.ntqqrev.yogurt.script.internalApiHandle
import org.ntqqrev.yogurt.script.rootHandle

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : Any, reified R : Any> Application.defineJsApi(
    qjs: QuickJs,
    scope: ObjectBindingScope,
    handler: MilkyApiHandler<T, R>
) {
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
        val bot = dependencies.resolve<Bot>()
        val context = MilkyApiContext(bot, this)
        val resp = handler.callHandler(context, milkyJsonModule.decodeFromString(payloadString))
        milkyJsonModule.encodeToString(resp)
    }
}