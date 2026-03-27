package org.ntqqrev.acidify

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import org.ntqqrev.acidify.internal.proto.misc.UserInfoKey
import org.ntqqrev.acidify.internal.service.friend.FetchFriends
import org.ntqqrev.acidify.internal.service.group.FetchGroupMembers
import org.ntqqrev.acidify.internal.service.group.FetchGroups
import org.ntqqrev.acidify.internal.service.system.*
import org.ntqqrev.acidify.struct.*

/**
 * 尝试使用现有的 Session 信息上线。
 * 请优先调用 [login]，该方法会在现有 Session 失效时自动调用 [qrCodeLogin] 或 [passwordLogin]。
 * 若确定 Session 有效且不希望进行二维码登录，可调用此方法。
 * @param preloadContacts 是否预加载好友和群信息以初始化内存缓存
 */
suspend fun AbstractBot.online(preloadContacts: Boolean = false) {
    client.sendOnlinePacket()
    isLoggedIn = true
    logger.i { "发送上线包成功" }
    client.doPostOnlineLogic()

    eventCollectJob = launch {
        while (currentCoroutineContext().isActive) {
            val sso = client.pushChannel.receive()
            val signal = signals[sso.command]
            if (signal != null) {
                try {
                    val parsed = signal.parse(this@online, sso.response)
                    logger.v { "由 ${sso.command} 解析出事件：$parsed" }
                    parsed.forEach { sharedEventFlow.emit(it) }
                } catch (e: Exception) {
                    logger.e(e) { "处理信令 ${sso.command} 时出现错误" }
                }
            }
        }
    }

    refreshFaceDetailsMap()

    if (preloadContacts) {
        // Preload friends, groups and group members to initialize in-memory cache
        val friendCount = getFriends().size
        val groups = getGroups()
        val groupCount = groups.size
        val groupMemberCount = groups.sumOf { it.getMembers().size }
        logger.d { "加载了 $friendCount 个好友, $groupCount 个群和 $groupMemberCount 个群成员" }
    }

    logger.i { "用户 $uin 已上线" }
}

/**
 * 下线 Bot，释放资源。
 */
suspend fun AbstractBot.offline() {
    client.doPreOfflineLogic()
    eventCollectJob?.cancel()
    eventCollectJob = null
    client.callService(BotOffline)
    logger.i { "用户 $uin 已下线" }
    client.packetContext.closeConnection()
}

/**
 * 通过 QQ 号获取用户信息。
 */
suspend fun AbstractBot.fetchUserInfoByUin(uin: Long) = client.callService(FetchUserInfo.ByUin, uin)

/**
 * 通过 uid 获取用户信息。
 */
