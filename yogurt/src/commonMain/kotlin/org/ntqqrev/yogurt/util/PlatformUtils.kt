package org.ntqqrev.yogurt.util

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.random.Random

data class CommandExecutionResult(
    val errorCode: Int,
    val stdout: String,
    val stderr: String,
)

expect fun executeCommand(vararg args: String): CommandExecutionResult

fun createCommandTempFilePath(kind: String): String {
    while (true) {
        val candidate = Path(
            SystemTemporaryDirectory,
            "yogurt-$kind-${Random.nextLong().toULong().toString(16)}.tmp",
        )
        if (SystemFileSystem.exists(candidate)) {
            continue
        }

        SystemFileSystem.sink(candidate).buffered().use { }
        return candidate.toString()
    }
}

fun readCommandTempFile(path: String): String {
    val file = Path(path)
    if (!SystemFileSystem.exists(file)) {
        return ""
    }

    return readByteArrayFromFilePath(path).decodeToString()
}

fun deleteCommandTempFile(path: String) {
    val file = Path(path)
    if (SystemFileSystem.exists(file)) {
        SystemFileSystem.delete(file, mustExist = false)
    }
}

fun buildPosixRedirectedCommand(
    args: Array<out String>,
    stdoutPath: String,
    stderrPath: String,
): String =
    buildString {
        append(args.joinToString(" ") { quotePosixArgument(it) })
        append(" > ")
        append(quotePosixArgument(stdoutPath))
        append(" 2> ")
        append(quotePosixArgument(stderrPath))
    }

private fun quotePosixArgument(argument: String): String =
    "'" + argument.replace("'", "'\"'\"'") + "'"
