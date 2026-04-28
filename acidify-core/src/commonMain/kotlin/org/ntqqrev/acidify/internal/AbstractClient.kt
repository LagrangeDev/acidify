package org.ntqqrev.acidify.internal

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import org.ntqqrev.acidify.common.SsoResponse
import org.ntqqrev.acidify.exception.ServiceException
import org.ntqqrev.acidify.exception.UrlSignException
import org.ntqqrev.acidify.internal.context.FlashTransferContext
import org.ntqqrev.acidify.internal.context.HighwayContext
import org.ntqqrev.acidify.internal.context.PacketContext
import org.ntqqrev.acidify.internal.context.TicketContext
import org.ntqqrev.acidify.internal.proto.system.SsoSecureInfo
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.logging.Logger
import kotlin.random.Random

internal sealed class AbstractClient(
    val loggerFactory: (Any) -> Logger,
    scope: CoroutineScope
) : CoroutineScope by scope {
    abstract val os: String
    abstract val uin: Long
    abstract val uid: String

    abstract val appId: Int
    abstract val subAppId: Int
    abstract val currentVersion: String
    abstract val appClientVersion: Int

    abstract val d2: ByteArray
    abstract val d2Key: ByteArray
    abstract val a2: ByteArray
    abstract val guid: ByteArray

    val pushChannel = Channel<SsoResponse>(
        capacity = 15,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    var ssoSequence by atomic(Random.nextInt(0x10000, 0x20000))

    val flashTransferContext = FlashTransferContext(this)
    val highwayContext = HighwayContext(this)
    val packetContext = PacketContext(this)
    val ticketContext = TicketContext(this)

    open val contextCollection = listOf(
        packetContext,
        ticketContext,
        highwayContext,
        flashTransferContext,
    )

    protected val logger = loggerFactory(this)

    suspend fun doPostOnlineLogic() {
        contextCollection.forEach {
            it.postOnline()
        }
    }

    suspend fun doPreOfflineLogic() {
        contextCollection.forEach {
            it.preOffline()
        }
    }

    abstract suspend fun getSsoSecureInfo(cmd: String, seq: Int, src: ByteArray): SsoSecureInfo?

    suspend fun <T, R> callService(service: Service<T, R>, payload: T, timeout: Long = 10_000L): R {
        val sequence = ssoSequence++
        val byteArray = service.build(this, payload)
        val resp = packetContext.sendPacket(
            command = service.cmd,
            sequence = sequence,
            payload = byteArray,
            ssoReservedMsgType = service.androidSsoReservedMsgType ?: 32,
            timeoutMillis = timeout,
            requestType = service.ssoRequestType,
            encryptType = service.ssoEncryptType,
            ssoSecureInfo = try {
                getSsoSecureInfo(service.cmd, sequence, byteArray)
            } catch (e: UrlSignException) {
                logger.w { "没有成功获取 ${service.cmd} 的签名，该操作可能会失败: ${e.message}" }
                null
            } catch (e: Exception) {
                logger.w(e) { "获取 ${service.cmd} 的签名时发生未知错误，该操作可能会失败" }
                null
            }
        )
        if (resp.retCode != 0) {
            throw ServiceException(
                service.cmd,
                resp.retCode,
                resp.extra ?: ""
            )
        }
        return service.parse(this, resp.response)
    }

    suspend fun <R> callService(service: Service<Unit, R>, timeout: Long = 10_000L): R =
        callService(service, Unit, timeout)

    abstract suspend fun sendOnlinePacket()
}