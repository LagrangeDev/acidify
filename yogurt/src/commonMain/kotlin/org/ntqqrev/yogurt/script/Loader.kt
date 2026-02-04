package org.ntqqrev.yogurt.script

import io.ktor.server.application.*
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.flow.SharedFlow
import org.ntqqrev.milky.Event
import org.ntqqrev.milky.milkyJsonModule

suspend fun Application.loadScript(script: String) {
    val qjs = createScriptEnvironment()
    qjs.evaluate<Any?>(script)
    val flow = dependencies.resolve<SharedFlow<Event>>()
    flow.collect {
        qjs.evaluate<Any?>("$internalEmitHandle(${milkyJsonModule.encodeToString(it)})")
    }
}