package org.ntqqrev.acidify.struct

import kotlin.js.JsExport

/**
 * 二维码状态
 */
@JsExport
enum class QRCodeState(val value: Byte) {
    /**
     * 未知状态
     */
    UNKNOWN(-1),

    /**
     * 用户已确认
     */
    CONFIRMED(0),

    /**
     * 二维码已过期
     */
    CODE_EXPIRED(17),

    /**
     * 二维码已生成，等待扫描
     */
    WAITING_FOR_SCAN(48),

    /**
     * 二维码已扫描，等待用户确认
     */
    WAITING_FOR_CONFIRMATION(53),

    /**
     * 用户取消登录
     */
    CANCELLED(54);

    companion object {
        internal fun fromByte(value: Byte): QRCodeState = entries.find { it.value == value } ?: UNKNOWN
    }
}