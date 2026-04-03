package org.ntqqrev.yogurt.util

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.files.Path
import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.yogurt.fs.withFs
import kotlin.io.encoding.Base64

val httpClient = HttpClient()

suspend fun resolveUri(uri: String): MediaSource = withContext(Dispatchers.IO) {
    when {
        uri.startsWith("file://") -> withFs {
            val filePath = Path(uri.removePrefix("file://").decodeURLPart())
            if (!exists(filePath)) {
                throw IOException("File not found: $filePath")
            }
            MediaSource(
                size = metadataOrNull(filePath)!!.size,
                opener = { source(filePath) }
            )
        }

        uri.startsWith("http://") || uri.startsWith("https://") -> {
            // TODO: prepare content into temporary file
            MediaSource.fromByteArray(httpClient.get(uri).readRawBytes())
        }

        uri.startsWith("base64://") -> {
            val base64Data = uri.removePrefix("base64://")
            MediaSource.fromByteArray(Base64.decode(base64Data))
        }

        else -> throw IllegalArgumentException("Unsupported URI scheme: $uri")
    }
}