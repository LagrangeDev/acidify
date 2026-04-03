@file:OptIn(ExperimentalForeignApi::class)

package org.ntqqrev.yogurt.fs

import kotlinx.cinterop.*
import kotlinx.io.*
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.Path
import platform.windows.*

actual val defaultFileSystem: FileSystem = MingwFileSystem

object MingwFileSystem : FileSystem {
    override fun exists(path: Path): Boolean {
        return metadataOrNull(path) != null
    }

    override fun delete(path: Path, mustExist: Boolean) {
        val metadata = metadataOrNull(path)
        if (metadata == null) {
            if (mustExist) {
                throw FileNotFoundException("Path does not exist: $path")
            }
            return
        }

        val deleted = if (metadata.isDirectory) {
            RemoveDirectoryW(windowsPath(path))
        } else {
            DeleteFileW(windowsPath(path))
        }

        if (deleted == 0) {
            throw windowsIOException("delete", path, lastErrorCode())
        }
    }

    override fun createDirectories(path: Path, mustCreate: Boolean) {
        val pathString = windowsPath(path)
        val existing = metadataOrNull(pathString)
        if (existing != null) {
            if (!existing.isDirectory) {
                throw IOException("Path exists and is not a directory: $path")
            }
            if (mustCreate) {
                throw IOException("Directory already exists: $path")
            }
            return
        }

        for (candidate in directoryCreationChain(pathString)) {
            val candidateMetadata = metadataOrNull(candidate)
            if (candidateMetadata != null) {
                if (!candidateMetadata.isDirectory) {
                    throw IOException("Path exists and is not a directory: $candidate")
                }
                continue
            }

            if (CreateDirectoryW(candidate, null) == 0) {
                val error = lastErrorCode()
                if (error == ERROR_ALREADY_EXISTS || error == ERROR_FILE_EXISTS) {
                    val created = metadataOrNull(candidate)
                    if (created?.isDirectory == true) {
                        continue
                    }
                    throw IOException("Path exists and is not a directory: $candidate")
                }
                throw windowsIOException("create directory", candidate, error)
            }
        }
    }

    override fun atomicMove(source: Path, destination: Path) {
        val flags = (MOVEFILE_REPLACE_EXISTING or MOVEFILE_WRITE_THROUGH).toUInt()
        if (MoveFileExW(windowsPath(source), windowsPath(destination), flags) == 0) {
            throw windowsIOException("move", source, lastErrorCode(), destination)
        }
    }

    override fun source(path: Path): RawSource {
        val handle = openFileHandle(
            path = path,
            desiredAccess = GENERIC_READ,
            creationDisposition = OPEN_EXISTING.toUInt(),
            flagsAndAttributes = FILE_ATTRIBUTE_NORMAL.toUInt(),
        )
        return WindowsFileSource(path, handle)
    }

    override fun sink(path: Path, append: Boolean): RawSink {
        val handle = openFileHandle(
            path = path,
            desiredAccess = GENERIC_WRITE.toUInt(),
            creationDisposition = if (append) OPEN_ALWAYS.toUInt() else CREATE_ALWAYS.toUInt(),
            flagsAndAttributes = FILE_ATTRIBUTE_NORMAL.toUInt(),
        )
        if (append) {
            seek(handle, 0L, FILE_END.toUInt(), path)
        }
        return WindowsFileSink(path, handle)
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        return metadataOrNull(windowsPath(path))
    }

    override fun resolve(path: Path): Path {
        val metadata = metadataOrNull(path) ?: throw FileNotFoundException("Path does not exist: $path")
        val flags = if (metadata.isDirectory) FILE_FLAG_BACKUP_SEMANTICS.toUInt() else 0u
        val handle = openFileHandle(
            path = path,
            desiredAccess = 0u,
            creationDisposition = OPEN_EXISTING.toUInt(),
            flagsAndAttributes = flags,
        )

        try {
            val resolved = memScoped {
                val needed = GetFinalPathNameByHandleW(handle, null, 0u, VOLUME_NAME_DOS.toUInt())
                if (needed == 0u) {
                    throw windowsIOException("resolve", path, lastErrorCode())
                }

                val buffer = allocArray<UShortVar>(needed.toInt() + 1)
                val written = GetFinalPathNameByHandleW(
                    handle,
                    buffer,
                    (needed + 1u),
                    VOLUME_NAME_DOS.toUInt(),
                )
                if (written == 0u) {
                    throw windowsIOException("resolve", path, lastErrorCode())
                }
                buffer.reinterpret<ShortVar>().toKStringFromUtf16()
            }
            return Path(stripExtendedPrefix(resolved))
        } finally {
            closeHandle(handle, path)
        }
    }

