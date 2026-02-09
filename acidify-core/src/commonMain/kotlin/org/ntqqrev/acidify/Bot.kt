package org.ntqqrev.acidify

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import org.ntqqrev.acidify.common.*
import org.ntqqrev.acidify.entity.BotFriend
import org.ntqqrev.acidify.entity.BotGroup
import org.ntqqrev.acidify.event.AcidifyEvent
import org.ntqqrev.acidify.event.internal.KickSignal
import org.ntqqrev.acidify.event.internal.MsgPushSignal
import org.ntqqrev.acidify.internal.CacheUtility
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.packet.EncryptType
import org.ntqqrev.acidify.internal.packet.RequestType
import org.ntqqrev.acidify.internal.proto.system.SsoSecureInfo
import org.ntqqrev.acidify.logging.*
import org.ntqqrev.acidify.struct.BotFaceDetail
import kotlin.js.JsName
import kotlin.js.JsStatic

/**
 * Acidify Bot 实例
 */
class Bot internal constructor(
    val appInfo: AppInfo,
    val sessionStore: SessionStore,
    signProvider: SignProvider,
    scope: CoroutineScope,
) : CoroutineScope by scope {
    internal val logger = this.createLogger(this)
    internal val client = LagrangeClient(appInfo, sessionStore, signProvider, this::createLogger, scope)
    internal val sharedEventFlow = MutableSharedFlow<AcidifyEvent>(
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    internal val sharedLogFlow = MutableSharedFlow<LogMessage>(
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    internal val signals = listOf(
        MsgPushSignal,
        KickSignal
    ).associateBy { it.cmd }
    internal val faceDetailMapMut = mutableMapOf<String, BotFaceDetail>()
    internal var eventCollectJob: Job? = null

    internal val friendCache = CacheUtility(
        bot = this,
        updateCache = { bot -> bot.fetchFriends().associateBy { it.uin } },
        entityFactory = ::BotFriend
    )

    internal val groupCache = CacheUtility(
        bot = this,
        updateCache = { bot -> bot.fetchGroups().associateBy { it.uin } },
        entityFactory = ::BotGroup
    )

    internal val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    internal val uin2uidMap = mutableMapOf<Long, String>()
    internal val uid2uinMap = mutableMapOf<String, Long>()
    internal val idMapQueryMutex = Mutex()

    /**
     * [AcidifyEvent] 流，可用于监听各种事件
     *
     * 示例：
     * ```
     * bot.eventFlow.collect { event ->
     *     when (event) {
     *         is QRCodeGeneratedEvent -> {
     *             println("QR Code URL: ${event.url}")
     *         }
     *     }
     * }
     * ```
     *
     * 注意 `collect` 是一个 `suspend` 函数，强烈建议在与 Bot 实例相同的 [CoroutineScope] 中使用。
     */
    val eventFlow: SharedFlow<AcidifyEvent>
        get() = sharedEventFlow

    /**
     * 当前登录用户的 QQ 号
     */
    val uin: Long
        get() = sessionStore.uin.takeIf { it != 0L }
            ?: throw IllegalStateException("用户尚未登录")

    /**
     * 当前登录用户的 uid
     */
    val uid: String
        get() = sessionStore.uid.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("用户尚未登录")

    /**
     * 表情信息映射，键为 qSid，值为对应的 [BotFaceDetail] 实例。
     */
    val faceDetailMap: Map<String, BotFaceDetail>
        get() = faceDetailMapMut

    /**
     * 表示当前 Bot 是否已登录
     */
    var isLoggedIn: Boolean = false
        internal set

    /**
     * 创建一个 [Logger] 实例，通常用于库内部日志记录，并将产生的日志发送到提供的 [LogHandler]。
     */
    @JsName("createLoggerFromObject")
    fun createLogger(fromObject: Any): Logger {
        return Logger(
            this,
            fromObject::class.loggingTag
                ?: throw IllegalStateException("Cannot create logger for anonymous class")
        )
    }

    /**
     * 根据一个自定义的 tag 创建一个 [Logger] 实例，通常用于匿名类或方法的日志记录，并将产生的日志发送到提供的 [LogHandler]。
     */
    @JsName("createLoggerFromTag")
    fun createLogger(fromTag: String): Logger {
        return Logger(this, fromTag)
    }

    internal suspend fun HttpRequestBuilder.withCookies(domain: String) {
        header(
            HttpHeaders.Cookie,
            getCookies(domain).entries.joinToString("; ") { (k, v) -> "$k=$v" }
        )
    }

    internal suspend fun HttpRequestBuilder.withBkn() {
        parameter("bkn", getCsrfToken())
    }

    /**
     * 发送自定义 SSO 数据包。
     * **Ensure that you know what you are doing!**
     * @param cmd 命令字符串
     * @param payload 原始数据
     * @param timeoutMillis 超时时间，默认 10000 毫秒
     */
    @UnsafeAcidifyApi
    suspend fun sendPacket(cmd: String, payload: ByteArray, timeoutMillis: Long = 10000L): SsoResponse {
        val sequence = client.ssoSequence++
        return client.packetContext.sendPacket(
            command = cmd,
            sequence = sequence,
            payload = payload,
            requestType = RequestType.D2Auth,
            encryptType = EncryptType.WithD2Key,
            timeoutMillis = timeoutMillis,
            ssoSecureInfo = if (client.signRequiredCommand.contains(cmd)) {
                client.signProvider.sign(
                    cmd = cmd,
                    seq = sequence,
                    src = payload,
                ).let {
                    SsoSecureInfo(
                        sign = it.sign,
                        token = it.token,
                        extra = it.extra,
                    )
                }
            } else {
                null
            }
        )
    }

    companion object {
        @JsStatic
        suspend fun create(
            appInfo: AppInfo,
            sessionStore: SessionStore,
            signProvider: SignProvider,
            scope: CoroutineScope,
            minLogLevel: LogLevel,
            logHandler: LogHandler,
        ): Bot = Bot(
            appInfo = appInfo,
            sessionStore = sessionStore,
            signProvider = signProvider,
            scope = scope,
        ).apply {
            launch {
                sharedLogFlow
                    .filter { it.level >= minLogLevel }
                    .collect {
                        logHandler.handleLog(
                            it.level,
                            it.tag,
                            it.messageSupplier(),
                            it.throwable
                        )
                    }
            }
            client.packetContext.startConnectLoop()
        }
    }
}
