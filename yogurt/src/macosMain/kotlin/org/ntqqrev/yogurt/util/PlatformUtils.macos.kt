@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package org.ntqqrev.yogurt.util

import kotlinx.cinterop.*
import org.ntqqrev.yogurt.fs.withFs
import platform.Foundation.NSError
import platform.Foundation.NSTask
import platform.Foundation.NSURL
import platform.posix.usleep

actual fun executeCommand(vararg args: String): CommandExecutionResult = withFs {
    withCommandTempFile { stdout, stderr ->
        require(args.isNotEmpty()) { "Command arguments must not be empty." }

        val task = NSTask()
        task.executableURL = NSURL(fileURLWithPath = "/bin/sh")
        task.arguments = listOf("-c", buildPosixRedirectedCommand(args, stdout.toString(), stderr.toString()))

        val launchError = memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            if (task.launchAndReturnError(error.ptr)) {
                null
            } else {
                error.value?.localizedDescription ?: "Failed to start process."
            }
        }
        if (launchError != null) {
            return CommandExecutionResult(-1, "", launchError)
        }

        while (task.running) {
            usleep(10_000u)
        }

        CommandExecutionResult(
            errorCode = task.terminationStatus,
            stdout = stdout.readText(),
            stderr = stderr.readText(),
        )
    }
}
