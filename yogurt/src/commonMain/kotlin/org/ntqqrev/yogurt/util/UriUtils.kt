package org.ntqqrev.yogurt.util

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import org.ntqqrev.yogurt.fs.withFs
import kotlin.io.encoding.Base64

val httpClient = HttpClient()

suspend fun resolveUri(uri: String): ByteArray = withContext(Dispatchers.IO) {
    when {
        uri.startsWith("file://") -> withFs {
            val filePath = uri.removePrefix("file://")
            Path(filePath.decodeURLPart()).readBytes()
        }

        uri.startsWith("http://") || uri.startsWith("https://") -> {
            httpClient.get(uri).readRawBytes()
        }

        uri.startsWith("base64://") -> {
            val base64Data = uri.removePrefix("base64://")
            Base64.decode(base64Data)
        }

        else -> throw IllegalArgumentException("Unsupported URI scheme: $uri")
    }
}