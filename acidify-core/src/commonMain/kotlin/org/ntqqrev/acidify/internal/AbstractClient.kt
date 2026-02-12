package org.ntqqrev.acidify.internal

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import org.ntqqrev.acidify.common.SsoResponse
import org.ntqqrev.acidify.internal.context.FlashTransferContext
import org.ntqqrev.acidify.internal.context.HighwayContext
import org.ntqqrev.acidify.internal.context.PacketContext
import org.ntqqrev.acidify.internal.context.TicketContext
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

    val contextCollection = listOf(
        packetContext,
        ticketContext,
        highwayContext,
        flashTransferContext,
    )

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

    abstract suspend fun <T, R> callService(service: Service<T, R>, payload: T, timeout: Long = 10_000L): R

    suspend fun <R> callService(service: Service<Unit, R>, timeout: Long = 10_000L): R =
        callService(service, Unit, timeout)

    abstract suspend fun sendOnlinePacket()
}