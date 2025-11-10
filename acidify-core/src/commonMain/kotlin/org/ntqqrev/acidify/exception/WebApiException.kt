package org.ntqqrev.acidify.exception

import kotlin.js.JsExport

/**
 * Web API 调用异常
 * @property statusCode HTTP 状态码
 */
@JsExport
class WebApiException(
    msg: String,
    val statusCode: Int
) : Exception("$msg ($statusCode)")