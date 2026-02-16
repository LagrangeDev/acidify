package org.ntqqrev.yogurt

import io.ktor.server.engine.*

actual fun EmbeddedServer<*, *>.onSigint(hook: () -> Unit) {
    addShutdownHook(hook)
}