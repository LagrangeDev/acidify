package org.ntqqrev.acidify

import co.touchlab.stately.collections.ConcurrentMutableMap
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.json.Json
import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.SessionStore
import org.ntqqrev.acidify.common.SignProvider
import org.ntqqrev.acidify.event.AcidifyEvent
import org.ntqqrev.acidify.event.QRCodeGeneratedEvent
import org.ntqqrev.acidify.event.QRCodeStateQueryEvent
import org.ntqqrev.acidify.event.SessionStoreUpdatedEvent
import org.ntqqrev.acidify.event.internal.MsgPushSignal
import org.ntqqrev.acidify.event.internal.Signal
import org.ntqqrev.acidify.exception.BotOnlineException
import org.ntqqrev.acidify.exception.MessageSendException
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.packet.message.media.FileId
import org.ntqqrev.acidify.internal.packet.message.media.IndexNode
import org.ntqqrev.acidify.internal.packet.misc.GroupAnnounceResponse
import org.ntqqrev.acidify.internal.packet.misc.GroupAnnounceSendResponse
import org.ntqqrev.acidify.internal.packet.misc.GroupEssenceResponse
import org.ntqqrev.acidify.internal.service.file.*
import org.ntqqrev.acidify.internal.service.group.*
import org.ntqqrev.acidify.internal.service.message.*
import org.ntqqrev.acidify.internal.service.system.*
import org.ntqqrev.acidify.internal.util.md5
import org.ntqqrev.acidify.internal.util.sha1
import org.ntqqrev.acidify.internal.util.triSha1
import org.ntqqrev.acidify.message.*
import org.ntqqrev.acidify.message.BotEssenceMessage.Companion.toBotEssenceMessage
import org.ntqqrev.acidify.message.BotForwardedMessage.Companion.parseForwardedMessage
import org.ntqqrev.acidify.message.BotIncomingMessage.Companion.parseMessage
import org.ntqqrev.acidify.message.internal.MessageBuildingContext
import org.ntqqrev.acidify.pb.invoke
import org.ntqqrev.acidify.struct.*
import org.ntqqrev.acidify.util.HtmlEntities
import org.ntqqrev.acidify.util.createHttpClient
import org.ntqqrev.acidify.util.log.LogHandler
import org.ntqqrev.acidify.util.log.LogLevel
import org.ntqqrev.acidify.util.log.LogMessage
import org.ntqqrev.acidify.util.log.Logger
import kotlin.io.encoding.Base64

/**
 * Acidify Bot 实例
 */