    override fun list(directory: Path): Collection<Path> {
        val directoryString = windowsPath(directory)
        val metadata =
            metadataOrNull(directoryString) ?: throw FileNotFoundException("Directory does not exist: $directory")
        if (!metadata.isDirectory) {
            throw IOException("Path is not a directory: $directory")
        }

        return memScoped {
            val findData = alloc<WIN32_FIND_DATAW>()
            val searchPattern = buildChildPath(directoryString, "*")
            val handle = FindFirstFileW(searchPattern, findData.ptr)
            if (isInvalidHandle(handle)) {
                val error = lastErrorCode()
                if (error == ERROR_FILE_NOT_FOUND) {
                    return@memScoped emptyList()
                }
                throw windowsIOException("list", directory, error)
            }

            val children = mutableListOf<Path>()
            try {
                do {
                    val name = findData.cFileName.reinterpret<ShortVar>().toKStringFromUtf16()
                    if (name != "." && name != "..") {
                        children += Path(directory, name)
                    }
                } while (FindNextFileW(handle, findData.ptr) != 0)

                val error = lastErrorCode()
                if (error != ERROR_NO_MORE_FILES) {
                    throw windowsIOException("list", directory, error)
                }
            } finally {
                FindClose(handle)
            }
            children
        }
    }
}

private class WindowsFileSource(
    private val path: Path,
    private val handle: CPointer<out CPointed>?,
) : RawSource {
    private var closed = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "Source is closed" }
        require(byteCount >= 0) { "byteCount < 0: $byteCount" }
        if (byteCount == 0L) {
            return 0L
        }

        val requested = minOf(byteCount, (8 * 1024).toLong()).toInt()
        val chunk = ByteArray(requested)
        val bytesRead = memScoped {
            val read = alloc<UIntVar>()
            val ok = chunk.usePinned {
                ReadFile(handle, it.addressOf(0), requested.toUInt(), read.ptr, null)
            }
            if (ok == 0) {
                throw windowsIOException("read", path, lastErrorCode())
            }
            read.value.toInt()
        }

        if (bytesRead == 0) {
            return -1L
        }

        sink.write(chunk, 0, bytesRead)
        return bytesRead.toLong()
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        closeHandle(handle, path)
    }
}

private class WindowsFileSink(
    private val path: Path,
    private val handle: CPointer<out CPointed>?,
) : RawSink {
    private var closed = false

    override fun write(source: Buffer, byteCount: Long) {
        check(!closed) { "Sink is closed" }
        require(byteCount >= 0) { "byteCount < 0: $byteCount" }
        if (byteCount == 0L) {
            return
        }

        val chunk = ByteArray(8 * 1024)
        var remaining = byteCount
        while (remaining > 0L) {
            val toRead = minOf(remaining, chunk.size.toLong()).toInt()
            source.readTo(chunk, 0, toRead)

            var written = 0
            while (written < toRead) {
                val justWritten = memScoped {
                    val bytesWritten = alloc<UIntVar>()
                    val ok = chunk.usePinned {
                        WriteFile(
                            handle,
                            it.addressOf(written),
                            (toRead - written).toUInt(),
                            bytesWritten.ptr,
                            null,
                        )
                    }
                    if (ok == 0) {
                        throw windowsIOException("write", path, lastErrorCode())
                    }
                    bytesWritten.value.toInt()
                }

                if (justWritten <= 0) {
                    throw IOException("Write returned no progress for $path")
                }
                written += justWritten
            }

            remaining -= toRead.toLong()
        }
    }

    override fun flush() {
        check(!closed) { "Sink is closed" }
        if (FlushFileBuffers(handle) == 0) {
            throw windowsIOException("flush", path, lastErrorCode())
        }
    }

    override fun close() {
        if (closed) {
            return
        }

        var failure: Throwable? = null
        try {
            flush()
        } catch (t: Throwable) {
            failure = t
        }

        closed = true
        try {
            closeHandle(handle, path)
        } catch (t: Throwable) {
            if (failure != null) {
                failure.addSuppressed(t)
            } else {
                failure = t
            }
        }

        if (failure != null) {
            throw failure
        }
    }
}

private fun openFileHandle(
    path: Path,
    desiredAccess: UInt,
    creationDisposition: UInt,
    flagsAndAttributes: UInt,
): CPointer<out CPointed>? {
    val handle = CreateFileW(
        windowsPath(path),
        desiredAccess,
        (FILE_SHARE_READ or FILE_SHARE_WRITE or FILE_SHARE_DELETE).toUInt(),
        null,
        creationDisposition,
        flagsAndAttributes,
        null,
    )
    if (isInvalidHandle(handle)) {
        throw windowsIOException("open", path, lastErrorCode())
    }
    return handle
}

