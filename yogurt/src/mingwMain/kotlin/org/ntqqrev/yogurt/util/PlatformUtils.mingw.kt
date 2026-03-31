@file:OptIn(ExperimentalForeignApi::class)

package org.ntqqrev.yogurt.util

import kotlinx.cinterop.*
import platform.windows.*
import platform.windows.CloseHandle as closeHandle

actual fun executeCommand(vararg args: String): CommandExecutionResult = memScoped {
    require(args.isNotEmpty()) { "Command arguments must not be empty." }

    val stdoutPath = createCommandTempFilePath("stdout")
    val stderrPath = createCommandTempFilePath("stderr")

    val securityAttributes = alloc<SECURITY_ATTRIBUTES>().apply {
        nLength = sizeOf<SECURITY_ATTRIBUTES>().convert()
        lpSecurityDescriptor = null
        bInheritHandle = TRUE
    }

    val stdoutHandle = CreateFileW(
        stdoutPath,
        GENERIC_WRITE.toUInt(),
        (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
        securityAttributes.ptr,
        CREATE_ALWAYS.toUInt(),
        FILE_ATTRIBUTE_NORMAL.toUInt(),
        null
    )
    if (stdoutHandle == INVALID_HANDLE_VALUE) {
        return@memScoped CommandExecutionResult(-1, "", "Failed to open stdout temp file (error=${GetLastError()}).")
    }

    val stderrHandle = CreateFileW(
        stderrPath,
        GENERIC_WRITE.toUInt(),
        (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
        securityAttributes.ptr,
        CREATE_ALWAYS.toUInt(),
        FILE_ATTRIBUTE_NORMAL.toUInt(),
        null
    )
    if (stderrHandle == INVALID_HANDLE_VALUE) {
        closeHandle(stdoutHandle)
        return@memScoped CommandExecutionResult(-1, "", "Failed to open stderr temp file (error=${GetLastError()}).")
    }

    var stdoutHandleOpen = true
    var stderrHandleOpen = true

    try {
        val startupInfo = alloc<STARTUPINFOW>().apply {
            cb = sizeOf<STARTUPINFOW>().convert()
            dwFlags = STARTF_USESTDHANDLES.toUInt()
            hStdInput = GetStdHandle(STD_INPUT_HANDLE)
            hStdOutput = stdoutHandle
            hStdError = stderrHandle
        }
        val processInfo = alloc<PROCESS_INFORMATION>()
        val commandLine = buildWindowsCommandLine(args).wcstr.ptr

        if (
            CreateProcessW(
                null,
                commandLine,
                null,
                null,
                TRUE,
                CREATE_NO_WINDOW.toUInt(),
                null,
                null,
                startupInfo.ptr,
                processInfo.ptr,
            ) == 0
        ) {
            return CommandExecutionResult(-1, "", "Failed to start process (error=${GetLastError()}).")
        }

        closeHandle(processInfo.hThread)
        closeHandle(stdoutHandle)
        closeHandle(stderrHandle)
        stdoutHandleOpen = false
        stderrHandleOpen = false

        WaitForSingleObject(processInfo.hProcess, INFINITE)

        val exitCode = alloc<DWORDVar>()
        if (GetExitCodeProcess(processInfo.hProcess, exitCode.ptr) == 0) {
            closeHandle(processInfo.hProcess)
            return CommandExecutionResult(
                -1,
                readCommandTempFile(stdoutPath),
                "Failed to read process exit code (error=${GetLastError()})."
            )
        }

        closeHandle(processInfo.hProcess)

        CommandExecutionResult(
            errorCode = exitCode.value.toInt(),
            stdout = readCommandTempFile(stdoutPath),
            stderr = readCommandTempFile(stderrPath),
        )
    } finally {
        if (stdoutHandleOpen) {
            closeHandle(stdoutHandle)
        }
        if (stderrHandleOpen) {
            closeHandle(stderrHandle)
        }
        deleteCommandTempFile(stdoutPath)
        deleteCommandTempFile(stderrPath)
    }
}

private fun buildWindowsCommandLine(args: Array<out String>): String =
    args.joinToString(" ") { quoteWindowsArgument(it) }

private fun quoteWindowsArgument(argument: String): String {
    if (argument.isEmpty() || argument.none { it == ' ' || it == '\t' || it == '"' }) {
        return argument
    }

    val result = StringBuilder("\"")
    var backslashes = 0

    for (ch in argument) {
        when (ch) {
            '\\' -> backslashes++
            '"' -> {
                result.append("\\".repeat(backslashes * 2 + 1))
                result.append('"')
                backslashes = 0
            }

            else -> {
                result.append("\\".repeat(backslashes))
                result.append(ch)
                backslashes = 0
            }
        }
    }

    result.append("\\".repeat(backslashes * 2))
    result.append('"')
    return result.toString()
}
