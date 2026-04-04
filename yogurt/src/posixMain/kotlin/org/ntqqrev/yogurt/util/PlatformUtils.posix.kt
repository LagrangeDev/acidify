@file:OptIn(ExperimentalForeignApi::class)

package org.ntqqrev.yogurt.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import org.ntqqrev.yogurt.fs.withFs
import platform.posix.errno
import platform.posix.strerror
import platform.posix.system

actual fun executeCommand(vararg args: String): CommandExecutionResult = withFs {
    withCommandTempFile { stdout, stderr ->
        require(args.isNotEmpty()) { "Command arguments must not be empty." }

        val status = system(buildPosixRedirectedCommand(args, stdout.toString(), stderr.toString()))
        if (status < 0) {
            return CommandExecutionResult(-1, "", strerror(errno)?.toKString() ?: "Failed to execute command.")
        }

        CommandExecutionResult(
            errorCode = exitCodeFromStatus(status),
            stdout = stdout.readText(),
            stderr = stderr.readText(),
        )
    }
}

private fun exitCodeFromStatus(status: Int): Int = when {
    (status and 0x7f) == 0 -> (status ushr 8) and 0xff
    (status and 0x7f) != 0x7f -> 128 + (status and 0x7f)
    else -> -1
}