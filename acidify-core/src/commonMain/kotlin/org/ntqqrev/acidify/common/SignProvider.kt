package org.ntqqrev.acidify.common

/**
 * 签名提供者接口，实现 [sign] 方法以提供签名功能
 */
fun interface SignProvider {
    suspend fun sign(cmd: String, seq: Int, src: ByteArray): SignResult
}