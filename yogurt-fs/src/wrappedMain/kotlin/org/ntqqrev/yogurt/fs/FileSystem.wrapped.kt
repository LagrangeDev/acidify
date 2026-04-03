package org.ntqqrev.yogurt.fs

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

actual val defaultFileSystem: FileSystem = WrappedFileSystem

internal object WrappedFileSystem : FileSystem {
    override fun exists(path: Path): Boolean {
        return SystemFileSystem.exists(path)
    }

    override fun delete(path: Path, mustExist: Boolean) {
        SystemFileSystem.delete(path, mustExist)
    }

    override fun createDirectories(path: Path, mustCreate: Boolean) {
        SystemFileSystem.createDirectories(path, mustCreate)
    }

    override fun atomicMove(source: Path, destination: Path) {
        SystemFileSystem.atomicMove(source, destination)
    }

    override fun source(path: Path): RawSource {
        return SystemFileSystem.source(path)
    }

    override fun sink(path: Path, append: Boolean): RawSink {
        return SystemFileSystem.sink(path, append)
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        return SystemFileSystem.metadataOrNull(path)
    }

    override fun resolve(path: Path): Path {
        return SystemFileSystem.resolve(path)
    }

    override fun list(directory: Path): Collection<Path> {
        return SystemFileSystem.list(directory)
    }
}