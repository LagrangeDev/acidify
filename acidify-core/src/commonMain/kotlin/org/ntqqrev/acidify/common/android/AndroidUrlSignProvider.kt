package org.ntqqrev.acidify.common.android

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.ntqqrev.acidify.common.SignResult

/**
 * 通过 HTTP 接口进行签名的 [AndroidSignProvider] 实现
 * @param url 签名服务的 URL 地址
 * @param httpProxy 可选的 HTTP 代理地址，例如 `http://127.0.0.1:7890`
 */
class AndroidUrlSignProvider(val url: String, val httpProxy: String? = null) : AndroidSignProvider {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        engine {
            if (!httpProxy.isNullOrEmpty()) {
                proxy = ProxyBuilder.http(httpProxy)
            }
        }
    }

    override suspend fun sign(
        uin: Long,
        cmd: String,
        buffer: ByteArray,
        guid: String,
        seq: Int,
        version: String,
        qua: String
    ): SignResult {
        val data = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(
                AndroidUrlSignRequest(
                    uin = uin,
                    cmd = cmd,
                    buffer = buffer.toHexString(),
                    guid = guid,
                    seq = seq,
                    version = version,
                    qua = qua,
                )
            )
        }.body<AndroidUrlSignResponse>().data
        return SignResult(
            sign = data.sign.hexToByteArray(),
            token = data.token.hexToByteArray(),
            extra = data.extra.hexToByteArray(),
        )
    }
}

@Serializable
private data class AndroidUrlSignRequest(
    val uin: Long,
    val cmd: String,
    val buffer: String,
    val guid: String,
    val seq: Int,
    val version: String,
    val qua: String
)

@Serializable
private data class AndroidUrlSignResponse(
    val data: AndroidUrlSignValue
)

@Serializable
private data class AndroidUrlSignValue(
    val sign: String,
    val token: String,
    val extra: String
)