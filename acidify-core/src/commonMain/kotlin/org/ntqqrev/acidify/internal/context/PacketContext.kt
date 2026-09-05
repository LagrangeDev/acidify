package org.ntqqrev.acidify.internal.context

import dev.karmakrafts.kompress.Inflater
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ClosedByteChannelException
import io.ktor.utils.io.ClosedWriteChannelException
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.writePacket
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.readByteArray
import org.ntqqrev.acidify.common.SsoResponse
import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.KuromeClient
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.crypto.tea.TEA
import org.ntqqrev.acidify.internal.proto.system.SsoReservedFields
import org.ntqqrev.acidify.internal.proto.system.SsoSecureInfo
import org.ntqqrev.acidify.internal.service.EncryptType
import org.ntqqrev.acidify.internal.service.RequestType
import org.ntqqrev.acidify.internal.service.system.Alive
import org.ntqqrev.acidify.internal.service.system.Heartbeat
import org.ntqqrev.acidify.internal.util.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

internal class PacketContext(client: AbstractClient) : AbstractContext(client) {
    private val host = "msfwifi.3g.qq.com"
    private val port = 8080
    private val connectRetryInitialDelay = 1_000L.milliseconds
    private val connectRetryMaxDelay = 30_000L.milliseconds
    private val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    private val onlineRequested = atomic(false)
    private val connectionLoopJob = client.launch(start = CoroutineStart.LAZY) { runConnectionLoop() }

    init {
        connectionLoopJob.invokeOnCompletion { cause ->
            connectionState.value = ConnectionState.Closed(IOException("连接管理已停止", cause))
        }
        connectionLoopJob.start()
    }

