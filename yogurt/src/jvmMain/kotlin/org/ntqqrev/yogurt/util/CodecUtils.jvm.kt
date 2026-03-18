package org.ntqqrev.yogurt.util

import java.io.BufferedReader
import java.io.InputStreamReader

internal actual fun runCliCommand(arguments: List<String>): CommandResult {
    val process = ProcessBuilder(arguments)
        .redirectErrorStream(true)
        .start()
    val output = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
        reader.readText()
    }
    val exitCode = process.waitFor()
    return CommandResult(exitCode = exitCode, output = output)
}
