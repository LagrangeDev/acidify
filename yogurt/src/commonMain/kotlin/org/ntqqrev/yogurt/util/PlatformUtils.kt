package org.ntqqrev.yogurt.util

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemTemporaryDirectory
import org.ntqqrev.yogurt.fs.withFs
import kotlin.random.Random

data class CommandExecutionResult(
    val errorCode: Int,
    val stdout: String,
    val stderr: String,
)

expect fun executeCommand(vararg args: String): CommandExecutionResult

fun createCommandTempFilePath(kind: String): String {
    while (true) {
        withFs {
            val candidate = Path(
                SystemTemporaryDirectory,
                "yogurt-$kind-${Random.nextLong().toULong().toString(16)}.tmp",
            )
            if (exists(candidate)) {
                continue
            }

            sink(candidate).buffered().use { }
            return candidate.toString()
        }
    }
}

fun readCommandTempFile(path: String): String = withFs {
    val file = Path(path)
    if (!exists(file)) {
        return ""
    }

    return Path(path).readText()
}

fun deleteCommandTempFile(path: String) = withFs {
    val file = Path(path)
    if (exists(file)) {
        delete(file, mustExist = false)
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
