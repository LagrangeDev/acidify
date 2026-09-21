package org.ntqqrev.acidify.common

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.ntqqrev.acidify.exception.UrlSignException

/**
 * 通过 HTTP 接口进行签名的 [SignProvider] 实现
 * @param url 签名服务的 URL 地址
 * @param httpProxy 可选的 HTTP 代理地址，例如 `http://127.0.0.1:7890`
 */
@Deprecated("请使用 LagrangeUrlSignProvider 以对接 Lagrange V2 Sign API")
class UrlSignProvider(val url: String, val httpProxy: String? = null) : SignProvider {
    private val signUrl = Url(url)
    private val jsonModule = Json {
        ignoreUnknownKeys = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(jsonModule)
        }
        engine {
            if (!httpProxy.isNullOrEmpty()) {
                proxy = ProxyBuilder.http(httpProxy)
            }
        }
    }

    override suspend fun sign(cmd: String, seq: Int, src: ByteArray): SignResult? {
        if (!linuxCommandWhitelist.contains(cmd)) {
            return null
        }
        val resp = client.post {
            url(signUrl)
            contentType(ContentType.Application.Json)
            setBody(UrlSignRequest(cmd, seq, src.toHexString()))
        }
        if (resp.status != HttpStatusCode.OK) {
            throw UrlSignException(resp.status.description, resp.status.value)
        }
        val value = jsonModule.decodeFromString<UrlSignResponse>(resp.bodyAsText()).value
        return SignResult(
            sign = value.sign.hexToByteArray(),
            token = value.token.hexToByteArray(),
            extra = value.extra.hexToByteArray(),
        )
    }

    /**
     * 通过 Lagrange 的签名服务提供的额外的 `/appinfo` 接口获取 [AppInfo]，若未提供则返回 `null`
     */
    suspend fun getAppInfo(): AppInfo? {
        val response = client.get {
            url {
                takeFrom(signUrl)
                appendPathSegments("appinfo")
            }
        }
        return if (response.status == HttpStatusCode.OK) {
            AppInfo.fromJson(response.bodyAsText())
        } else {
            null
        }
    }
}

@Serializable
private data class UrlSignRequest(
    val cmd: String,
    val seq: Int,
    val src: String
)

@Serializable
private data class UrlSignResponse(
    val platform: String,
    val version: String,
    val value: UrlSignValue
)

@Serializable
private data class UrlSignValue(
    val sign: String,
    val token: String,
    val extra: String
)