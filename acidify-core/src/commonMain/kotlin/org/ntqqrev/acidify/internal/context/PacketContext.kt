package org.ntqqrev.acidify.internal.context

import dev.karmakrafts.kompress.Inflater
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.Buffer
import kotlinx.io.IOException
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

internal class PacketContext(client: AbstractClient) : AbstractContext(client) {
    private val host = "msfwifi.3g.qq.com"
    private val port = 8080
    private val selectorManager = SelectorManager(client.coroutineContext)
    private var currentSocket: Socket? = null
    private lateinit var input: ByteReadChannel
    private lateinit var output: ByteWriteChannel
    private val pending = mutableMapOf<Int, CompletableDeferred<SsoResponse>>()
    private val sendPacketMutex = Mutex()
    private val mapQueryMutex = Mutex()
    private var startConnectLoopJob: Job? = null
    private var heartbeatJob: Job? = null
    private val recentPushSequenceCache = RecentPushSequenceCache(2048)

    private class RecentPushSequenceCache(
        private val capacity: Int
    ) {
        private val queue = ArrayDeque<Int>(capacity)
        private val seen = mutableSetOf<Int>()

        fun clear() {
            queue.clear()
            seen.clear()
        }

        fun isDuplicate(seq: Int): Boolean {
            if (!seen.add(seq)) {
                return true
            }
            queue.addLast(seq)
            if (queue.size > capacity) {
                seen.remove(queue.removeFirst())
            }
            return false
        }
    }

    init {
        startConnectLoopJob = client.launch {
            startConnectLoop()
        }
    }

    override suspend fun postOnline() {
        recentPushSequenceCache.clear()
        heartbeatJob = client.launch {
            var aliveCount = 0
            while (isActive) {
                aliveCount++
                try {
                    client.callService(Alive)
                    if (aliveCount % 27 == 0) {
                        client.callService(Heartbeat) // 270s per Heartbeat
                        aliveCount = 0
                    }
                } catch (e: Exception) {
                    logger.w(e) { "心跳包发送失败" }
                }
                delay(10_000L) // 10s
            }
        }
    }

    override suspend fun preOffline() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        recentPushSequenceCache.clear()
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
                } catch (_: ClosedByteChannelException) {
                    break
                } catch (_: kotlinx.coroutines.CancellationException) {
                    break
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
        ssoReservedMsgType: Int,
        timeoutMillis: Long = 10_000L,
        requestType: RequestType = RequestType.D2Auth,
        encryptType: EncryptType = EncryptType.WithD2Key,
        ssoSecureInfo: SsoSecureInfo? = null,
    ): SsoResponse {
        startConnectLoopJob?.join()
        val packet = when (requestType) {
            RequestType.D2Auth -> client.buildProtocol12(
                command = command,
                payload = payload,
                sequence = sequence,
                encryptType = encryptType,
                ssoReservedMsgType = ssoReservedMsgType,
                ssoSecureInfo = ssoSecureInfo,
            )

            RequestType.Simple -> client.buildProtocol13(
                command = command,
                payload = payload,
                sequence = sequence,
                encryptType = encryptType,
                ssoReservedMsgType = ssoReservedMsgType,
                ssoSecureInfo = ssoSecureInfo,
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
            val header = input.readByteArray(4)
            val packetLength = header.readUInt32BE(0)
            val packet = input.readByteArray(packetLength.toInt() - 4)
            val sso = client.parseSsoFrame(packet)
            logger.v { "[seq=${sso.sequence}] <- ${sso.command} (code=${sso.retCode})" }
            mapQueryMutex.withLock { pending.remove(sso.sequence) }.also {
                if (it != null) {
                    it.complete(sso)
                } else {
                    if (recentPushSequenceCache.isDuplicate(sso.sequence)) {
                        logger.v { "忽略重复推送包 [seq=${sso.sequence}] <- ${sso.command}" }
                        return@also
                    }
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

    // Packet building

    private val buildSsoFixedBytes = byteArrayOf(
        0x02, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
    )

    private fun AbstractClient.buildSsoReserved(
        msgType: Int,
        secureInfo: SsoSecureInfo?
    ) = when (this) {
        is LagrangeClient -> SsoReservedFields(
            trace = generateTrace(),
            uid = uid,
            msgType = msgType,
            secureInfo = secureInfo,
        )

        is KuromeClient -> SsoReservedFields(
            trace = generateTrace(),
            uid = uid,
            msgType = msgType,
            secureInfo = secureInfo,
            ntCoreVersion = 100,
        )
    }.pbEncode()

    private fun AbstractClient.buildProtocol12(
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
            when (encryptType) {
                EncryptType.None -> transferFrom(sso)
                EncryptType.WithD2Key -> writeBytes(TEA.encrypt(sso.readByteArray(), d2Key))
                EncryptType.WithEmptyKey -> writeBytes(TEA.encrypt(sso.readByteArray(), ByteArray(16)))
            }
        }
    }

    private fun AbstractClient.buildProtocol13(
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
            when (encryptType) {
                EncryptType.None -> transferFrom(sso)
                EncryptType.WithD2Key -> writeBytes(TEA.encrypt(sso.readByteArray(), d2Key))
                EncryptType.WithEmptyKey -> writeBytes(TEA.encrypt(sso.readByteArray(), ByteArray(16)))
            }
        }
    }

    internal fun AbstractClient.parseSsoFrame(packet: ByteArray): SsoResponse {
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
