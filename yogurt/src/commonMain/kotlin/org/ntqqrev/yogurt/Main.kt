@file:JvmName("Main")

package org.ntqqrev.yogurt

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.jvm.JvmName

fun main() {
    val server = YogurtApp.createServer()
    server.start(wait = false)
    server.onSigint {
        server.stop(gracePeriodMillis = 2000L, timeoutMillis = 5000L)
    }
    runBlocking {
        delay(Long.MAX_VALUE)
    }
}