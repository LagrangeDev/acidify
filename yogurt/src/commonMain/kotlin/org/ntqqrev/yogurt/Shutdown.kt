package org.ntqqrev.yogurt

import io.ktor.server.engine.EmbeddedServer

expect fun EmbeddedServer<*, *>.onSigint(hook: () -> Unit)