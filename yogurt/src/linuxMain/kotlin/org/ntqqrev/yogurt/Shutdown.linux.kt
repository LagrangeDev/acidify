package org.ntqqrev.yogurt

import io.ktor.server.engine.*
import io.ktor.server.plugins.di.dependencies
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.runBlocking
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.offline
import platform.posix.SIGINT
import platform.posix.exit
import platform.posix.signal

val botRef = atomic<AbstractBot?>(null)

@OptIn(ExperimentalForeignApi::class)
actual fun EmbeddedServer<*, *>.onSigint(hook: () -> Unit) {
    botRef.value = runBlocking { application.dependencies.resolve<AbstractBot>() }
    // On Linux targets, the shutdown hook gets unexpectedly overridden by Ktor's internal shutdown hook;
    // and `server.stop()` blocks forever on Linux,
    // so we have to use a `exit(0)` to force the process to exit immediately after the shutdown hook is executed.
    // See KTOR-9308 and KTOR-9309 for more details.
    signal(SIGINT, staticCFunction { _ ->
        println("Received SIGINT, shutting down...")
        runBlocking {
            botRef.value?.offline()
        }
        exit(0)
    })
}