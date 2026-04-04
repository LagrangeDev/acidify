package org.ntqqrev.yogurt.util

import kotlinx.io.files.Path
import org.ntqqrev.yogurt.fs.withFs

data class CommandExecutionResult(
    val errorCode: Int,
    val stdout: String,
    val stderr: String,
)

expect fun executeCommand(vararg args: String): CommandExecutionResult

inline fun <R> withCommandTempFile(
    block: (stdout: Path, stderr: Path) -> R
): R {
    val stdout = createTempFile("stdout")
    val stderr = createTempFile("stderr")
    try {
        return block(stdout, stderr)
    } finally {
        withFs {
            delete(stdout, mustExist = false)
            delete(stderr, mustExist = false)
        }
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
