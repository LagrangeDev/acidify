package org.ntqqrev.acidify.internal.context

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import org.ntqqrev.acidify.common.SsoResponse
import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.packet.*
import org.ntqqrev.acidify.internal.proto.system.SsoSecureInfo
import org.ntqqrev.acidify.internal.util.readUInt32BE

internal class PacketContext(client: AbstractClient) : AbstractContext(client) {
    private val host = "msfwifi.3g.qq.com"
    private val port = 8080
    private val selectorManager = SelectorManager(client.coroutineContext)
    private var currentSocket: Socket? = null
    private lateinit var input: ByteReadChannel
    private lateinit var output: ByteWriteChannel
    private val pending = mutableMapOf<Int, CompletableDeferred<SsoResponse>>()
    private val headerLength = 4
    private val sendPacketMutex = Mutex()
    private val mapQueryMutex = Mutex()
    private var heartbeatJob: Job? = null

    override suspend fun postOnline() {
        heartbeatJob = client.launch {
            while (isActive) {
                try {
                    client.sendHeartbeat()
                } catch (e: Exception) {
                    logger.w(e) { "心跳包发送失败" }
                }
                delay(270_000L) // 4.5min
            }
        }
    }

    override suspend fun preOffline() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    suspend fun startConnectLoop() {
        connect()
        client.launch {
            var isReconnect = false
            while (isActive) {
                try {
                    if (isReconnect) {
                        client.launch(CoroutineExceptionHandler { _, t ->
                            logger.e(t) { "发送上线包时出现错误" }
                        }) {
                            client.sendOnlinePacket()
                            logger.i { "上线包发送成功，重连完成" }
                            client.doPostOnlineLogic()
                        }
                    }
                    handleReceiveLoop()
                } catch (e: Exception) {
                    logger.e(e) { "接收数据包时出现错误，5s 后尝试重新连接" }
                    cleanupPendingRequests(e)
                    client.doPreOfflineLogic()
                    closeConnection()
                    delay(5000)
                    isReconnect = true
                    connect()
                }
            }
        }
    }

    suspend fun closeConnection() {
        try {
            input.cancel()
            output.flushAndClose()
            currentSocket?.close()
            currentSocket = null
            logger.d { "已关闭连接" }
        } catch (e: Exception) {
            logger.w(e) { "关闭连接时出现错误" }
        }
    }

    private suspend fun connect() {
        val newSocket = aSocket(selectorManager).tcp().connect(host, port) {
            keepAlive = true
        }
        currentSocket = newSocket
        input = newSocket.openReadChannel()
        output = newSocket.openWriteChannel(autoFlush = true)
        logger.d { "已连接到 $host:$port" }
    }

    suspend inline fun sendPacket(
        command: String,
        sequence: Int,
        payload: ByteArray,
        requestType: RequestType,
        encryptType: EncryptType,
        timeoutMillis: Long,
        ssoSecureInfo: SsoSecureInfo?
    ): SsoResponse {
        val packet = when (requestType) {
            RequestType.D2Auth -> client.buildProtocol12(
                command = command,
                payload = payload,
                sequence = sequence,
                ssoSecureInfo = ssoSecureInfo,
                encryptType = encryptType,
            )

            RequestType.Simple -> client.buildProtocol13(
                command = command,
                payload = payload,
                sequence = sequence,
                ssoSecureInfo = ssoSecureInfo,
                encryptType = encryptType,
            )
        }
        val deferred = CompletableDeferred<SsoResponse>()
        mapQueryMutex.withLock { pending[sequence] = deferred }
        sendPacketMutex.withLock { output.writePacket(packet) }
        logger.v { "[seq=$sequence] -> $command" }
        return try {
            withTimeout(timeoutMillis) { deferred.await() }
        } catch (e: Exception) {
            mapQueryMutex.withLock { pending.remove(sequence) }
            throw e
        }
    }

    private suspend fun handleReceiveLoop() {
        while (currentCoroutineContext().isActive) {
            val header = input.readByteArray(headerLength)
            val packetLength = header.readUInt32BE(0)
            val packet = input.readByteArray(packetLength.toInt() - 4)
            val sso = client.parseSsoFrame(packet)
            logger.v { "[seq=${sso.sequence}] <- ${sso.command} (code=${sso.retCode})" }
            mapQueryMutex.withLock { pending.remove(sso.sequence) }.also {
                if (it != null) {
                    it.complete(sso)
                } else {
                    client.pushChannel.send(sso)
                }
            }
        }
    }

    private suspend fun cleanupPendingRequests(error: Throwable) {
        val pendingCount = mapQueryMutex.withLock { pending.size }
        if (pendingCount > 0) {
            logger.w { "清理 $pendingCount 个待处理的请求" }
            mapQueryMutex.withLock {
                pending.forEach { (_, deferred) ->
                    deferred.completeExceptionally(
                        IOException("连接已断开: ${error.message}", error)
                    )
                }
                pending.clear()
            }
        }
    }
}