    override suspend fun postOnline() {
        if (currentCoroutineContext()[SessionBinding] == null) {
            onlineRequested.value = true
        } else if (!onlineRequested.value) {
            return
        }
        val session = awaitSession()
        session.startHeartbeat {
            var aliveCount = 0
            while (isActive) {
                aliveCount++
                try {
                    client.callService(Alive)
                    if (aliveCount % 27 == 0) {
                        client.callService(Heartbeat) // 270s per Heartbeat
                        aliveCount = 0
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.w(e) { "心跳包发送失败" }
                    if (session.isDisconnected) {
                        break
                    }
                }
                delay(10_000L.milliseconds) // 10s
            }
        }
    }

    override suspend fun preOffline() {
        val boundSession = currentCoroutineContext()[SessionBinding]?.session
        // Connection cleanup preserves the intent to restore the online session.
        if (boundSession == null) onlineRequested.value = false
        val session = boundSession ?: (connectionState.value as? ConnectionState.Connected)?.session
        session?.stopHeartbeat()
    }

    private suspend fun runConnectionLoop() {
        val selectorManager = SelectorManager(currentCoroutineContext())
        var retryDelay = connectRetryInitialDelay
        try {
            while (currentCoroutineContext().isActive) {
                try {
                    val socket = aSocket(selectorManager).tcp().connect(host, port) { keepAlive = true }
                    retryDelay = connectRetryInitialDelay
                    logger.d { "已连接到 $host:$port" }
                    runSession(socket)
                } catch (e: Exception) {
                    currentCoroutineContext().ensureActive()
                    logger.w(e) {
                        "连接失败或已断开，将在 ${retryDelay.inWholeMilliseconds}ms 后重新连接"
                    }
                }
                delay(retryDelay)
                retryDelay = (retryDelay * 2).coerceAtMost(connectRetryMaxDelay)
            }
        } finally {
            selectorManager.close()
        }
    }

    private suspend fun runSession(socket: Socket): Nothing = coroutineScope {
        var session: ConnectionSession? = null
        var disconnectCause: Throwable = IOException("连接已断开")
        try {
            val activeSession = ConnectionSession(socket, this)
            session = activeSession
            val connected = ConnectionState.Connected(activeSession)
            if (!connectionState.compareAndSet(ConnectionState.Connecting, connected)) {
                throw CancellationException("连接管理已停止")
            }
            withContext(SessionBinding(activeSession)) {
                launch {
                    try {
                        handleReceiveLoop(activeSession)
                    } catch (e: Exception) {
                        currentCoroutineContext().ensureActive()
                        throw IOException("接收数据包失败", e)
                    }
                }
                if (onlineRequested.value) {
                    client.sendOnlinePacket()
                    client.doPostOnlineLogic()
                    logger.i { "上线包及初始化完成，重连成功" }
                }
                throw activeSession.disconnection.await()
            }
        } catch (e: Exception) {
            disconnectCause = e
            throw e
        } finally {
            try {
                if (session != null) {
                    val reconnecting = connectionState.compareAndSet(
                        ConnectionState.Connected(session), ConnectionState.Connecting
                    )
                    withContext(NonCancellable + SessionBinding(session)) {
                        session.finish(disconnectCause)
                        if (reconnecting) client.doPreOfflineLogic()
                    }
                }
            } finally {
                socket.close()
            }
        }
    }

    suspend fun closeConnection(reconnect: Boolean = false) {
        val error = IOException(if (reconnect) "请求重新连接" else "连接已主动关闭")
        if (reconnect) {
            (connectionState.value as? ConnectionState.Connected)?.session?.disconnect(error)
            return
        }
        onlineRequested.value = false
        val previous = connectionState.getAndUpdate { ConnectionState.Closed(error) }
        try {
            (previous as? ConnectionState.Connected)?.session?.disconnect(error)
        } finally {
            withContext(NonCancellable) { connectionLoopJob.cancelAndJoin() }
        }
    }

    private suspend fun awaitSession(): ConnectionSession {
        currentCoroutineContext()[SessionBinding]?.let { return it.session }
        return when (val state = connectionState.first { it !is ConnectionState.Connecting }) {
            is ConnectionState.Connected -> state.session
            is ConnectionState.Closed -> throw state.cause
            ConnectionState.Connecting -> error("Unreachable connection state")
        }
    }

    suspend fun sendPacket(
        command: String,
        sequence: Int,
        payload: ByteArray,
        ssoReservedMsgType: Int,
        timeoutMillis: Long = 10_000L,
        requestType: RequestType = RequestType.D2Auth,
        encryptType: EncryptType = EncryptType.WithD2Key,
        ssoSecureInfo: SsoSecureInfo? = null,
    ): SsoResponse {
        var session: ConnectionSession? = null
        try {
            return withTimeout(timeoutMillis.milliseconds) {
                val activeSession = awaitSession()
                session = activeSession
                val packet = SsoCodec.encode(
                    parameters = packetParameters(),
                    request = PacketRequest(
                        command = command,
                        sequence = sequence,
                        payload = payload,
                        ssoReservedMsgType = ssoReservedMsgType,
                        requestType = requestType,
                        encryptType = encryptType,
                        ssoSecureInfo = ssoSecureInfo,
                    ),
                )
                activeSession.request(sequence, packet) {
                    logger.v { "[seq=$sequence] -> $command" }
                }
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            if (shouldTriggerReconnect(command, e)) {
                if (session?.disconnect(e) == true) {
                    logger.w(e) { "数据包 $command 发送或响应失败，准备重新连接" }
                }
            }
            throw e
        }
    }

    private fun packetParameters() = PacketParameters(
        uin = client.uin,
        uid = client.uid,
        subAppId = client.subAppId,
        currentVersion = client.currentVersion,
        d2 = client.d2,
        d2Key = client.d2Key,
        a2 = client.a2,
        guid = client.guid,
        ntCoreVersion = when (client) {
            is LagrangeClient -> null
            is KuromeClient -> 100
        },
    )

    private suspend fun handleReceiveLoop(session: ConnectionSession) {
        while (currentCoroutineContext().isActive) {
            val sso = SsoCodec.decode(session.readFrame(), client.d2Key)
            logger.v { "[seq=${sso.sequence}] <- ${sso.command} (code=${sso.retCode})" }
            if (session.pending.complete(sso)) continue
            if (session.recentPushSequences.isDuplicate(sso.sequence)) {
                logger.v { "忽略重复推送包 [seq=${sso.sequence}] <- ${sso.command}" }
                continue
            }
            client.pushChannel.send(sso)
        }
    }

    private fun shouldTriggerReconnect(command: String, error: Throwable): Boolean =
        when (error) {
            is ClosedByteChannelException,
            is ClosedWriteChannelException,
            is IOException -> true

            is TimeoutCancellationException -> command == Alive.cmd || command == Heartbeat.cmd
            else -> false
        }
}

private sealed interface ConnectionState {
    data object Connecting : ConnectionState
    data class Connected(val session: ConnectionSession) : ConnectionState
    data class Closed(val cause: IOException) : ConnectionState
}

// Online initialization and heartbeats must never migrate to a newer connection.
private class SessionBinding(val session: ConnectionSession) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<SessionBinding>
}

private class ConnectionSession(
    private val socket: Socket,
    private val scope: CoroutineScope,
) {
    private val input = socket.openReadChannel()
    private val output = socket.openWriteChannel(autoFlush = true)
    private val writeMutex = Mutex()
    private val heartbeatMutex = Mutex()
    private var heartbeatJob: Job? = null
    private val failure = atomic<Throwable?>(null)
    val disconnection = CompletableDeferred<Throwable>()
    val pending = PendingRequests()
    val recentPushSequences = RecentPushSequenceCache(2048)
    val isDisconnected: Boolean get() = failure.value != null

    suspend fun readFrame(): ByteArray {
        val packetLength = input.readByteArray(4).readUInt32BE(0)
        if (packetLength < 4L || packetLength > Int.MAX_VALUE.toLong()) {
            throw IOException("无效的数据包长度: $packetLength")
        }
        return input.readByteArray(packetLength.toInt() - 4)
    }

    suspend fun request(sequence: Int, packet: Buffer, onSent: () -> Unit): SsoResponse {
        val response = pending.register(sequence)
        try {
            writeMutex.withLock {
                ensureConnected()
                try {
                    output.writePacket(packet)
                } catch (e: Exception) {
                    // A cancelled write may leave a partial frame on the TCP stream.
                    disconnect(e)
                    throw e
                }
            }
            onSent()
            return response.await()
        } finally {
            withContext(NonCancellable) { pending.remove(sequence, response) }
        }
    }

    suspend fun startHeartbeat(block: suspend CoroutineScope.() -> Unit) {
        heartbeatMutex.withLock {
            ensureConnected()
            if (heartbeatJob?.isActive == true) return
            heartbeatJob = scope.launch(SessionBinding(this), block = block)
        }
    }

    suspend fun stopHeartbeat() {
        heartbeatMutex.withLock {
            heartbeatJob?.cancelAndJoin()
            heartbeatJob = null
        }
    }

    fun disconnect(cause: Throwable): Boolean {
        if (!failure.compareAndSet(null, cause)) return false
        try {
            // Socket.close() flushes asynchronously; failed transports must abort pending I/O first.
            try {
                input.cancel(cause)
            } finally {
                try {
                    output.cancel(cause)
                } finally {
                    socket.close()
                }
            }
        } finally {
            disconnection.complete(cause)
        }
        return true
    }

    suspend fun finish(cause: Throwable) {
        try {
            disconnect(cause)
        } finally {
            pending.failAll(IOException("连接已断开", failure.value ?: cause))
            stopHeartbeat()
            withTimeout(5_000L.milliseconds) { socket.socketContext.join() }
        }
    }

    private fun ensureConnected() {
        failure.value?.let { throw IOException("连接已断开", it) }
    }
}

private class PendingRequests {
    private val mutex = Mutex()
    private val requests = mutableMapOf<Int, CompletableDeferred<SsoResponse>>()
    private var failure: Throwable? = null

