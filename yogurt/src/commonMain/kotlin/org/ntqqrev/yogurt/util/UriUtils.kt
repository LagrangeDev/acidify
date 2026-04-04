package org.ntqqrev.yogurt.util

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.acidify.common.MediaSource.Companion.toMediaSource
import org.ntqqrev.yogurt.fs.withFs
import kotlin.io.encoding.Base64

val httpClient = HttpClient()

class LocalFileMediaSource(val path: Path) : MediaSource() {
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
}

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
            // TODO: prepare content into temporary file
            httpClient.get(uri).readRawBytes().toMediaSource()
        }

        uri.startsWith("base64://") -> {
            val base64Data = uri.removePrefix("base64://")
            Base64.decode(base64Data).toMediaSource()
        }

        else -> throw IllegalArgumentException("Unsupported URI scheme: $uri")
    }
}