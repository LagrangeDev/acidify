package org.ntqqrev.acidify.exception

import kotlin.js.JsExport

/**
 * WtLogin 异常，表示用户扫码登录失败
 * @property code 错误码
 * @property tag 错误标签
 * @property msg 错误信息
 */
@JsExport
class WtLoginException(
    val code: Int,
    val tag: String,
    val msg: String
) : Exception("WtLogin failed with code=$code ($tag: $msg)")