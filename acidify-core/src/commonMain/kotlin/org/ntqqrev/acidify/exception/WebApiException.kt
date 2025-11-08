package org.ntqqrev.acidify.exception

import io.ktor.http.HttpStatusCode
import kotlin.js.JsExport

/**
 * Web API 调用异常
 * @property statusCode HTTP 状态码
 */
@JsExport
class WebApiException(
    msg: String,
    val statusCode: HttpStatusCode
) : Exception("$msg ($statusCode)")