    suspend fun register(sequence: Int): CompletableDeferred<SsoResponse> = mutex.withLock {
        failure?.let { throw it }
        check(sequence !in requests) { "Duplicate request sequence: $sequence" }
        CompletableDeferred<SsoResponse>().also { requests[sequence] = it }
    }

    suspend fun remove(sequence: Int, response: CompletableDeferred<SsoResponse>) {
        mutex.withLock {
            if (requests[sequence] === response) requests.remove(sequence)
        }
        response.cancel()
    }

    suspend fun complete(response: SsoResponse): Boolean {
        val deferred = mutex.withLock { requests.remove(response.sequence) } ?: return false
        deferred.complete(response)
        return true
    }

    suspend fun failAll(cause: Throwable) {
        val abandoned = mutex.withLock {
            failure = cause
            requests.values.toList().also { requests.clear() }
        }
        abandoned.forEach { it.completeExceptionally(cause) }
    }
}

private class RecentPushSequenceCache(private val capacity: Int) {
    private val queue = ArrayDeque<Int>(capacity)
    private val seen = mutableSetOf<Int>()

    fun isDuplicate(sequence: Int): Boolean {
        if (!seen.add(sequence)) return true
        queue.addLast(sequence)
        if (queue.size > capacity) seen.remove(queue.removeFirst())
        return false
    }
}

private data class PacketParameters(
    val uin: Long,
    val uid: String,
    val subAppId: Int,
    val currentVersion: String,
    val d2: ByteArray,
    val d2Key: ByteArray,
    val a2: ByteArray,
    val guid: ByteArray,
    val ntCoreVersion: Int?,
)

private data class PacketRequest(
    val command: String,
    val sequence: Int,
    val payload: ByteArray,
    val ssoReservedMsgType: Int,
    val requestType: RequestType,
    val encryptType: EncryptType,
    val ssoSecureInfo: SsoSecureInfo?,
)

private object SsoCodec {
    fun encode(parameters: PacketParameters, request: PacketRequest): Buffer = with(request) {
        when (requestType) {
            RequestType.D2Auth -> parameters.buildProtocol12(
                command = command,
                payload = payload,
                sequence = sequence,
                encryptType = encryptType,
                ssoReservedMsgType = ssoReservedMsgType,
                ssoSecureInfo = ssoSecureInfo,
            )

            RequestType.Simple -> parameters.buildProtocol13(
                command = command,
                payload = payload,
                sequence = sequence,
                encryptType = encryptType,
                ssoReservedMsgType = ssoReservedMsgType,
                ssoSecureInfo = ssoSecureInfo,
            )
        }
    }

