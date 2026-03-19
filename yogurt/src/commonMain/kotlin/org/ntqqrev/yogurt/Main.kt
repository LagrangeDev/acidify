@file:JvmName("Main")

package org.ntqqrev.yogurt

import com.github.ajalt.mordant.rendering.TextColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.ntqqrev.yogurt.YogurtApp.config
import org.ntqqrev.yogurt.YogurtApp.t
import org.ntqqrev.yogurt.util.isCausedByAddrInUse
import kotlin.jvm.JvmName

fun main() {
    val server = YogurtApp.createServer()
    try {
        server.start(wait = false)
        server.onSigint {
            server.stop(gracePeriodMillis = 2000L, timeoutMillis = 5000L)
        }
        runBlocking {
            delay(Long.MAX_VALUE)
        }
    } catch (e: Throwable) {
        if (e.isCausedByAddrInUse()) {
            t.println(
                TextColors.red(
                    """
                        无法启动服务器，可能是 ${config.milky.http.host}:${config.milky.http.port} 已被占用。
                        请检查是否有其他程序正在使用该地址，或者修改配置文件中的 host 和 port 后重试。
                    """.trimIndent()
                )
            )
        }
        throw e
    }
}