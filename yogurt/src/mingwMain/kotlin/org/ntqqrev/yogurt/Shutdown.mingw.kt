package org.ntqqrev.yogurt

import io.ktor.server.engine.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import org.ntqqrev.yogurt.YogurtApp.t
import platform.windows.*

@OptIn(ExperimentalForeignApi::class)
actual fun EmbeddedServer<*, *>.onSigint(hook: () -> Unit) {
    SetConsoleCtrlHandler(staticCFunction { fdwCtrlType ->
        when (fdwCtrlType.toInt()) {
            CTRL_C_EVENT, CTRL_CLOSE_EVENT -> {
                t.println("收到关闭信号，正在关闭...")
                ExitProcess(0u)
                TRUE
            }

            else -> FALSE
        }
    }, TRUE)
}

actual fun halt(status: Int) {
    ExitProcess(0u)
}