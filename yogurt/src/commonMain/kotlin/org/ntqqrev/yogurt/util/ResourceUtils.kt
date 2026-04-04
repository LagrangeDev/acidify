package org.ntqqrev.yogurt.util

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemTemporaryDirectory
import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.acidify.common.MediaSource.Companion.toMediaSource
import org.ntqqrev.yogurt.fs.withFs
import kotlin.io.encoding.Base64
import kotlin.random.Random

private val httpClient = HttpClient()

suspend fun resolveUri(uri: String): MediaSource = withContext(Dispatchers.IO) {
    when {
        uri.startsWith("file://") -> withFs {
            val filePath = Path(uri.removePrefix("file://").decodeURLPart())
            if (!exists(filePath)) {
                throw IOException("File not found: $filePath")
            }
            LocalFileMediaSource(filePath)
        }

        uri.startsWith("http://") || uri.startsWith("https://") -> {
            val response = httpClient.get(uri)
            if (!response.status.isSuccess()) {
                throw IOException("Failed to download HTTP URL $uri: ${response.status}")
            }
            TempFileMediaSource(
                kind = "http-cache",
                initialRawSource = response.bodyAsChannel().asSource(),
            )
        }

        uri.startsWith("base64://") -> {
            val base64Data = uri.removePrefix("base64://")
            Base64.decode(base64Data).toMediaSource()
        }

        else -> throw IllegalArgumentException("Unsupported URI scheme: $uri")
    }
}

fun createTempFile(kind: String, ext: String = "tmp"): Path = withFs {
    var candidate: Path
    do {
        candidate = Path(
            SystemTemporaryDirectory,
            "yogurt-$kind-${Random.nextLong().toULong().toString(16)}.$ext",
        )
    } while (exists(candidate))
    // Touch the file
    sink(candidate).buffered().use { }
    candidate
}


abstract class LazyMediaSource : MediaSource() {
    private val lazyData: ByteArray by lazy {
        super.readByteArray()
    }

    override fun readByteArray(): ByteArray {
        return lazyData
    }
}

class LocalFileMediaSource(val path: Path) : LazyMediaSource() {
    override val size: Long = withFs {
        metadataOrNull(path)?.size
            ?: throw IOException("File not found: $path")
    }

    override fun openRawSource(): RawSource = withFs {
        source(path)
    }

    override fun dispose() {
        // No-op
    }

    override fun toString(): String {
        return "LocalFileMediaSource(path=$path)"
    }
}

class TempFileMediaSource(
    kind: String,
    ext: String = "tmp",
    initialRawSource: RawSource? = null,
) : LazyMediaSource() {
    val path: Path = createTempFile(kind, ext)
    private var _size: Long = 0

    init {
        withFs {
            try {
                sink(path).buffered().use { sink ->
                    initialRawSource?.use { source ->
                        _size = sink.transferFrom(source)
                    }
                }
            } catch (e: Exception) {
                delete(path, mustExist = false)
                throw e
            }
        }
    }

    override val size: Long
        get() = _size

    override fun openRawSource(): RawSource = withFs {
        source(path)
    }

    override fun dispose() = withFs {
        delete(path, mustExist = false)
    }

    override fun toString(): String {
        return "TempFileMediaSource(path=$path)"
    }
}