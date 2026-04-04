package org.ntqqrev.yogurt

import io.ktor.server.engine.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import org.ntqqrev.yogurt.YogurtApp.t
import platform.posix.SIGINT
import platform.posix._exit
import platform.posix.signal

@OptIn(ExperimentalForeignApi::class)
actual fun EmbeddedServer<*, *>.onSigint(hook: () -> Unit) {
    // On Posix targets, the shutdown hook gets unexpectedly overridden by Ktor's internal shutdown hook;
    // and `server.stop()` blocks forever on Linux,
    // so we have to use a `exit(0)` to force the process to exit immediately after the shutdown hook is executed.
    // See KTOR-9308 and KTOR-9309 for more details.
    signal(SIGINT, staticCFunction { _ ->
        t.println("收到 SIGINT 信号，正在关闭...")
        _exit(0)
    })
}

actual fun halt(status: Int) {
    _exit(status)
}