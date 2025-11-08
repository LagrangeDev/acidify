package org.ntqqrev.acidify.struct

import kotlin.js.JsExport

/**
 * 请求状态枚举
 */
@JsExport
enum class RequestState(val value: Int) {
    /**
     * 默认 / 未知状态
     */
    DEFAULT(0),

    /**
     * 待处理
     */
    PENDING(1),

    /**
     * 已被接受
     */
    ACCEPTED(2),

    /**
     * 已被拒绝
     */
    REJECTED(3);

    companion object {
        fun from(value: Int): RequestState {
            return entries.firstOrNull { it.value == value } ?: DEFAULT
        }
    }
}