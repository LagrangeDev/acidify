package org.ntqqrev.acidify.common

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.ntqqrev.acidify.exception.UrlSignException

/**
 * 通过 HTTP 接口进行签名的 [SignProvider] 实现，用于对接 Lagrange V2 Sign API。
 * 要对接普通的 Sign API，请使用 [UrlSignProvider]。
 * @param url 签名服务的 URL 地址
 * @param token 访问签名服务所需的 Token
 * @param uin 访问签名服务所用的 uin（QQ 号）
 * @param guid 当前登录设备的 GUID
 * @param qua 当前使用的 AppInfo 的 QUA 字符串，形如 `V1_LNX_NQ_3.2.**_*****_GW_B`
 * @param httpProxy 可选的 HTTP 代理地址，例如 `http://127.0.0.1:7890`
 */
class LagrangeUrlSignProvider(
    val url: String,
    val token: String,
    val uin: Long,
    val guid: String,
    val qua: String,
    val httpProxy: String? = null,
) : SignProvider {
    private val signUrl = Url(url)
    private val jsonModule = Json {
        ignoreUnknownKeys = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(jsonModule)
        }
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(token, null)
                }
            }
        }
        engine {
            if (!httpProxy.isNullOrEmpty()) {
                proxy = ProxyBuilder.http(httpProxy)
            }
        }
    }

    override suspend fun sign(
        cmd: String,
        seq: Int,
        src: ByteArray
    ): SignResult {
        val resp = client.post {
            url {
                takeFrom(signUrl)
                appendPathSegments("api", "sign", "sec-sign")
            }
            contentType(ContentType.Application.Json)
            setBody(
                LagrangeUrlSignRequest(
                    command = cmd,
                    seq = seq,
                    body = src.toHexString(),
                    uin = uin,
                    guid = guid,
                    qua = qua,
                )
            )
        }
        if (resp.status != HttpStatusCode.OK) {
            throw UrlSignException(resp.status.description, resp.status.value)
        }
        val respBody = resp.body<LagrangeUrlSignResponse>()
        if (respBody.code != 0 || respBody.value == null) {
            throw UrlSignException(respBody.message ?: "", respBody.code)
        }
        val value = respBody.value
        return SignResult(
            sign = value.sign.hexToByteArray(),
            token = value.token.hexToByteArray(),
            extra = value.extra.hexToByteArray(),
        )
    }
}

@Serializable
private class LagrangeUrlSignRequest(
    val command: String,
    val seq: Int,
    val body: String,
    val uin: Long,
    val guid: String,
    val qua: String,
)

@Serializable
private class LagrangeUrlSignResponse(
    val code: Int = 0,
    val message: String? = null,
    val value: LagrangeUrlSignValue? = null,
)

@Serializable
private class LagrangeUrlSignValue(
    @SerialName("sec_sign") val sign: String,
    @SerialName("sec_token") val token: String,
    @SerialName("sec_extra") val extra: String,
)