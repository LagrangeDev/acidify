package org.ntqqrev.acidify.internal.context

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.io.buffered
import kotlinx.io.readTo
import org.ntqqrev.acidify.common.MediaSource
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

    suspend fun uploadFile(
        uKey: String,
        appId: Int,
        source: MediaSource,
        size: Long,
    ): Boolean {
        val chunkCount = ((size + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()
        val sha1StateList = mutableListOf<ByteArray>()
        val sha1Stream = SHA1Stream()
        val scanSource = source.openRawSource().buffered()
        try {
            for (i in 0 until chunkCount) {
                val chunkSize = minOf(CHUNK_SIZE.toLong(), size - i.toLong() * CHUNK_SIZE).toInt()
                val chunkBuffer = ByteArray(chunkSize)
                scanSource.readTo(chunkBuffer)
                sha1Stream.update(chunkBuffer)
                val digest = ByteArray(20)
                if (i != chunkCount - 1) {
                    sha1Stream.hash(digest, false)
                } else {
                    sha1Stream.final(digest)
                }
                sha1StateList.add(digest)
            }
        } finally {
            scanSource.close()
        }

        val uploadSource = source.openRawSource().buffered()
        try {
            for (i in 0 until chunkCount) {
                val chunkStart = i * CHUNK_SIZE
                val chunkLength = minOf(CHUNK_SIZE.toLong(), size - chunkStart.toLong()).toInt()
                val uploadBuffer = ByteArray(chunkLength)
                uploadSource.readTo(uploadBuffer)
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
        } finally {
            uploadSource.close()
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
            if (!response.status.isSuccess()) {
                logger.e { "FlashTransfer 上传块 $start 失败: ${response.status}, ${responseBytes.toHexString()}" }
                return@async false
            }
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