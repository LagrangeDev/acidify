package org.ntqqrev.yogurt.util

import kotlinx.io.files.Path
import org.ntqqrev.ktfs.withFs

data class CommandExecutionResult(
    val errorCode: Int,
    val stdout: String,
    val stderr: String,
)

/**
 * Converts the path component of a `file://` URI, already decoded from percent-encoding, into a
 * local filesystem path.
 *
 * A Windows file URI keeps the leading slash of the URI (`file:///C:/dir/file.png` decodes to
 * `/C:/dir/file.png`), which is not part of the Windows path, so it is dropped there. POSIX paths
 * are valid as-is and are returned unchanged.
 */
internal expect fun normalizeFileUriPath(path: String): String

internal fun stripWindowsDrivePrefix(path: String): String =
    if (
        path.length >= 4 && path[0] == '/' &&
        (path[1] in 'A'..'Z' || path[1] in 'a'..'z') &&
        path[2] == ':' && path[3] == '/'
    ) {
        path.substring(1)
    } else {
        path
    }

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
