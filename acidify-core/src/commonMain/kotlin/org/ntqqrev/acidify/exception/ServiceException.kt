package org.ntqqrev.acidify.exception

import kotlin.js.JsExport

/**
 * 服务调用异常
 * @property cmd 服务的 `cmd`
 * @property retCode 错误码，为非 0 值
 * @property extra 额外信息
 */
@JsExport
class ServiceException(
    val cmd: String,
    val retCode: Int,
    val extra: String
) : Exception("Service ($cmd) call failed with code $retCode: $extra")