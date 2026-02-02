package org.ntqqrev.acidify.internal.context

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import korlibs.io.compression.deflate.ZLib
import korlibs.io.compression.uncompress
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.readByteArray
import org.ntqqrev.acidify.common.SignResult
import org.ntqqrev.acidify.common.SsoResponse
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.crypto.tea.TeaProvider
import org.ntqqrev.acidify.internal.proto.system.SsoReservedFields
import org.ntqqrev.acidify.internal.proto.system.SsoSecureInfo
import org.ntqqrev.acidify.internal.service.system.BotOnline
import org.ntqqrev.acidify.internal.service.system.Heartbeat
import org.ntqqrev.acidify.internal.util.*
import kotlin.random.Random

internal class PacketContext(client: LagrangeClient) : AbstractContext(client) {
    private var sequence = Random.nextInt(0x10000, 0x20000)
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
    private val signRequiredCommand = setOf(
        "MessageSvc.PbSendMsg",
        "wtlogin.trans_emp",
        "wtlogin.login",
        "trpc.login.ecdh.EcdhService.SsoKeyExchange",
        "trpc.login.ecdh.EcdhService.SsoNTLoginPasswordLogin",
        "trpc.login.ecdh.EcdhService.SsoNTLoginEasyLogin",
        "trpc.login.ecdh.EcdhService.SsoNTLoginPasswordLoginNewDevice",
        "trpc.login.ecdh.EcdhService.SsoNTLoginEasyLoginUnusualDevice",
        "trpc.login.ecdh.EcdhService.SsoNTLoginPasswordLoginUnusualDevice",
        "OidbSvcTrpcTcp.0x6d9_4"
    )
    private var heartbeatJob: Job? = null
    private val logger = client.createLogger(this)

    override suspend fun postOnline() {
        heartbeatJob = client.launch {
            while (isActive) {
                try {
                    client.callService(Heartbeat)
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
                            client.callService(BotOnline)
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

    suspend fun sendPacket(cmd: String, payload: ByteArray, timeoutMillis: Long): SsoResponse {
        val sequence = this.sequence++
        val sso = buildSso(cmd, payload, sequence)
        val service = buildService(sso)

        val deferred = CompletableDeferred<SsoResponse>()
        mapQueryMutex.withLock { pending[sequence] = deferred }

        sendPacketMutex.withLock {
            output.writePacket(service)
        }
        logger.v { "[seq=$sequence] -> $cmd" }

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
            val service = parseService(packet)
            val sso = parseSso(service)
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

    private fun buildService(sso: ByteArray): Buffer {
        val packet = Buffer()

        packet.barrier(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) {
            writeInt(12)
            writeByte(if (client.sessionStore.d2.isEmpty()) 2 else 1)
            writeBytes(client.sessionStore.d2, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
            writeByte(0) // unknown
            writeString(client.sessionStore.uin.toString(), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
            writeBytes(TeaProvider.encrypt(sso, client.sessionStore.d2Key))
        }

        return packet
    }

    val buildSsoFixedBytes = byteArrayOf(
        0x02, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
    )

    private suspend fun buildSso(command: String, payload: ByteArray, sequence: Int): ByteArray {
        val packet = Buffer()
        val ssoReserved = buildSsoReserved(command, payload, sequence)

        packet.barrier(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) {
            writeInt(sequence)
            writeInt(client.appInfo.subAppId)
            writeInt(2052)  // locale id
            writeFully(buildSsoFixedBytes)
            writeBytes(client.sessionStore.a2, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
            writeString(command, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
            writeBytes(ByteArray(0), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) // unknown
            writeString(client.sessionStore.guid.toHexString(), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
            writeBytes(ByteArray(0), Prefix.UINT_32 or Prefix.INCLUDE_PREFIX) // unknown
            writeString(client.appInfo.currentVersion, Prefix.UINT_16 or Prefix.INCLUDE_PREFIX)
            writeBytes(ssoReserved, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)
        }

        packet.writeBytes(payload, Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)

        return packet.readByteArray()
    }

    private suspend fun buildSsoReserved(command: String, payload: ByteArray, sequence: Int): ByteArray {
        val result: SignResult? = if (signRequiredCommand.contains(command)) {
            client.signProvider.sign(command, sequence, payload)
        } else null

        return SsoReservedFields(
            trace = generateTrace(),
            uid = client.sessionStore.uid,
            secureInfo = result?.toSsoSecureInfo(),
        ).pbEncode()
    }

    private fun parseSso(packet: ByteArray): SsoResponse {
        val reader = packet.reader()
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
            payload = ZLib.uncompress(payload)
        }

        return if (retCode == 0) {
            SsoResponse(retCode, command, payload, sequence.toInt())
        } else {
            SsoResponse(retCode, command, payload, sequence.toInt(), extra)
        }
    }

    private fun parseService(raw: ByteArray): ByteArray {
        val reader = raw.reader()

        val protocol = reader.readUInt()
        val authFlag = reader.readByte()
        /* val flag = */ reader.readByte()
        /* val uin = */ reader.readPrefixedString(Prefix.UINT_32 or Prefix.INCLUDE_PREFIX)

        if (protocol != 12u && protocol != 13u) throw Exception("Unrecognized protocol: $protocol")

        val encrypted = reader.readByteArray()
        return when (authFlag) {
            0.toByte() -> encrypted
            1.toByte() -> TeaProvider.decrypt(encrypted, client.sessionStore.d2Key)
            2.toByte() -> TeaProvider.decrypt(encrypted, ByteArray(16))
            else -> throw Exception("Unrecognized auth flag: $authFlag")
        }
    }

    private fun SignResult.toSsoSecureInfo(): SsoSecureInfo = SsoSecureInfo(sign, token, extra)

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