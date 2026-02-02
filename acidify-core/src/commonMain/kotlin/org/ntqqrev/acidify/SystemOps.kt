package org.ntqqrev.acidify

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.ntqqrev.acidify.event.QRCodeGeneratedEvent
import org.ntqqrev.acidify.event.QRCodeStateQueryEvent
import org.ntqqrev.acidify.event.SessionStoreUpdatedEvent
import org.ntqqrev.acidify.exception.BotOnlineException
import org.ntqqrev.acidify.internal.proto.misc.UserInfoKey
import org.ntqqrev.acidify.internal.service.friend.FetchFriends
import org.ntqqrev.acidify.internal.service.group.FetchGroupMembers
import org.ntqqrev.acidify.internal.service.group.FetchGroups
import org.ntqqrev.acidify.internal.service.system.*
import org.ntqqrev.acidify.struct.*

/**
 * 发起二维码登录请求。过程中会触发事件：
 * - [QRCodeGeneratedEvent]：当二维码生成时触发，包含二维码链接和 PNG 图片数据
 * - [QRCodeStateQueryEvent]：每次查询二维码状态时触发，包含当前二维码状态（例如未扫码、已扫码未确认、已确认等）
 * @param queryInterval 查询间隔（单位 ms），不能小于 `1000`
 * @param preloadContacts 是否在登录成功后预加载好友和群信息以初始化内存缓存
 * @throws org.ntqqrev.acidify.exception.WtLoginException 当二维码扫描成功，但后续登录失败时抛出
 * @throws IllegalStateException 当二维码过期或用户取消登录时抛出
 * @see QRCodeState
 */
suspend fun Bot.qrCodeLogin(queryInterval: Long = 3000L, preloadContacts: Boolean = false) {
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
    online(preloadContacts)
}

/**
 * 尝试使用现有的 Session 信息上线。
 * 请优先调用 [login]，该方法会在现有 Session 失效时自动调用 [qrCodeLogin]。
 * 若确定 Session 有效且不希望进行二维码登录，可调用此方法。
 * @param preloadContacts 是否预加载好友和群信息以初始化内存缓存
 */
