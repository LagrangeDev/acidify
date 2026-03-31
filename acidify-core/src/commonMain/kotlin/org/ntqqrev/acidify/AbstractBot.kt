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
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import org.ntqqrev.acidify.common.SsoResponse
import org.ntqqrev.acidify.common.UnsafeAcidifyApi
import org.ntqqrev.acidify.entity.BotFriend
import org.ntqqrev.acidify.entity.BotGroup
import org.ntqqrev.acidify.entity.internal.CacheUtility
import org.ntqqrev.acidify.event.AcidifyEvent
import org.ntqqrev.acidify.event.internal.KickSignal
import org.ntqqrev.acidify.event.internal.MsgPushSignal
import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.logging.LogHandler
import org.ntqqrev.acidify.logging.LogLevel
import org.ntqqrev.acidify.logging.Logger
import org.ntqqrev.acidify.logging.loggingTag
import org.ntqqrev.acidify.struct.BotFaceDetail
import kotlin.js.JsName

sealed class AbstractBot(
    scope: CoroutineScope,
    val minLogLevel: LogLevel,
    val logHandler: LogHandler,
) : CoroutineScope by scope {
    internal abstract val client: AbstractClient

    internal val logger = this.createLogger(this)
    internal val sharedEventFlow = MutableSharedFlow<AcidifyEvent>(
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    internal val signals = listOf(
        MsgPushSignal,
        KickSignal
    ).associateBy { it.cmd }
    internal lateinit var faceDetailMapMut: Map<String, BotFaceDetail>
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
     * 表情信息映射，键为 qSid，值为对应的 [BotFaceDetail] 实例。
     */
    val faceDetailMap: Map<String, BotFaceDetail>
        get() = faceDetailMapMut

    /**
     * 当前登录用户的 QQ 号
     */
    abstract val uin: Long

    /**
     * 当前登录用户的 uid
     */
    abstract val uid: String

    /**
     * 表示当前 Bot 是否已登录
     */
    var isLoggedIn: Boolean = false
        internal set

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
            ssoReservedMsgType = 0,
            timeoutMillis = timeoutMillis,
            ssoSecureInfo = client.getSsoSecureInfo(cmd, sequence, payload)
        )
    }
}