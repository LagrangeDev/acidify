package org.ntqqrev.acidify.internal.logic

import co.touchlab.stately.collections.ConcurrentMutableMap
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
import kotlinx.io.readByteArray
import org.ntqqrev.acidify.common.SignProvider
import org.ntqqrev.acidify.crypto.tea.TeaProvider
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.packet.SsoResponse
import org.ntqqrev.acidify.internal.packet.system.SsoReservedFields
import org.ntqqrev.acidify.internal.packet.system.SsoSecureInfo
import org.ntqqrev.acidify.internal.service.system.BotOnline
import org.ntqqrev.acidify.internal.util.*
import org.ntqqrev.acidify.pb.PbObject
import org.ntqqrev.acidify.pb.invoke
import kotlin.random.Random

internal class PacketLogic(client: LagrangeClient) : AbstractLogic(client) {
    private var sequence = Random.nextInt(0x10000, 0x20000)
    private val host = "msfwifi.3g.qq.com"
    private val port = 8080

    private val selectorManager = SelectorManager(client.coroutineContext)
    private var currentSocket: Socket? = null
    private lateinit var input: ByteReadChannel
    private lateinit var output: ByteWriteChannel
    private val pending = ConcurrentMutableMap<Int, CompletableDeferred<SsoResponse>>()
    private val headerLength = 4
    private val sendPacketMutex = Mutex()
    val signRequiredCommand = setOf(
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

    private val logger = client.createLogger(this)

    fun startConnectLoop() {
        runBlocking { connect() }
        client.launch {
            var isReconnect = false
            while (currentCoroutineContext().isActive) {
                try {
                    if (isReconnect) {
                        client.launch(CoroutineExceptionHandler { _, t ->
                            logger.e(t) { "发送上线包时出现错误" }
                        }) {
                            client.callService(BotOnline)
                            logger.i { "上线包发送成功，重连完成" }
                        }
                    }
                    handleReceiveLoop()
                } catch (e: Exception) {
                    logger.e(e) { "接收数据包时出现错误，5s 后尝试重新连接" }
                    cleanupPendingRequests(e)
                    closeConnection()
                    delay(5000)
                    isReconnect = true
                    connect()
                }
            }
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
        pending[sequence] = deferred

        sendPacketMutex.withLock {
            output.writePacket(service)
        }
        logger.v { "[seq=$sequence] -> $cmd" }

        return try {
            withTimeout(timeoutMillis) {
                deferred.await()
            }
        } catch (e: Exception) {
            pending.remove(sequence)
            throw e
        }
    }

    private suspend fun handleReceiveLoop() {
        while (true) {
            val header = input.readByteArray(headerLength)
            val packetLength = header.readUInt32BE(0)
            val packet = input.readByteArray(packetLength.toInt() - 4)
            val service = parseService(packet)
            val sso = parseSso(service)
            logger.v { "[seq=${sso.sequence}] <- ${sso.command} (code=${sso.retCode})" }
            pending.remove(sso.sequence).also {
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
        val result: SignProvider.Result? = if (signRequiredCommand.contains(command)) {
            client.signProvider.sign(command, sequence, payload)
        } else null

        return SsoReservedFields {
            it[trace] = generateTrace()
            it[uid] = client.sessionStore.uid
            it[secureInfo] = result?.toSsoSecureInfo()
        }.toByteArray()
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

    private fun SignProvider.Result.toSsoSecureInfo(): PbObject<SsoSecureInfo> {
        return SsoSecureInfo {
            it[sign] = this@toSsoSecureInfo.sign
            it[token] = this@toSsoSecureInfo.token
            it[extra] = this@toSsoSecureInfo.extra
        }
    }

    private fun cleanupPendingRequests(error: Throwable) {
        val pendingCount = pending.size
        if (pendingCount > 0) {
            logger.w { "清理 $pendingCount 个待处理的请求" }
            pending.forEach { (seq, deferred) ->
                deferred.completeExceptionally(
                    Exception("连接已断开: ${error.message}", error)
                )
            }
            pending.clear()
        }
    }

    private suspend fun closeConnection() {
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
}