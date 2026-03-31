package org.ntqqrev.yogurt.util

import java.io.File

actual fun executeCommand(vararg args: String): CommandExecutionResult {
    require(args.isNotEmpty()) { "Command arguments must not be empty." }

    val stdoutPath = createCommandTempFilePath("stdout")
    val stderrPath = createCommandTempFilePath("stderr")
    val stdoutFile = File(stdoutPath)
    val stderrFile = File(stderrPath)

    return try {
        val process = ProcessBuilder(args.toList())
            .redirectOutput(stdoutFile)
            .redirectError(stderrFile)
            .start()

        CommandExecutionResult(
            errorCode = process.waitFor(),
            stdout = readCommandTempFile(stdoutPath),
            stderr = readCommandTempFile(stderrPath),
        )
    } catch (e: Exception) {
        CommandExecutionResult(
            errorCode = -1,
            stdout = "",
            stderr = e.message ?: e.toString(),
        )
    } finally {
        deleteCommandTempFile(stdoutPath)
        deleteCommandTempFile(stderrPath)
    }
}
