package org.ntqqrev.yogurt.util

import org.ntqqrev.yogurt.fs.withFs
import java.io.File

actual fun executeCommand(vararg args: String): CommandExecutionResult = withFs {
    withCommandTempFile { stdout, stderr ->
        require(args.isNotEmpty()) { "Command arguments must not be empty." }

        try {
            val process = ProcessBuilder(args.toList())
                .redirectOutput(File(stdout.toString()))
                .redirectError(File(stderr.toString()))
                .start()

            CommandExecutionResult(
                errorCode = process.waitFor(),
                stdout = stdout.readText(),
                stderr = stderr.readText(),
            )
        } catch (e: Exception) {
            CommandExecutionResult(
                errorCode = -1,
                stdout = "",
                stderr = e.message ?: e.toString(),
            )
        }
    }
}
