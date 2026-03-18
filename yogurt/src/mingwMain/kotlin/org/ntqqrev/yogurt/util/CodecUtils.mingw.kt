@file:OptIn(ExperimentalForeignApi::class)

package org.ntqqrev.yogurt.util

import kotlinx.cinterop.*
import platform.windows.*

internal actual fun runCliCommand(arguments: List<String>): CommandResult = memScoped {
    val stdoutRead = alloc<HANDLEVar>()
    val stdoutWrite = alloc<HANDLEVar>()
    val securityAttributes = alloc<SECURITY_ATTRIBUTES>().apply {
        nLength = sizeOf<SECURITY_ATTRIBUTES>().toUInt()
        lpSecurityDescriptor = null
        bInheritHandle = TRUE
    }

    check(CreatePipe(stdoutRead.ptr, stdoutWrite.ptr, securityAttributes.ptr, 0u) != 0) {
        "CreatePipe 失败，错误码=${GetLastError()}"
    }
    check(SetHandleInformation(stdoutRead.value, HANDLE_FLAG_INHERIT.toUInt(), 0u) != 0) {
        CloseHandle(stdoutRead.value)
        CloseHandle(stdoutWrite.value)
        "SetHandleInformation 失败，错误码=${GetLastError()}"
    }

    val startupInfo = alloc<STARTUPINFOW>().apply {
        cb = sizeOf<STARTUPINFOW>().toUInt()
        dwFlags = STARTF_USESTDHANDLES.toUInt()
        hStdOutput = stdoutWrite.value
        hStdError = stdoutWrite.value
        hStdInput = GetStdHandle(STD_INPUT_HANDLE)
    }
    val processInfo = alloc<PROCESS_INFORMATION>()
    val commandLine = buildString {
        append(arguments.joinToString(" ") { it.quoteForWindowsCommandLine() })
    }
    var processCreated = false

    try {
        val created = CreateProcessW(
            null,
            commandLine.wcstr.ptr,
            null,
            null,
            TRUE,
            0u,
            null,
            null,
            startupInfo.ptr,
            processInfo.ptr,
        )
        check(created != 0) {
            "CreateProcessW 失败，错误码=${GetLastError()}，命令=$commandLine"
        }
        processCreated = true
    } finally {
        CloseHandle(stdoutWrite.value)
        if (!processCreated) {
            CloseHandle(stdoutRead.value)
        }
    }

    try {
        val output = readPipe(stdoutRead.value)
        WaitForSingleObject(processInfo.hProcess, INFINITE)
        val exitCode = alloc<DWORDVar>()
        check(GetExitCodeProcess(processInfo.hProcess, exitCode.ptr) != 0) {
            "GetExitCodeProcess 失败，错误码=${GetLastError()}"
        }
        return CommandResult(
            exitCode = exitCode.value.toInt(),
            output = output,
        )
    } finally {
        CloseHandle(stdoutRead.value)
        CloseHandle(processInfo.hThread)
        CloseHandle(processInfo.hProcess)
    }
}

private fun readPipe(handle: CPointer<*>?): String = memScoped {
    val chunk = allocArray<ByteVar>(4096)
    val bytesRead = alloc<DWORDVar>()
    val output = mutableListOf<Byte>()

    while (true) {
        val ok = ReadFile(
            handle,
            chunk,
            4096u,
            bytesRead.ptr,
            null,
        )
        if (ok == 0 || bytesRead.value == 0u) {
            break
        }
        output += chunk.readBytes(bytesRead.value.toInt()).asList()
    }

    output.toByteArray().decodeToString()
}

private fun String.quoteForWindowsCommandLine(): String {
    if (isEmpty()) return "\"\""
    if (none { it.isWhitespace() || it == '"' }) return this

    val result = StringBuilder(length + 2)
    result.append('"')
    var backslashCount = 0
    for (char in this) {
        when (char) {
            '\\' -> backslashCount++
            '"' -> {
                result.append("\\".repeat(backslashCount * 2 + 1))
                result.append('"')
                backslashCount = 0
            }

            else -> {
                if (backslashCount > 0) {
                    result.append("\\".repeat(backslashCount))
                    backslashCount = 0
                }
                result.append(char)
            }
        }
    }
    if (backslashCount > 0) {
        result.append("\\".repeat(backslashCount * 2))
    }
    result.append('"')
    return result.toString()
}
