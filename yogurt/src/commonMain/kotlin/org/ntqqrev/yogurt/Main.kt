@file:JvmName("Main")

package org.ntqqrev.yogurt

import io.ktor.server.engine.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.jvm.JvmName

val serverRef = atomic<EmbeddedServer<*, *>?>(null)

fun main() {
    val server = YogurtApp.createServer()
    serverRef.value = server
    server.start(wait = false)
    server.addShutdownHook {
        server.platformShutdown()
    }
    runBlocking {
        delay(Long.MAX_VALUE)
    }
}