    private val buildSsoFixedBytes = byteArrayOf(
        0x02, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
    )

    private fun PacketParameters.buildSsoReserved(
        msgType: Int,
        secureInfo: SsoSecureInfo?
    ) = SsoReservedFields(
        trace = generateTrace(),
        uid = uid,
        msgType = msgType,
        secureInfo = secureInfo,
        ntCoreVersion = ntCoreVersion,
    ).pbEncode()

    private fun PacketParameters.buildProtocol12(
        command: String,
        payload: ByteArray,
        sequence: Int,
        encryptType: EncryptType,
        ssoReservedMsgType: Int,
        ssoSecureInfo: SsoSecureInfo?,
    ): Buffer = Buffer().apply {
        barrier(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) {
            writeInt(12)
            writeByte(encryptType.underlying)
            writeBytes(
                when (encryptType) {
                    EncryptType.WithD2Key -> d2
                    else -> ByteArray(0)
                },
                Prefix.UINT_32 or Prefix.INCLUDE_PREFIX
            )
            writeByte(0) // unknown
            writeString(uin.toString(), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
            val sso = Buffer().apply {
                barrier(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) {
                    writeInt(sequence)
                    writeInt(subAppId)
                    writeInt(2052)  // locale id
                    writeBytes(buildSsoFixedBytes)
                    writeBytes(a2, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
                    writeString(command, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
                    writeBytes(ByteArray(0), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) // unknown
                    writeString(guid.toHexString(), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
                    writeBytes(ByteArray(0), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) // unknown
                    writeString(currentVersion, Prefix.UINT_16 or Prefix.INCLUDE_PREFIX)
                    writeBytes(
                        buildSsoReserved(
                            msgType = ssoReservedMsgType,
                            secureInfo = ssoSecureInfo,
                        ),
                        Prefix.UINT_32 or Prefix.INCLUDE_PREFIX
                    )
                }
                writeBytes(payload, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
            }
            writeEncrypted(sso, encryptType, d2Key)
        }
    }

    private fun Sink.writeEncrypted(sso: Buffer, encryptType: EncryptType, d2Key: ByteArray) {
        when (encryptType) {
            EncryptType.None -> transferFrom(sso)
            EncryptType.WithD2Key -> writeBytes(TEA.encrypt(sso.readByteArray(), d2Key))
            EncryptType.WithEmptyKey -> writeBytes(TEA.encrypt(sso.readByteArray(), ByteArray(16)))
        }
    }

    private fun PacketParameters.buildProtocol13(
        command: String,
        payload: ByteArray,
        sequence: Int,
        encryptType: EncryptType,
        ssoReservedMsgType: Int,
        ssoSecureInfo: SsoSecureInfo?,
    ): Buffer = Buffer().apply {
        barrier(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) {
            writeInt(13)
            writeByte(encryptType.underlying)
            writeInt(sequence)
            writeByte(0)
            writeString(uin.toString(), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
            val sso = Buffer().apply {
                barrier(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) {
                    writeString(command, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
                    writeInt(4) // uint 32 with prefix, ByteArray(0)
                    writeBytes(
                        buildSsoReserved(
                            msgType = ssoReservedMsgType,
                            secureInfo = ssoSecureInfo,
                        ),
                        Prefix.UINT_32 or Prefix.INCLUDE_PREFIX
                    )
                }
                writeBytes(payload, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
            }
            writeEncrypted(sso, encryptType, d2Key)
        }
    }

    fun decode(packet: ByteArray, d2Key: ByteArray): SsoResponse {
        val rawReader = packet.reader()
        val protocol = rawReader.readUInt()
        val authFlag = rawReader.readByte()
        rawReader.readByte() // dummy
        rawReader.readPrefixedString(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) // uin
        if (protocol != 12u && protocol != 13u) throw Exception("Unrecognized protocol: $protocol")
        val encrypted = rawReader.readByteArray()
        val decrypted = when (authFlag) {
            EncryptType.None.underlying -> encrypted
            EncryptType.WithD2Key.underlying -> TEA.decrypt(encrypted, d2Key)
            EncryptType.WithEmptyKey.underlying -> TEA.decrypt(encrypted, ByteArray(16))
            else -> throw Exception("Unrecognized auth flag: $authFlag")
        }

        val reader = decrypted.reader()
        reader.readUInt() // headLen
        val sequence = reader.readUInt()
        val retCode = reader.readInt()
        val extra = reader.readPrefixedString(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
        val command = reader.readPrefixedString(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
        reader.readPrefixedBytes(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) // messageCookie
        val isCompressed = reader.readInt() == 1
        reader.readPrefixedBytes(Prefix.UINT_32) // reservedField
        var payload = reader.readPrefixedBytes(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)

        if (isCompressed) {
            payload = Inflater.inflate(payload, raw = false)
        }

        return if (retCode == 0) {
            SsoResponse(retCode, command, payload, sequence.toInt())
        } else {
            SsoResponse(retCode, command, payload, sequence.toInt(), extra)
        }
    }
}
