package org.ntqqrev.acidify.struct

import kotlin.js.JsExport

/**
 * 用户性别枚举
 */
@JsExport
enum class UserInfoGender(val value: Int) {
    /**
     * 未设置
     */
    UNSET(0),

    /**
     * 男性
     */
    MALE(1),

    /**
     * 女性
     */
    FEMALE(2),

    /**
     * 未知
     */
    UNKNOWN(255);

    companion object {
        fun from(value: Int): UserInfoGender {
            return entries.firstOrNull { it.value == value } ?: UNKNOWN
        }
    }
}