class Bot(
    val appInfo: AppInfo,
    val sessionStore: SessionStore,
    signProvider: SignProvider,
    scope: CoroutineScope,
    minLogLevel: LogLevel,
    logHandler: LogHandler,
) : CoroutineScope by scope {
    private val logger = this.createLogger(this)
    internal val client = LagrangeClient(appInfo, sessionStore, signProvider, this::createLogger, scope)
    internal val sharedEventFlow = MutableSharedFlow<AcidifyEvent>(
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    internal val sharedLogFlow = MutableSharedFlow<LogMessage>(
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    internal val signals = listOf<Signal>(
        MsgPushSignal
    ).associateBy { it.cmd }
    internal val faceDetailMapMut = mutableMapOf<String, BotFaceDetail>()
    internal val uin2uidMap = ConcurrentMutableMap<Long, String>()
    internal val uid2uinMap = ConcurrentMutableMap<String, Long>()
    internal var heartbeatJob: Job? = null
    internal var eventCollectJob: Job? = null

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
     * 当前登录用户的 uin（QQ 号）
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
     * HTTP 客户端实例，可用于发起自定义的 HTTP 请求。
     */
    val httpClient = createHttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    init {
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
        client.packetLogic.startConnectLoop()
    }

    /**
     * 创建一个 [Logger] 实例，通常用于库内部日志记录，并将产生的日志发送到提供的 [LogHandler]。
     */
    fun createLogger(fromObject: Any): Logger {
        return Logger(
            this,
            fromObject::class.qualifiedName
                ?: throw IllegalStateException("Cannot create logger for anonymous class")
        )
    }

    /**
     * 发起二维码登录请求。过程中会触发事件：
     * - [QRCodeGeneratedEvent]：当二维码生成时触发，包含二维码链接和 PNG 图片数据
     * - [QRCodeStateQueryEvent]：每次查询二维码状态时触发，包含当前二维码状态（例如未扫码、已扫码未确认、已确认等）
     * @param queryInterval 查询间隔（单位 ms），不能小于 `1000`
     * @throws org.ntqqrev.acidify.exception.WtLoginException 当二维码扫描成功，但后续登录失败时抛出
     * @throws IllegalStateException 当二维码过期或用户取消登录时抛出
     * @see QRCodeState
     */
    suspend fun qrCodeLogin(queryInterval: Long = 3000L) {
        require(queryInterval >= 1000L) { "查询间隔不能小于 1000 毫秒" }
        val qrCode = client.callService(FetchQRCode)
        logger.i { "二维码 URL：${qrCode.qrCodeUrl}" }
        sharedEventFlow.emit(QRCodeGeneratedEvent(qrCode.qrCodeUrl, qrCode.qrCodePng))

        while (true) {
            val state = client.callService(QueryQRCodeState)
            logger.d { "二维码状态：${state.name} (${state.value})" }
            sharedEventFlow.emit(QRCodeStateQueryEvent(state))
            when (state) {
                QRCodeState.CONFIRMED -> break
                QRCodeState.CODE_EXPIRED -> throw IllegalStateException("二维码已过期")
                QRCodeState.CANCELLED -> throw IllegalStateException("用户取消了登录")
                QRCodeState.UNKNOWN -> throw IllegalStateException("未知的二维码状态")
                else -> {} // pass
            }
            delay(queryInterval)
        }

        client.callService(WtLogin)
        logger.d { "成功获取 $uin 的登录凭据" }
        sharedEventFlow.emit(SessionStoreUpdatedEvent(sessionStore))
        online()
    }

    /**
     * 尝试使用现有的 Session 信息上线。
     * 请优先调用 [tryLogin]，该方法会在现有 Session 失效时自动调用 [qrCodeLogin]。
     * 若确定 Session 有效且不希望进行二维码登录，可调用此方法。
     */
    suspend fun online() {
        val result = client.callService(BotOnline)
        if (result != "register success") {
            throw BotOnlineException(result)
        }
        isLoggedIn = true
        logger.i { "用户 $uin 已上线" }
        
        val highwayInfo = client.callService(FetchHighwayInfo)
        val (host, port) = highwayInfo.servers[1]!![0]
        client.highwayLogic.setHighwayUrl(host, port, highwayInfo.sigSession)
        logger.d { "已配置 Highway 服务器: $host:$port" }

        heartbeatJob = launch {
            while (isLoggedIn) {
                try {
                    client.callService(Heartbeat)
                } catch (e: Exception) {
                    logger.w(e) { "心跳包发送失败" }
                }
                delay(300_000L)
            }
        }

        eventCollectJob = launch {
            while (currentCoroutineContext().isActive) {
                val sso = client.pushChannel.receive()
                val signal = signals[sso.command]
                if (signal != null) {
                    try {
                        val parsed = signal.parse(this@Bot, sso.response)
                        parsed.forEach { sharedEventFlow.emit(it) }
                    } catch (e: Exception) {
                        logger.e(e) { "处理信令 ${sso.command} 时出现错误" }
                    }
                }
            }
        }

        faceDetailMapMut.putAll(
            client.callService(FetchFaceDetails).associateBy { it.qSid }
        ).also { logger.d { "加载了 ${faceDetailMapMut.size} 条表情信息" } }
    }

    /**
     * 下线 Bot，释放资源。
     */
    suspend fun offline() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        eventCollectJob?.cancel()
        eventCollectJob = null
        client.callService(BotOffline)
        logger.i { "用户 $uin 已下线" }
    }

    /**
     * 先尝试使用现有的 Session 信息登录，若失败则调用 [qrCodeLogin] 重新登录。
     * 如果是第一次登录，请务必调用 [qrCodeLogin]。
     */
    suspend fun tryLogin() {
        try {
            online()
        } catch (e: Exception) {
            logger.w(e) { "使用现有 Session 登录失败，尝试二维码登录" }
            sessionStore.clear()
            sharedEventFlow.emit(SessionStoreUpdatedEvent(sessionStore))
            qrCodeLogin()
        }
    }

    /**
     * 通过 uin（QQ 号）获取用户信息。
     */
    suspend fun fetchUserInfoByUin(uin: Long) = client.callService(FetchUserInfo.ByUin, uin)

    /**
     * 通过 uid 获取用户信息。
     */
    suspend fun fetchUserInfoByUid(uid: String) = client.callService(FetchUserInfo.ByUid, uid)

    /**
     * 获取好友与好友分组信息。
     */
    suspend fun fetchFriends(): List<BotFriendData> {
        var nextUin: Long? = null
        val friendDataResult = mutableListOf<BotFriendData>()
        do {
            val resp = client.callService(FetchFriends, FetchFriends.Req(nextUin))
            nextUin = resp.nextUin
            friendDataResult.addAll(resp.friendDataList)
        } while (nextUin != null)
        return friendDataResult.also {
            launch {
                it.forEach { data ->
                    uin2uidMap[data.uin] = data.uid
                    uid2uinMap[data.uid] = data.uin
                }
            }
        }
    }

    /**
     * 获取群信息。
     */
    suspend fun fetchGroups(): List<BotGroupData> {
        return client.callService(FetchGroups)
    }

    /**
     * 获取指定群的成员信息。
     */
    suspend fun fetchGroupMembers(groupUin: Long): List<BotGroupMemberData> {
        var cookie: ByteArray? = null
        val memberDataResult = mutableListOf<BotGroupMemberData>()
        do {
            val resp = client.callService(FetchGroupMembers, FetchGroupMembers.Req(groupUin, cookie))
            cookie = resp.cookie
            memberDataResult.addAll(resp.memberDataList)
        } while (cookie != null)
        return memberDataResult.also {
            launch {
                it.forEach { data ->
                    uin2uidMap[data.uin] = data.uid
                    uid2uinMap[data.uid] = data.uin
                }
            }
        }
    }

    /**
     * 解析 uid 到 uin（QQ 号）。
     * 如果之前未解析过该 uid，会发起网络请求获取用户信息。
     */
    suspend fun getUinByUid(uid: String): Long {
        return uid2uinMap.getOrPut(uid) {
            fetchUserInfoByUid(uid).uin.also { uin2uidMap[it] = uid }
        }
    }

    /**
     * 解析 uin（QQ 号）到 uid，该过程可能失败，此时抛出 [NoSuchElementException]。
     * 若 [mayComeFromGroupUin] 非空且在缓存中未找到对应 uid，会尝试从该群的成员列表中查找；
     * 否则，会尝试从好友列表中查找。
     */
    suspend fun getUidByUin(uin: Long, mayComeFromGroupUin: Long? = null): String {
        return uin2uidMap[uin] ?: if (mayComeFromGroupUin != null) {
            fetchGroupMembers(mayComeFromGroupUin).firstOrNull { it.uin == uin }?.uid?.also {
                uin2uidMap[uin] = it
                uid2uinMap[it] = uin
            }
        } else {
            fetchFriends().firstOrNull { it.uin == uin }?.uid?.also {
                uin2uidMap[uin] = it
                uid2uinMap[it] = uin
            }
        } ?: throw NoSuchElementException("无法解析 uin $uin 对应的 uid")
    }

    /**
     * 获取 s_key，用于组成 Cookie。
     */
    suspend fun getSKey() = client.ticketLogic.getSKey()

    /**
     * 获取给定域名的 p_skey，用于组成 Cookie。
     */
    suspend fun getPSKey(domain: String) = client.ticketLogic.getPSKey(domain)

    /**
     * 获取指定域名的 Cookie 键值对。
     */
    suspend fun getCookies(domain: String) = mapOf(
        "p_uin" to "o$uin",
        "p_skey" to getPSKey(domain),
        "skey" to getSKey(),
        "uin" to uin.toString(),
    )

    /**
     * 获取 CSRF Token。
     */
    suspend fun getCsrfToken() = client.ticketLogic.getCsrfToken()

    /**
     * 发送好友消息
     * @param friendUin 好友 QQ 号
     * @param build 消息构建器
     */
    suspend fun sendFriendMessage(
        friendUin: Long,
        build: suspend BotOutgoingMessageBuilder.() -> Unit
    ): BotOutgoingMessageResult {
        val friendUid = getUidByUin(friendUin)
        val context = MessageBuildingContext(
            bot = this,
            scene = MessageScene.FRIEND,
            peerUin = friendUin,
            peerUid = friendUid
        )
        build(context)
        val elems = context.build()
        val resp = client.callService(SendFriendMessage, SendFriendMessage.Req(friendUin, friendUid, elems))
        if (resp.result != 0) {
            throw MessageSendException(resp.result, resp.errMsg)
        }
        return BotOutgoingMessageResult(resp.sequence, resp.sendTime)
    }

    /**
     * 发送群消息
     * @param groupUin 群号
     * @param build 消息构建器
     */
    suspend fun sendGroupMessage(
        groupUin: Long,
        build: suspend BotOutgoingMessageBuilder.() -> Unit
    ): BotOutgoingMessageResult {
        val context = MessageBuildingContext(
            bot = this,
            scene = MessageScene.GROUP,
            peerUin = groupUin,
            peerUid = groupUin.toString(),
        )
        build(context)
        val elems = context.build()
        val resp = client.callService(SendGroupMessage, SendGroupMessage.Req(groupUin, elems))
        if (resp.result != 0) {
            throw MessageSendException(resp.result, resp.errMsg)
        }
        return BotOutgoingMessageResult(resp.sequence, resp.sendTime)
    }

    /**
     * 撤回好友消息
     * @param friendUin 好友 QQ 号
     * @param sequence 消息序列号（clientSequence）
     * @param privateSequence 消息的私聊序列号（用于获取消息详情）
     * @param timestamp 消息时间戳（秒）
     */
    suspend fun recallFriendMessage(
        friendUin: Long, sequence: Long, privateSequence: Long, timestamp: Long
    ) {
        val friendUid = getUidByUin(friendUin)

        // 从原始消息包中提取 random 字段
        val raw = client.callService(
            FetchFriendMessages, FetchFriendMessages.Req(friendUid, privateSequence, privateSequence)
        ).firstOrNull() ?: throw IllegalStateException("消息不存在")

        val contentHead = raw.get { contentHead }
        val random = contentHead.get { random }

        client.callService(
            RecallFriendMessage, RecallFriendMessage.Req(
                targetUid = friendUid,
                clientSequence = sequence,
                messageSequence = privateSequence,
                random = random,
                timestamp = timestamp
            )
        )
    }

    /**
     * 撤回群消息
     * @param groupUin 群号
     * @param sequence 消息序列号
     */
    suspend fun recallGroupMessage(groupUin: Long, sequence: Long) = client.callService(
        RecallGroupMessage, RecallGroupMessage.Req(
            groupUin = groupUin, sequence = sequence
        )
    )

    /**
     * 向上获取与好友的历史消息
     * @param friendUin 好友 QQ 号
     * @param limit 最多获取的消息数量，最大值为 30
     * @param startSequence 起始消息序列号（包含该序列号），为 `null` 则从最新消息开始获取
     */
    suspend fun getFriendHistoryMessages(
        friendUin: Long,
        limit: Int,
        startSequence: Long? = null
    ): BotHistoryMessages {
        require(limit in 1..30) { "limit 必须在 1 到 30 之间" }
        val friendUid = getUidByUin(friendUin)
        val end = startSequence ?: client.callService(GetFriendLatestSequence, friendUid)
        val start = (end - limit + 1).coerceAtLeast(1)

        val resp = client.callService(
            FetchFriendMessages,
            FetchFriendMessages.Req(friendUid, start, end)
        )

        val messages = resp.mapNotNull { parseMessage(it) }

        val nextStartSeq = if (start > 1) (start - 1) else null
        return BotHistoryMessages(messages, nextStartSeq)
    }

    /**
     * 向上获取群聊的历史消息
     * @param groupUin 群号
     * @param limit 最多获取的消息数量，最大值为 30
     * @param startSequence 起始消息序列号（包含该序列号），为 `null` 则从最新消息开始获取
     */
    suspend fun getGroupHistoryMessages(
        groupUin: Long,
        limit: Int,
        startSequence: Long? = null
    ): BotHistoryMessages {
        require(limit in 1..30) { "limit 必须在 1 到 30 之间" }
        val end = startSequence ?: client.callService(FetchGroupExtraInfo, groupUin).latestMessageSeq
        val start = (end - limit + 1).coerceAtLeast(1)

        val resp = client.callService(
            FetchGroupMessages,
            FetchGroupMessages.Req(groupUin, start, end)
        )

        val messages = resp.mapNotNull { parseMessage(it) }

        val nextStartSeq = if (start > 1) (start - 1) else null
        return BotHistoryMessages(messages, nextStartSeq)
    }

    /**
     * 获取给定资源 ID 的下载链接，支持图片、语音、视频。
     */
    suspend fun getDownloadUrl(resourceId: String): String {
        if (resourceId.startsWith("http://") || resourceId.startsWith("https://"))
            return resourceId // direct URL

        val actualLength = if (resourceId.length % 4 == 0) {
            resourceId.length
        } else {
            resourceId.length + (4 - resourceId.length % 4)
        }
        val normalizedBase64 = resourceId
            .replace("-", "+")
            .replace("_", "/")
            .padEnd(actualLength, '=')
        val fileIdDecoded = FileId(Base64.decode(normalizedBase64))
        val appId = fileIdDecoded.get { appId }
        val indexNode = IndexNode {
            it[fileUuid] = resourceId
            it[storeId] = fileIdDecoded.get { storeId }
            it[ttl] = fileIdDecoded.get { ttl }
        }
        return client.callService(
            when (appId) {
                1402 -> RichMediaDownload.PrivateRecord
                1403 -> RichMediaDownload.GroupRecord
                1406 -> RichMediaDownload.PrivateImage
                1407 -> RichMediaDownload.GroupImage
                1413 -> RichMediaDownload.PrivateVideo
                1415 -> RichMediaDownload.GroupVideo
                else -> throw IllegalArgumentException("不支持的资源类型 $appId")
            },
            indexNode
        )
    }

    /**
     * 获取合并转发消息内容
     * @param resId 合并转发消息的 resId
     * @return 转发消息列表
     */
    suspend fun getForwardedMessages(resId: String): List<BotForwardedMessage> {
        return client.callService(RecvLongMsg, RecvLongMsg.Req(resId)).mapNotNull { parseForwardedMessage(it) }
    }

    /**
     * 标记好友消息为已读
     * @param friendUin 好友 QQ 号
     * @param startSequence 消息序列号，标记该序列号及之前的消息为已读
     * @param startTime 消息的 Unix 时间戳（秒）
     */
    suspend fun markFriendMessagesAsRead(
        friendUin: Long,
        startSequence: Long,
        startTime: Long
    ) = client.callService(
        ReportMessageRead,
        ReportMessageRead.Req(
            groupUin = null,
            targetUid = getUidByUin(friendUin),
            startSequence = startSequence,
            time = startTime
        )
    )

    /**
     * 发送好友戳一戳
     * @param friendUin 好友 QQ 号
     * @param isSelf 是否戳自己（默认为 false）
     */
    suspend fun sendFriendNudge(
        friendUin: Long, isSelf: Boolean = false
    ) = client.callService(
        SendFriendNudge, SendFriendNudge.Req(
            friendUin = friendUin, isSelf = isSelf
        )
    )

    /**
     * 给好友点赞
     * @param friendUin 好友 QQ 号
     * @param count 点赞次数（默认为 1）
     */
    suspend fun sendProfileLike(
        friendUin: Long, count: Int = 1
    ) = client.callService(
        SendProfileLike, SendProfileLike.Req(
            targetUid = getUidByUin(friendUin), count = count
        )
    )

    /**
     * 标记群消息为已读
     * @param groupUin 群号
     * @param startSequence 消息序列号，标记该序列号及之前的消息为已读
     */
    suspend fun markGroupMessagesAsRead(
        groupUin: Long,
        startSequence: Long
    ) = client.callService(
        ReportMessageRead,
        ReportMessageRead.Req(
            groupUin = groupUin,
            targetUid = null,
            startSequence = startSequence,
            time = 0L
        )
    )

    /**
     * 设置群名称
     * @param groupUin 群号
     * @param groupName 新的群名称
     */
    suspend fun setGroupName(
        groupUin: Long,
        groupName: String
    ) = client.callService(
        SetGroupName,
        SetGroupName.Req(
            groupUin = groupUin,
            groupName = groupName
        )
    )

    /**
     * 设置群头像
     * @param groupUin 群号
     * @param imageData 图片数据（字节数组）
     */
    suspend fun setGroupAvatar(
        groupUin: Long,
        imageData: ByteArray
    ) = client.highwayLogic.uploadGroupAvatar(groupUin, imageData)

    /**
     * 设置群成员的群名片
     * @param groupUin 群号
     * @param memberUin 成员 QQ 号
     * @param card 新的群名片
     */
    suspend fun setGroupMemberCard(
        groupUin: Long,
        memberUin: Long,
        card: String
    ) = client.callService(
        SetMemberCard,
        SetMemberCard.Req(
            groupUin = groupUin,
            memberUid = getUidByUin(memberUin, groupUin),
            card = card
        )
    )

    /**
     * 设置群成员的专属头衔
     * @param groupUin 群号
     * @param memberUin 成员 QQ 号
     * @param specialTitle 专属头衔内容，长度不能超过 18 个字节（通常为 6 个汉字或 18 个英文字符）
     */
    suspend fun setGroupMemberSpecialTitle(
        groupUin: Long,
        memberUin: Long,
        specialTitle: String
    ) = client.callService(
        SetMemberTitle,
        SetMemberTitle.Req(
            groupUin = groupUin,
            memberUid = getUidByUin(memberUin, groupUin),
            specialTitle = specialTitle.takeIf {
                it.encodeToByteArray().size <= 18
            } ?: throw IllegalArgumentException("专属头衔长度不能超过 18 个字节")
        )
    )

    /**
     * 设置群管理员
     * @param groupUin 群号
     * @param memberUin 成员 QQ 号
     * @param isAdmin 是否设置为管理员，`false` 表示取消管理员
     */
    suspend fun setGroupMemberAdmin(
        groupUin: Long,
        memberUin: Long,
        isAdmin: Boolean
    ) = client.callService(
        SetMemberAdmin,
        SetMemberAdmin.Req(
            groupUin = groupUin,
            memberUid = getUidByUin(memberUin, groupUin),
            isAdmin = isAdmin
        )
    )

    /**
     * 设置群成员禁言
     * @param groupUin 群号
     * @param memberUin 成员 QQ 号
     * @param duration 禁言时长（秒），设为 `0` 表示取消禁言
     */
    suspend fun setGroupMemberMute(
        groupUin: Long,
        memberUin: Long,
        duration: Int
    ) = client.callService(
        SetMemberMute,
        SetMemberMute.Req(
            groupUin = groupUin,
            memberUid = getUidByUin(memberUin, groupUin),
            duration = duration
        )
    )

    /**
     * 设置群全员禁言
     * @param groupUin 群号
     * @param isMute 是否开启全员禁言，`false` 表示取消全员禁言
     */
    suspend fun setGroupWholeMute(
        groupUin: Long,
        isMute: Boolean
    ) = client.callService(
        SetGroupWholeMute,
        SetGroupWholeMute.Req(
            groupUin = groupUin,
            isMute = isMute
        )
    )

    /**
     * 踢出群成员
     * @param groupUin 群号
     * @param memberUin 成员 QQ 号
     * @param rejectAddRequest 是否拒绝再次加群申请
     * @param reason 踢出原因（可选）
     */
    suspend fun kickGroupMember(
        groupUin: Long,
        memberUin: Long,
        rejectAddRequest: Boolean = false,
        reason: String = ""
    ) = client.callService(
        KickMember,
        KickMember.Req(
            groupUin = groupUin,
            memberUid = getUidByUin(memberUin, groupUin),
            rejectAddRequest = rejectAddRequest,
            reason = reason
        )
    )

    /**
     * 获取群公告列表
     * @param groupUin 群号
     * @return 群公告列表
     */
    suspend fun getGroupAnnouncements(groupUin: Long): List<BotGroupAnnouncement> {
        val bkn = getCsrfToken()
        val url = "https://web.qun.qq.com/cgi-bin/announce/get_t_list" +
                "?bkn=$bkn&qid=$groupUin&ft=23&ni=1&n=1&i=1&log_read=1&platform=1&s=-1&n=20"

        val cookie = getCookies("qun.qq.com").entries.joinToString("; ") { (k, v) -> "$k=$v" }
        val response = httpClient.get(url) {
            headers {
                append(HttpHeaders.Cookie, cookie)
            }
        }

        if (!response.status.isSuccess()) {
            throw RuntimeException("获取群公告失败: ${response.status}")
        }

        val announceResp = response.body<GroupAnnounceResponse>()
        return (announceResp.feeds + announceResp.inst).map { feed ->
            BotGroupAnnouncement(
                groupUin = groupUin,
                announcementId = feed.noticeId,
                senderId = feed.senderId,
                time = feed.publishTime,
                content = HtmlEntities.unescape(feed.message.text),
                imageUrl = feed.message.images.firstOrNull()?.let {
                    "https://gdynamic.qpic.cn/gdynamic/${it.id}/0"
                }
            )
        }
    }

    /**
     * 发送群公告
     * @param groupUin 群号
     * @param content 公告内容
     * @param imageUrl 公告图片 URL（可选，暂不支持）
     * @return 公告 ID
     */
    suspend fun sendGroupAnnouncement(
        groupUin: Long,
        content: String,
        imageUrl: String? = null
    ): String {
        if (imageUrl != null) {
            TODO("暂不支持带图片的群公告")
        }

        val bkn = getCsrfToken()
        val url = "https://web.qun.qq.com/cgi-bin/announce/add_qun_notice?bkn=$bkn"
        val body = "qid=$groupUin&bkn=$bkn&text=${content.encodeURLParameter()}" +
                "&pinned=0&type=1&settings={\"is_show_edit_card\":0,\"tip_window_type\":1,\"confirm_required\":1}"

        val cookie = getCookies("qun.qq.com").entries.joinToString("; ") { (k, v) -> "$k=$v" }
        val response = httpClient.post(url) {
            headers {
                append(HttpHeaders.Cookie, cookie)
                append(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 7.1.2; PCRT00 Build/N2G48H)")
                append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
            }
            setBody(body)
        }

        if (!response.status.isSuccess()) {
            throw RuntimeException("发送群公告失败: ${response.status}")
        }

        val sendResp = response.body<GroupAnnounceSendResponse>()
        return sendResp.noticeId
    }

    /**
     * 删除群公告
     * @param groupUin 群号
     * @param announcementId 公告 ID
     */
    suspend fun deleteGroupAnnouncement(
        groupUin: Long,
        announcementId: String
    ) {
        val bkn = getCsrfToken()
        val url = "https://web.qun.qq.com/cgi-bin/announce/del_feed" +
                "?fid=$announcementId&qid=$groupUin&bkn=$bkn&ft=23&op=1"

        val cookie = getCookies("qun.qq.com").entries.joinToString("; ") { (k, v) -> "$k=$v" }
        val response = httpClient.get(url) {
            headers {
                append(HttpHeaders.Cookie, cookie)
            }
        }

        if (!response.status.isSuccess()) {
            throw RuntimeException("删除群公告失败: ${response.status}")
        }
    }

    /**
     * 获取群精华消息列表
     * @param groupUin 群号
     * @param pageIndex 页码索引
     * @param pageSize 每页包含的精华消息数量
     * @return 精华消息列表
     */
    suspend fun getGroupEssenceMessages(
        groupUin: Long,
        pageIndex: Int,
        pageSize: Int
    ): BotEssenceMessageResult {
        val bkn = getCsrfToken()
        val url = "https://qun.qq.com/cgi-bin/group_digest/digest_list" +
                "?random=7800&X-CROSS-ORIGIN=fetch&group_code=$groupUin" +
                "&page_start=$pageIndex&page_limit=$pageSize&bkn=$bkn"

        val cookie = getCookies("qun.qq.com").entries.joinToString("; ") { (k, v) -> "$k=$v" }
        val response = httpClient.get(url) {
            headers {
                append(HttpHeaders.Cookie, cookie)
            }
        }

        if (!response.status.isSuccess()) {
            throw RuntimeException("获取群精华消息失败: ${response.status}")
        }

        val essenceResp = response.body<GroupEssenceResponse>()
        val msgList = essenceResp.data.msgList ?: emptyList()

        return BotEssenceMessageResult(
            messages = msgList.mapNotNull { it.toBotEssenceMessage(groupUin) },
            isEnd = essenceResp.data.isEnd
        )
    }

    /**
     * 设置群精华消息
     * @param groupUin 群号
     * @param sequence 消息序列号
     * @param random 消息 random 字段
     */
    suspend fun setGroupEssenceMessage(
        groupUin: Long,
        sequence: Int
    ) {
        val random = client.callService(
            FetchGroupMessages,
            FetchGroupMessages.Req(groupUin, sequence.toLong(), sequence.toLong())
        ).firstOrNull()?.get { contentHead }?.get { random }
            ?: throw IllegalStateException("消息不存在，无法获取 random 字段")
        client.callService(
            SetGroupEssenceMessage,
            SetGroupEssenceMessage.Req(
                groupUin = groupUin,
                sequence = sequence,
                random = random
            )
        )
    }

    /**
     * 退出群聊
     * @param groupUin 群号
     */
    suspend fun quitGroup(groupUin: Long) = client.callService(
        QuitGroup,
        QuitGroup.Req(
            groupUin = groupUin
        )
    )

    /**
     * 发送群消息表情回应
     * @param groupUin 群号
     * @param sequence 消息序列号
     * @param code 表情代码
     * @param isAdd 是否添加表情回应，`false` 表示取消回应
     */
    suspend fun setGroupMessageReaction(
        groupUin: Long,
        sequence: Int,
        code: String,
        isAdd: Boolean = true
    ) = client.callService(
        SetGroupMessageReaction,
        SetGroupMessageReaction.Req(
            groupUin = groupUin,
            sequence = sequence,
            code = code,
            isAdd = isAdd
        )
    )

    /**
     * 发送群戳一戳
     * @param groupUin 群号
     * @param targetUin 被戳的成员 QQ 号
     */
    suspend fun sendGroupNudge(
        groupUin: Long,
        targetUin: Long
    ) = client.callService(
        SendGroupNudge,
        SendGroupNudge.Req(
            groupUin = groupUin,
            targetUin = targetUin
        )
    )

    /**
     * 上传群文件
     * @param groupUin 群号
     * @param fileName 文件名
     * @param fileData 文件数据
     * @param parentFolderId 父文件夹 ID，默认为根目录 "/"
     * @return 文件 ID
     */
    suspend fun uploadGroupFile(
        groupUin: Long, fileName: String, fileData: ByteArray, parentFolderId: String = "/"
    ): String {
        val uploadResp = client.callService(
            UploadGroupFile, UploadGroupFile.Req(
                groupUin = groupUin,
                fileName = fileName,
                fileSize = fileData.size.toLong(),
                fileMd5 = fileData.md5(),
                fileSha1 = fileData.sha1(),
                fileTriSha1 = fileData.triSha1(),
                parentFolderId = parentFolderId
            )
        )

        if (!uploadResp.fileExist) {
            client.highwayLogic.uploadGroupFile(
                senderUin = uin,
                groupUin = groupUin,
                fileName = fileName,
                fileData = fileData,
                fileId = uploadResp.fileId,
                fileKey = uploadResp.fileKey,
                checkKey = uploadResp.checkKey,
                uploadIp = uploadResp.uploadIp,
                uploadPort = uploadResp.uploadPort
            )
        }

        client.callService(
            BroadcastGroupFile, BroadcastGroupFile.Req(
                groupUin = groupUin, fileId = uploadResp.fileId
            )
        )

        return uploadResp.fileId
    }

    /**
     * TODO: THIS API IS BROKEN
     * 上传私聊文件
     * @param friendUin 好友 QQ 号
     * @param fileName 文件名
     * @param fileData 文件数据
     * @return 文件 ID
     */
    suspend fun uploadPrivateFile(
        friendUin: Long, fileName: String, fileData: ByteArray
    ): String {
        val friendUid = getUidByUin(friendUin)
        val fileMd5 = fileData.md5()
        val fileSha1 = fileData.sha1()
        val md510M = fileData.copyOfRange(0, minOf(10 * 1024 * 1024, fileData.size)).md5()
        val fileTriSha1 = fileData.triSha1()

        val uploadResp = client.callService(
            UploadPrivateFile,
            UploadPrivateFile.Req(
                senderUid = uid,
                receiverUid = friendUid,
                fileName = fileName,
                fileSize = fileData.size,
                fileMd5 = fileMd5,
                fileSha1 = fileSha1,
                md510M = md510M,
                fileTriSha1 = fileTriSha1
            )
        )

        if (!uploadResp.fileExist) {
            client.highwayLogic.uploadPrivateFile(
                receiverUin = friendUin,
                fileName = fileName,
                fileData = fileData,
                fileMd5 = fileMd5,
                fileSha1 = fileSha1,
                md510M = md510M,
                fileTriSha1 = fileTriSha1,
                fileId = uploadResp.fileId,
                uploadKey = uploadResp.uploadKey,
                uploadIpAndPorts = uploadResp.ipAndPorts
            )
        }

        client.callService(
            BroadcastPrivateFile,
            BroadcastPrivateFile.Req(
                friendUin = friendUin,
                friendUid = friendUid,
                fileId = uploadResp.fileId,
                fileMd5 = fileMd5,
                fileName = fileName,
                fileSize = fileData.size.toLong(),
                crcMedia = uploadResp.fileCrcMedia
            )
        )

        return uploadResp.fileId
    }

    /**
     * 获取私聊文件下载链接
     * @param friendUin 好友 QQ 号
     * @param fileId 文件 ID
     * @param fileHash 文件的 TriSHA1 哈希值
     * @return 文件下载链接
     */
    suspend fun getPrivateFileDownloadUrl(
        friendUin: Long,
        fileId: String,
        fileHash: String
    ): String = client.callService(
        GetPrivateFileDownloadUrl,
        GetPrivateFileDownloadUrl.Req(
            receiverUid = getUidByUin(friendUin),
            fileUuid = fileId,
            fileHash = fileHash
        )
    )

    /**
     * 获取群文件下载链接
     * @param groupUin 群号
     * @param fileId 文件 ID
     * @return 文件下载链接
     */
    suspend fun getGroupFileDownloadUrl(
        groupUin: Long,
        fileId: String
    ): String = client.callService(
        GetGroupFileDownloadUrl,
        GetGroupFileDownloadUrl.Req(
            groupUin = groupUin,
            fileId = fileId
        )
    )

    /**
     * 获取群通知列表
     * @param startSequence 起始通知序列号，为 null 则从最新通知开始获取
     * @param isFiltered 是否只获取被过滤的通知（风险账号发起）
     * @param count 获取的最大通知数量
     * @return 群通知列表和下一页起始序列号
     */
    suspend fun getGroupNotifications(
        startSequence: Long? = null,
        isFiltered: Boolean = false,
        count: Int = 20
    ): Pair<List<BotGroupNotification>, Long?> {
        val resp = client.callService(
            if (isFiltered) FetchGroupNotifications.Filtered else FetchGroupNotifications.Normal,
            FetchGroupNotifications.Req(
                startSequence = startSequence ?: 0,
                count = count
            )
        )
        val notifications = resp.notifications.mapNotNull {
            with(BotGroupNotification) { parseNotification(it, isFiltered) }
        }
        return notifications to resp.nextSequence.takeIf { it != 0L }
    }

    /**
     * 处理群请求（同意/拒绝）
     * @param groupUin 群号
     * @param sequence 通知序列号
     * @param eventType 事件类型（1=入群请求, 22=邀请他人入群）
     * @param accept 是否同意（true=同意, false=拒绝）
     * @param isFiltered 是否是被过滤的请求
     * @param reason 拒绝理由（仅在拒绝时使用）
     */
    suspend fun setGroupRequest(
        groupUin: Long,
        sequence: Long,
        eventType: Int,
        accept: Boolean,
        isFiltered: Boolean = false,
        reason: String = ""
    ) {
        client.callService(
            if (isFiltered) SetGroupRequest.Filtered else SetGroupRequest.Normal,
            SetGroupRequest.Req(
                groupUin = groupUin,
                sequence = sequence,
                eventType = eventType,
                accept = if (accept) 1 else 2,
                reason = reason
            )
        )
    }

    /**
     * 处理群邀请（他人邀请自己入群）
     * @param groupUin 群号
     * @param invitationSeq 邀请序列号
     * @param accept 是否同意
     */
    suspend fun setGroupInvitation(
        groupUin: Long,
        invitationSeq: Long,
        accept: Boolean
    ) {
        client.callService(
            SetGroupRequest.Normal,
            SetGroupRequest.Req(
                groupUin = groupUin,
                sequence = invitationSeq,
                eventType = 2,
                accept = if (accept) 1 else 2,
                reason = ""
            )
        )
    }

    /**
     * 获取群文件/文件夹列表
     * @param groupUin 群号
     * @param targetDirectory 目标目录路径，默认为根目录 "/"
     * @param startIndex 起始索引，用于分页，默认为 0
     * @return 文件系统列表，包含文件列表、文件夹列表和是否到达末尾的标志
     */
    suspend fun getGroupFileList(
        groupUin: Long,
        targetDirectory: String = "/",
        startIndex: Int = 0
    ): BotGroupFileSystemList {
        var isEnd = false
        var currentIndex = startIndex
        val batchSize = 100
        val allFiles = mutableListOf<BotGroupFileEntry>()
        val allFolders = mutableListOf<BotGroupFolderEntry>()
        while (!isEnd) {
            val resp = client.callService(
                GetGroupFileList,
                GetGroupFileList.Req(
                    groupUin = groupUin,
                    targetDirectory = targetDirectory,
                    startIndex = currentIndex,
                    batchSize = batchSize
                )
            )
            allFiles.addAll(resp.files)
            allFolders.addAll(resp.folders)
            isEnd = resp.isEnd
            currentIndex += batchSize
        }
        return BotGroupFileSystemList(
            files = allFiles,
            folders = allFolders
        )
    }

    /**
     * 重命名群文件
     * @param groupUin 群号
     * @param fileId 文件 ID
     * @param parentFolderId 父文件夹 ID
     * @param newFileName 新文件名
     */
    suspend fun renameGroupFile(
        groupUin: Long,
        fileId: String,
        parentFolderId: String,
        newFileName: String
    ) = client.callService(
        RenameGroupFile,
        RenameGroupFile.Req(
            groupUin = groupUin,
            fileId = fileId,
            parentFolderId = parentFolderId,
            newFileName = newFileName
        )
    )

    /**
     * 移动群文件
     * @param groupUin 群号
     * @param fileId 文件 ID
     * @param parentFolderId 父文件夹 ID
     * @param targetFolderId 目标文件夹 ID
     */
    suspend fun moveGroupFile(
        groupUin: Long,
        fileId: String,
        parentFolderId: String,
        targetFolderId: String
    ) = client.callService(
        MoveGroupFile,
        MoveGroupFile.Req(
            groupUin = groupUin,
            fileId = fileId,
            parentFolderId = parentFolderId,
            targetFolderId = targetFolderId
        )
    )

    /**
     * 删除群文件
     * @param groupUin 群号
     * @param fileId 文件 ID
     */
    suspend fun deleteGroupFile(
        groupUin: Long, fileId: String
    ) = client.callService(
        DeleteGroupFile, DeleteGroupFile.Req(
            groupUin = groupUin, fileId = fileId
        )
    )

    /**
     * 创建群文件夹
     * @param groupUin 群号
     * @param folderName 文件夹名称
     * @return 文件夹 ID
     */
    suspend fun createGroupFolder(
        groupUin: Long,
        folderName: String
    ): String = client.callService(
        CreateGroupFolder,
        CreateGroupFolder.Req(
            groupUin = groupUin,
            folderName = folderName
        )
    ).folderId

    /**
     * 重命名群文件夹
     * @param groupUin 群号
     * @param folderId 文件夹 ID
     * @param newFolderName 新文件夹名称
     */
    suspend fun renameGroupFolder(
        groupUin: Long,
        folderId: String,
        newFolderName: String
    ) = client.callService(
        RenameGroupFolder,
        RenameGroupFolder.Req(
            groupUin = groupUin,
            folderId = folderId,
            newFolderName = newFolderName
        )
    )

    /**
     * 删除群文件夹
     * @param groupUin 群号
     * @param folderId 文件夹 ID
     */
    suspend fun deleteGroupFolder(
        groupUin: Long, folderId: String
    ) = client.callService(
        DeleteGroupFolder, DeleteGroupFolder.Req(
            groupUin = groupUin, folderId = folderId
        )
    )
}