suspend fun Bot.online(preloadContacts: Boolean = false) {
    val result = client.callService(BotOnline)
    if (result != "register success") {
        throw BotOnlineException(result)
    }
    isLoggedIn = true
    logger.i { "用户 $uin 已上线" }
    client.doPostOnlineLogic()

    eventCollectJob = launch {
        while (currentCoroutineContext().isActive) {
            val sso = client.pushChannel.receive()
            val signal = signals[sso.command]
            if (signal != null) {
                try {
                    val parsed = signal.parse(this@online, sso.response)
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

    if (preloadContacts) {
        // Preload friends, groups and group members to initialize in-memory cache
        val friendCount = getFriends().size
        val groups = getGroups()
        val groupCount = groups.size
        val groupMemberCount = groups.sumOf { it.getMembers().size }
        logger.d { "加载了 $friendCount 个好友, $groupCount 个群和 $groupMemberCount 个群成员" }
    }
}

/**
 * 下线 Bot，释放资源。
 */
suspend fun Bot.offline() {
    client.doPreOfflineLogic()
    eventCollectJob?.cancel()
    eventCollectJob = null
    client.callService(BotOffline)
    logger.i { "用户 $uin 已下线" }
    client.packetContext.closeConnection()
}

/**
 * 如果 Session 为空则调用 [qrCodeLogin] 进行登录。
 * 如果 Session 不为空则尝试使用现有的 Session 信息登录，若失败则调用 [qrCodeLogin] 重新登录。
 * @param queryInterval 查询间隔（单位 ms），不能小于 `1000`
 * @param preloadContacts 是否预加载好友和群信息以初始化内存缓存
 */
suspend fun Bot.login(queryInterval: Long = 3000L, preloadContacts: Boolean = false) {
    if (sessionStore.a2.isEmpty()) {
        logger.i { "Session 为空，尝试二维码登录" }
        qrCodeLogin(queryInterval, preloadContacts)
    } else {
        try {
            try {
                online(preloadContacts)
            } catch (e: Exception) {
                logger.w(e) { "使用现有 Session 登录失败，尝试刷新 DeviceGuid 后重新登录" }
                sessionStore.refreshDeviceGuid()
                online(preloadContacts)
            }
        } catch (e: Exception) {
            logger.w(e) { "使用现有 Session 登录失败，尝试二维码登录" }
            sessionStore.clear()
            // sharedEventFlow.emit(SessionStoreUpdatedEvent(sessionStore))
            qrCodeLogin(queryInterval, preloadContacts)
        }
    }
}

/**
 * 通过 QQ 号获取用户信息。
 */
suspend fun Bot.fetchUserInfoByUin(uin: Long) = client.callService(FetchUserInfo.ByUin, uin)

/**
 * 通过 uid 获取用户信息。
 */
suspend fun Bot.fetchUserInfoByUid(uid: String): BotUserInfo {
    val userInfo = client.callService(FetchUserInfo.ByUid, uid)
    idMapQueryMutex.withLock {
        uin2uidMap[userInfo.uin] = uid
        uid2uinMap[uid] = userInfo.uin
    }
    return userInfo
}

/**
 * 拉取好友与好友分组信息。此操作不会被缓存。
 */
suspend fun Bot.fetchFriends(): List<BotFriendData> {
    var nextUin: Long? = null
    val friendDataResult = mutableListOf<BotFriendData>()
    do {
        val resp = client.callService(FetchFriends, FetchFriends.Req(nextUin))
        nextUin = resp.nextUin
        friendDataResult.addAll(resp.friendDataList)
    } while (nextUin != null)
    idMapQueryMutex.withLock {
        friendDataResult.forEach { data ->
            uin2uidMap[data.uin] = data.uid
            uid2uinMap[data.uid] = data.uin
        }
    }
    return friendDataResult
}

/**
 * 拉取群信息。此操作不会被缓存。
 */
suspend fun Bot.fetchGroups(): List<BotGroupData> {
    return client.callService(FetchGroups)
}

/**
 * 拉取指定群的成员信息。此操作不会被缓存。
 */
suspend fun Bot.fetchGroupMembers(groupUin: Long): List<BotGroupMemberData> {
    var cookie: ByteArray? = null
    val memberDataResult = mutableListOf<BotGroupMemberData>()
    do {
        val resp = client.callService(FetchGroupMembers, FetchGroupMembers.Req(groupUin, cookie))
        cookie = resp.cookie
        memberDataResult.addAll(resp.memberDataList)
    } while (cookie != null)
    idMapQueryMutex.withLock {
        memberDataResult.forEach { data ->
            uin2uidMap[data.uin] = data.uid
            uid2uinMap[data.uid] = data.uin
        }
    }
    return memberDataResult
}

/**
 * 获取所有好友实体。
 * @param forceUpdate 是否强制更新缓存
 */
suspend fun Bot.getFriends(forceUpdate: Boolean = false) = friendCache.getAll(forceUpdate)

/**
 * 根据 uin 获取好友实体。
 * @param uin 好友的 QQ 号
 * @param forceUpdate 是否强制更新缓存
 */
suspend fun Bot.getFriend(uin: Long, forceUpdate: Boolean = false) = friendCache.get(uin, forceUpdate)

/**
 * 获取所有群实体。
 * @param forceUpdate 是否强制更新缓存
 */
suspend fun Bot.getGroups(forceUpdate: Boolean = false) = groupCache.getAll(forceUpdate)

/**
 * 根据 uin 获取群实体。
 * @param uin 群号
 * @param forceUpdate 是否强制更新缓存
 */
suspend fun Bot.getGroup(uin: Long, forceUpdate: Boolean = false) = groupCache.get(uin, forceUpdate)

/**
 * 获取指定群的所有群成员实体。
 * @param groupUin 群号
 * @param forceUpdate 是否强制更新缓存
 */
suspend fun Bot.getGroupMembers(groupUin: Long, forceUpdate: Boolean = false) =
    getGroup(groupUin)?.getMembers(forceUpdate)

/**
 * 根据 uin 获取指定群的群成员实体。
 * @param groupUin 群号
 * @param memberUin 群成员的 QQ 号
 */
suspend fun Bot.getGroupMember(groupUin: Long, memberUin: Long, forceUpdate: Boolean = false) =
    getGroup(groupUin)?.getMember(memberUin, forceUpdate)

/**
 * 解析 uid 到 QQ 号。
 * 如果之前未解析过该 uid，会发起网络请求获取用户信息。
 */
suspend fun Bot.getUinByUid(uid: String): Long =
    idMapQueryMutex.withLock { uid2uinMap[uid] } ?: run {
        fetchUserInfoByUid(uid)
        idMapQueryMutex.withLock { uid2uinMap[uid]!! }
    }

/**
 * 解析 QQ 号到 uid，该过程可能失败，此时抛出 [NoSuchElementException]。
 * 若 [mayComeFromGroupUin] 非空且在缓存中未找到对应 uid，会尝试从该群的成员列表中查找；
 * 否则，会尝试从好友列表中查找。
 */
suspend fun Bot.getUidByUin(uin: Long, mayComeFromGroupUin: Long? = null) =
    idMapQueryMutex.withLock { uin2uidMap[uin] } ?: run {
        if (mayComeFromGroupUin != null) {
            fetchGroupMembers(mayComeFromGroupUin)
        } else {
            fetchFriends()
        }
        idMapQueryMutex.withLock { uin2uidMap[uin] }
    } ?: throw NoSuchElementException("无法解析 uin $uin 对应的 uid")

/**
 * 获取收藏表情的直链 URL 列表
 */
suspend fun Bot.getCustomFaceUrl(): List<String> = client.callService(FetchCustomFace)

/**
 * 获取当前置顶的好友与群聊
 */
suspend fun Bot.getPins(): BotPinnedChats {
    val resp = client.callService(FetchPins)
    val friendUins = resp.friendUids.mapNotNull {
        runCatching { getUinByUid(it) }.getOrNull()
    }
    return BotPinnedChats(
        friendUins = friendUins,
        groupUins = resp.groupUins
    )
}

/**
 * 设置好友置顶状态
 * @param friendUin 好友 QQ 号
 * @param isPinned 是否置顶
 */
suspend fun Bot.setFriendPin(friendUin: Long, isPinned: Boolean) =
    client.callService(SetFriendPin, SetFriendPin.Req(getUidByUin(friendUin), isPinned))

/**
 * 设置群聊置顶状态
 * @param groupUin 群号
 * @param isPinned 是否置顶
 */
suspend fun Bot.setGroupPin(groupUin: Long, isPinned: Boolean) =
    client.callService(SetGroupPin, SetGroupPin.Req(groupUin, isPinned))

/**
 * 设置账号头像
 * @param imageData 头像原始字节数据
 */
suspend fun Bot.setAvatar(imageData: ByteArray) = client.highwayContext.uploadAvatar(imageData)

/**
 * 设置账号昵称
 */
suspend fun Bot.setNickname(nickname: String) =
    client.callService(
        SetUserProfile, SetUserProfile.Req(
            stringProps = mapOf(
                UserInfoKey.NICKNAME to nickname
            )
        )
    )

/**
 * 设置账号个性签名
 */
suspend fun Bot.setBio(bio: String) =
    client.callService(
        SetUserProfile, SetUserProfile.Req(
            stringProps = mapOf(
                UserInfoKey.BIO to bio
            )
        )
    )

/**
 * 获取 s_key，用于组成 Cookie。
 */
suspend fun Bot.getSKey() = client.ticketContext.getSKey()

/**
 * 获取给定域名的 p_skey，用于组成 Cookie。
 */
suspend fun Bot.getPSKey(domain: String) = client.ticketContext.getPSKey(domain)

/**
 * 获取指定域名的 Cookie 键值对。
 */
suspend fun Bot.getCookies(domain: String) = mapOf(
    "p_uin" to "o$uin",
    "p_skey" to getPSKey(domain),
    "skey" to getSKey(),
    "uin" to uin.toString(),
)

/**
 * 获取 CSRF Token。
 */
suspend fun Bot.getCsrfToken() = client.ticketContext.getCsrfToken()