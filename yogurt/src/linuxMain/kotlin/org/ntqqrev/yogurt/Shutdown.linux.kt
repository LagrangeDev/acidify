package org.ntqqrev.yogurt

import io.ktor.server.engine.EmbeddedServer
import platform.posix.exit

actual fun EmbeddedServer<*, *>.platformShutdown() {
    // On Linux targets, the shutdown hook gets unexpectedly overridden by Ktor's internal shutdown hook;
    // and `server.stop()` blocks forever on Linux,
    // so we have to use a `exit(0)` to force the process to exit immediately after the shutdown hook is executed.
    // See KTOR-9308 and KTOR-9309 for more details.
    exit(0)
}