private fun metadataOrNull(path: String): FileMetadata? {
    return memScoped {
        val fileData = alloc<WIN32_FILE_ATTRIBUTE_DATA>()
        if (GetFileAttributesExW(
                path,
                _GET_FILEEX_INFO_LEVELS.GetFileExInfoStandard,
                fileData.ptr.reinterpret(),
            ) == 0
        ) {
            return@memScoped null
        }

        val attributes = fileData.dwFileAttributes.toInt()
        val isDirectory = attributes and FILE_ATTRIBUTE_DIRECTORY != 0
        val size = if (isDirectory) {
            0L
        } else {
            combineUnsignedLongs(fileData.nFileSizeHigh.toLong(), fileData.nFileSizeLow.toLong())
        }
        FileMetadata(
            isRegularFile = !isDirectory,
            isDirectory = isDirectory,
            size = size,
        )
    }
}

private fun seek(
    handle: CPointer<out CPointed>?,
    offset: Long,
    moveMethod: UInt,
    path: Path,
) {
    memScoped {
        val distance = alloc<LARGE_INTEGER>()
        distance.QuadPart = offset
        if (SetFilePointerEx(handle, distance.readValue(), null, moveMethod) == 0) {
            throw windowsIOException("seek", path, lastErrorCode())
        }
    }
}

private fun closeHandle(handle: CPointer<out CPointed>?, path: Path) {
    if (handle == null || handle == INVALID_HANDLE_VALUE) {
        return
    }
    if (CloseHandle(handle) == 0) {
        throw windowsIOException("close", path, lastErrorCode())
    }
}

private fun windowsIOException(
    operation: String,
    path: Path,
    errorCode: Int,
    otherPath: Path? = null,
): IOException {
    val targetSuffix = if (otherPath != null) " -> $otherPath" else ""
    val message = "Windows $operation failed for $path$targetSuffix (error=$errorCode)"
    return when (errorCode) {
        ERROR_FILE_NOT_FOUND, ERROR_PATH_NOT_FOUND -> FileNotFoundException(message)
        ERROR_ACCESS_DENIED,
        ERROR_DIR_NOT_EMPTY,
        ERROR_SHARING_VIOLATION,
        ERROR_NOT_SAME_DEVICE,
            -> IOException(message)

        else -> IOException(message)
    }
}

private fun windowsIOException(
    operation: String,
    path: String,
    errorCode: Int,
    otherPath: String? = null,
): IOException {
    val targetSuffix = if (otherPath != null) " -> $otherPath" else ""
    val message = "Windows $operation failed for $path$targetSuffix (error=$errorCode)"
    return when (errorCode) {
        ERROR_FILE_NOT_FOUND, ERROR_PATH_NOT_FOUND -> FileNotFoundException(message)
        else -> IOException(message)
    }
}

private fun lastErrorCode(): Int = GetLastError().toInt()

private fun isInvalidHandle(handle: CPointer<out CPointed>?): Boolean {
    return handle == null || handle == INVALID_HANDLE_VALUE || handle.toLong() == -1L
}

private fun windowsPath(path: Path): String = path.toString().replace('/', '\\')

private fun combineUnsignedLongs(high: Long, low: Long): Long {
    return ((high and 0xffffffffL) shl 32) or (low and 0xffffffffL)
}

private fun stripExtendedPrefix(path: String): String {
    return when {
        path.startsWith("\\\\?\\UNC\\") -> "\\\\" + path.removePrefix("\\\\?\\UNC\\")
        path.startsWith("\\\\?\\") -> path.removePrefix("\\\\?\\")
        else -> path
    }
}

private fun directoryCreationChain(path: String): List<String> {
    val normalized = path.replace('/', '\\').trimEnd('\\')
    val (root, parts) = splitWindowsPath(normalized)
    if (parts.isEmpty()) {
        return emptyList()
    }

    val chain = ArrayList<String>(parts.size)
    var current = root
    for (part in parts) {
        current = buildChildPath(current, part)
        chain += current
    }
    return chain
}

private fun buildChildPath(parent: String, child: String): String {
    return when {
        parent.isEmpty() -> child
        parent.endsWith('\\') -> parent + child
        else -> "$parent\\$child"
    }
}

private fun splitWindowsPath(path: String): Pair<String, List<String>> {
    if (path.startsWith("\\\\")) {
        val rawParts = path.split('\\').filter { it.isNotEmpty() }
        if (rawParts.size < 2) {
            return path to emptyList()
        }
        val root = "\\\\${rawParts[0]}\\${rawParts[1]}"
        return root to rawParts.drop(2)
    }

    if (path.length >= 2 && path[1] == ':') {
        val root = if (path.length >= 3 && path[2] == '\\') {
            path.substring(0, 3)
        } else {
            path.substring(0, 2)
        }
        val rest = path.substring(root.length).split('\\').filter { it.isNotEmpty() }
        return root to rest
    }

    if (path.startsWith("\\")) {
        return "\\" to path.drop(1).split('\\').filter { it.isNotEmpty() }
    }

    return "" to path.split('\\').filter { it.isNotEmpty() }
}