package org.ntqqrev.yogurt

import io.ktor.server.engine.EmbeddedServer

actual fun EmbeddedServer<*, *>.platformShutdown() {
    stop(gracePeriodMillis = 2000L, timeoutMillis = 5000L)
}