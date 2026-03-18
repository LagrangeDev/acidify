@file:OptIn(ExperimentalForeignApi::class)

package org.ntqqrev.yogurt.util

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen

internal actual fun runCliCommand(arguments: List<String>): CommandResult = memScoped {
    val command = buildString {
        append(arguments.joinToString(" ") { it.quoteForShell() })
        append(" 2>&1")
    }
    val pipe = popen(command, "r")
        ?: error("无法执行命令：${arguments.firstOrNull().orEmpty()}")

    val buffer = allocArray<ByteVar>(4096)
    val output = buildString {
        while (fgets(buffer, 4096, pipe) != null) {
            append(buffer.toKString())
        }
    }
    val status = pclose(pipe)
    CommandResult(
        exitCode = if (status == 0) 0 else 1,
        output = output,
    )
}

private fun String.quoteForShell(): String {
    return "'${replace("'", "'\"'\"'")}'"
}
