package org.ntqqrev.acidify.common.android

import org.ntqqrev.acidify.common.SignResult

/**
 * Android 签名提供者接口，实现 [sign] 方法以提供签名功能
 */
fun interface AndroidSignProvider {
    suspend fun sign(
        uin: Long,
        cmd: String,
        buffer: ByteArray,
        guid: String,
        seq: Int,
        version: String,
        qua: String,
    ): SignResult
}