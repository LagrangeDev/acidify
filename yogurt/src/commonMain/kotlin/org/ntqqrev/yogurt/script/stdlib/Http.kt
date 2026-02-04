package org.ntqqrev.yogurt.script.stdlib

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.JsObject
import com.dokar.quickjs.binding.define
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText

class HttpRequestOptions(
    val method: String,
    val headers: Map<String, String>,
    val body: ByteArray?,
) {
    companion object {
        fun fromJsObject(obj: JsObject?): HttpRequestOptions {
            if (obj == null) {
                return HttpRequestOptions("GET", emptyMap(), null)
            }

            val method = (obj["method"] as? String)?.uppercase() ?: "GET"
            val headers = (obj["headers"] as? JsObject)?.entries?.associate {
                val key = it.key
                val value = it.value as? String
                    ?: throw IllegalArgumentException("Header values must be strings")
                key to value
            } ?: emptyMap()
            val body = if ("body" in obj) {
                val bodyValue = obj["body"]
                when (bodyValue) {
                    is ByteArray -> bodyValue
                    is String -> bodyValue.encodeToByteArray()
                    else -> throw IllegalArgumentException("Body must be a string or Uint8Array")
                }
            } else {
                null
            }
            return HttpRequestOptions(method, headers, body)
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun QuickJs.defineHttp() = define("http") {
    val httpClient = HttpClient()

    fun Array<Any?>.extractParams(): Pair<String, HttpRequestOptions> {
        require(this.size in 1..2) {
            "Expected arguments: (url: String, options?: { method?: string, headers?: Record<string, string>, body?: string | Uint8Array })"
        }
        val url = this[0] as? String
            ?: throw IllegalArgumentException("Expected first argument to be a String (url)")
        val options = HttpRequestOptions.fromJsObject(this.getOrNull(1) as? JsObject)
        return url to options
    }

    suspend fun sendRequest(url: String, options: HttpRequestOptions): HttpResponse {
        return when (options.method) {
            "GET" -> httpClient.get(url) {
                options.headers.forEach { (key, value) ->
                    header(key, value)
                }
            }

            "POST" -> httpClient.post(url) {
                options.headers.forEach { (key, value) ->
                    header(key, value)
                }
                if (options.body != null) {
                    setBody(options.body)
                }
            }

            else -> throw IllegalArgumentException("Unsupported HTTP method: ${options.method}")
        }
    }

    asyncFunction("request") { args ->
        val (url, options) = args.extractParams()
        sendRequest(url, options).bodyAsText()
    }

    asyncFunction("requestBytes") { args ->
        val (url, options) = args.extractParams()
        sendRequest(url, options).bodyAsBytes()
    }
}