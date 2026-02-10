package org.ntqqrev.acidify.internal.context

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.async
import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.crypto.hash.SHA1Stream
import org.ntqqrev.acidify.internal.proto.message.media.FlashTransferSha1StateV
import org.ntqqrev.acidify.internal.proto.message.media.FlashTransferUploadBody
import org.ntqqrev.acidify.internal.proto.message.media.FlashTransferUploadReq
import org.ntqqrev.acidify.internal.proto.message.media.FlashTransferUploadResp
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.internal.util.sha1

internal class FlashTransferContext(client: AbstractClient) : AbstractContext(client) {
    private val httpClient = HttpClient()
    private val url = "https://multimedia.qfile.qq.com/sliceupload"

    companion object {
        const val CHUNK_SIZE = 1024 * 1024 // 1MB
    }

    suspend fun uploadFile(uKey: String, appId: Int, bodyStream: ByteArray): Boolean {
        val chunkCount = (bodyStream.size + CHUNK_SIZE - 1) / CHUNK_SIZE
        val sha1StateList = mutableListOf<ByteArray>()
        val sha1Stream = SHA1Stream()
        for (i in 0 until chunkCount) {
            if (i != chunkCount - 1) {
                val accLength = (i + 1) * CHUNK_SIZE
                val accBuffer = bodyStream.copyOfRange(0, accLength)
                val digest = ByteArray(20)
                sha1Stream.update(accBuffer)
                sha1Stream.hash(digest, false)
                sha1Stream.reset()
                sha1StateList.add(digest)
            } else {
                val digest = bodyStream.sha1()
                sha1StateList.add(digest)
            }
        }
        for (i in 0 until chunkCount) {
            val chunkStart = i * CHUNK_SIZE
            val chunkLength = minOf(CHUNK_SIZE, bodyStream.size - chunkStart)
            val uploadBuffer = bodyStream.copyOfRange(chunkStart, chunkStart + chunkLength)
            val success = uploadChunk(
                uKey = uKey,
                appId = appId,
                start = chunkStart,
                sha1StateList = sha1StateList,
                body = uploadBuffer
            )
            if (!success) {
                return false
            }
        }
        return true
    }

    private suspend fun uploadChunk(
        uKey: String,
        appId: Int,
        start: Int,
        sha1StateList: List<ByteArray>,
        body: ByteArray
    ): Boolean = client.async {
        val chunkSha1 = body.sha1()
        val end = start + body.size - 1
        val req = FlashTransferUploadReq(
            field1 = 0,
            appId = appId,
            field3 = 2,
            body = FlashTransferUploadBody(
                field1 = ByteArray(0),
                uKey = uKey,
                start = start,
                end = end,
                sha1 = chunkSha1,
                sha1StateV = FlashTransferSha1StateV(
                    state = sha1StateList
                ),
                body = body,
            )
        )
        val payload = req.pbEncode()
        try {
            val response = httpClient.post(url) {
                headers {
                    append(HttpHeaders.Accept, "*/*")
                    append(HttpHeaders.Expect, "100-continue")
                    append(HttpHeaders.Connection, "Keep-Alive")
                    append(HttpHeaders.AcceptEncoding, "gzip")
                }
                setBody(payload)
            }
            val responseBytes = response.readRawBytes()
            val resp = responseBytes.pbDecode<FlashTransferUploadResp>()
            val status = resp.status
            if (status != "success") {
                logger.e { "FlashTransfer 上传块 $start 失败: $status" }
                return@async false
            }
            logger.d { "FlashTransfer 上传块 $start 成功" }
            true
        } catch (e: Exception) {
            logger.e(e) { "FlashTransfer 上传块 $start 异常: ${e.message}" }
            false
        }
    }.await()
}