suspend fun AbstractBot.fetchUserInfoByUid(uid: String): BotUserInfo {
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
suspend fun AbstractBot.fetchFriends(): List<BotFriendData> {
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
suspend fun AbstractBot.fetchGroups(): List<BotGroupData> {
    return client.callService(FetchGroups)
}

/**
 * 拉取指定群的成员信息。此操作不会被缓存。
 */
suspend fun AbstractBot.fetchGroupMembers(groupUin: Long): List<BotGroupMemberData> {
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
suspend fun AbstractBot.getFriends(forceUpdate: Boolean = false) = friendCache.getAll(forceUpdate)

/**
 * 根据 uin 获取好友实体。
 * @param uin 好友的 QQ 号
 * @param forceUpdate 是否强制更新缓存
 */
suspend fun AbstractBot.getFriend(uin: Long, forceUpdate: Boolean = false) = friendCache.get(uin, forceUpdate)

/**
 * 获取所有群实体。
 * @param forceUpdate 是否强制更新缓存
 */
suspend fun AbstractBot.getGroups(forceUpdate: Boolean = false) = groupCache.getAll(forceUpdate)

/**
 * 根据 uin 获取群实体。
 * @param uin 群号
 * @param forceUpdate 是否强制更新缓存
 */
suspend fun AbstractBot.getGroup(uin: Long, forceUpdate: Boolean = false) = groupCache.get(uin, forceUpdate)

/**
 * 获取指定群的所有群成员实体。
 * @param groupUin 群号
 * @param forceUpdate 是否强制更新缓存
 */
suspend fun AbstractBot.getGroupMembers(groupUin: Long, forceUpdate: Boolean = false) =
    getGroup(groupUin)?.getMembers(forceUpdate)

/**
 * 根据 uin 获取指定群的群成员实体。
 * @param groupUin 群号
 * @param memberUin 群成员的 QQ 号
 */
suspend fun AbstractBot.getGroupMember(groupUin: Long, memberUin: Long, forceUpdate: Boolean = false) =
    getGroup(groupUin)?.getMember(memberUin, forceUpdate)

/**
 * 解析 uid 到 QQ 号。
 * 如果之前未解析过该 uid，会发起网络请求获取用户信息。
 */
suspend fun AbstractBot.getUinByUid(uid: String): Long =
    idMapQueryMutex.withLock { uid2uinMap[uid] } ?: run {
        fetchUserInfoByUid(uid)
        idMapQueryMutex.withLock { uid2uinMap[uid]!! }
    }

/**
 * 解析 QQ 号到 uid，该过程可能失败，此时抛出 [NoSuchElementException]。
 * 若 [mayComeFromGroupUin] 非空且在缓存中未找到对应 uid，会尝试从该群的成员列表中查找；
 * 否则，会尝试从好友列表中查找。
 */
suspend fun AbstractBot.getUidByUin(uin: Long, mayComeFromGroupUin: Long? = null) =
    idMapQueryMutex.withLock { uin2uidMap[uin] } ?: run {
        if (mayComeFromGroupUin != null) {
            fetchGroupMembers(mayComeFromGroupUin)
        } else {
            fetchFriends()
        }
        idMapQueryMutex.withLock { uin2uidMap[uin] }
    } ?: throw NoSuchElementException("无法解析 uin $uin 对应的 uid")

/**
 * 刷新 [faceDetailMap]，从服务器获取最新的表情信息并更新内存缓存
 */
suspend fun AbstractBot.refreshFaceDetailsMap() {
    faceDetailMapMut = client.callService(FetchFaceDetails).associateBy { it.qSid }
    logger.d { "加载了 ${faceDetailMap.size} 条表情信息" }
}

/**
 * 获取收藏表情的直链 URL 列表
 */
suspend fun AbstractBot.getCustomFaceUrl(): List<String> = client.callService(FetchCustomFace)

/**
 * 获取当前置顶的好友与群聊
 */
suspend fun AbstractBot.getPins(): BotPinnedChats {
    val resp = client.callService(FetchPins)
    val friendUins = resp.friendUids.map {
        async { runCatching { getUinByUid(it) } }
    }.awaitAll().mapNotNull { it.getOrNull() }
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
suspend fun AbstractBot.setFriendPin(friendUin: Long, isPinned: Boolean) =
    client.callService(SetFriendPin, SetFriendPin.Req(getUidByUin(friendUin), isPinned))

/**
 * 设置群聊置顶状态
 * @param groupUin 群号
 * @param isPinned 是否置顶
 */
suspend fun AbstractBot.setGroupPin(groupUin: Long, isPinned: Boolean) =
    client.callService(SetGroupPin, SetGroupPin.Req(groupUin, isPinned))

/**
 * 设置账号头像
 * @param imageData 头像原始字节数据
 */
suspend fun AbstractBot.setAvatar(imageData: ByteArray) = client.highwayContext.uploadAvatar(imageData)

/**
 * 设置账号昵称
 */
suspend fun AbstractBot.setNickname(nickname: String) =
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
suspend fun AbstractBot.setBio(bio: String) =
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
suspend fun AbstractBot.getSKey() = client.ticketContext.getSKey()

/**
 * 获取给定域名的 p_skey，用于组成 Cookie。
 */
suspend fun AbstractBot.getPSKey(domain: String) = client.ticketContext.getPSKey(domain)

/**
 * 获取指定域名的 Cookie 键值对。
 */
suspend fun AbstractBot.getCookies(domain: String) = mapOf(
    "p_uin" to "o$uin",
    "p_skey" to getPSKey(domain),
    "skey" to getSKey(),
    "uin" to uin.toString(),
)

/**
 * 获取 CSRF Token。
 */
suspend fun AbstractBot.getCsrfToken() = client.ticketContext.getCsrfToken()