package org.ntqqrev.acidify.exception

import kotlin.js.JsExport

/**
 * Oidb 服务调用异常
 * @property oidbCommand Oidb 命令
 * @property oidbService Oidb 服务（子命令）
 * @property oidbResult Oidb 返回码
 * @property oidbErrorMsg Oidb 错误信息
 */
@JsExport
class OidbException(
    val oidbCommand: Int,
    val oidbService: Int,
    val oidbResult: Int,
    val oidbErrorMsg: String
) : Exception("Oidb(cmd=${oidbCommand.toString(16)}, svc=${oidbService}) failed with $oidbResult: $oidbErrorMsg")