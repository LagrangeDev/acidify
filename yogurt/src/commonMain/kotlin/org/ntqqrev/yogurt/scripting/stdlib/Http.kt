package org.ntqqrev.yogurt.scripting.stdlib

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.JsObject
import com.dokar.quickjs.binding.define
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

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
                    is String -> bodyValue.encodeToByteArray()
                    else -> bodyValue.toByteArrayArg("body")
                }
            } else {
                null
            }
            return HttpRequestOptions(method, headers, body)
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun QuickJs.defineStdlibHttp() = define("http") {
    val httpClient = HttpClient()

    fun Array<Any?>.extractParams(): Pair<String, HttpRequestOptions> {
        requireSize(1..2, "(url: String, options?: { method?: string, headers?: Record<string, string>, body?: string | Uint8Array })")
        val url = stringArg(0, "url")
        val options = HttpRequestOptions.fromJsObject(jsObjectArg(1, "options"))
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
