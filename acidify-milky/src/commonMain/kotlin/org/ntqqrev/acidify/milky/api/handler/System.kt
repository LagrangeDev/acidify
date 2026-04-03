package org.ntqqrev.acidify.milky.api.handler

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.ntqqrev.acidify.*
import org.ntqqrev.acidify.milky.api.MilkyApiException
import org.ntqqrev.acidify.milky.api.define
import org.ntqqrev.acidify.milky.transform.toMilkyEntity
import org.ntqqrev.acidify.milky.transform.toMilkyOutput
import org.ntqqrev.milky.*

val GetLoginInfo = ApiEndpoint.GetLoginInfo.define {
    GetLoginInfoOutput(
        uin = bot.uin,
        nickname = bot.fetchUserInfoByUid(bot.uid).nickname
    )
}

val GetImplInfo = ApiEndpoint.GetImplInfo.define {
    GetImplInfoOutput(
        implName = implName,
        implVersion = implVersion,
        qqProtocolVersion = when (bot) {
            is Bot -> (bot as Bot).appInfo.currentVersion
            is AndroidBot -> (bot as AndroidBot).appInfo.ptVersion
        },
        qqProtocolType = when (protocolOs) {
            "Windows" -> "windows"
            "Linux" -> "linux"
            "Mac" -> "macos"
            "AndroidPhone" -> "android_phone"
            "AndroidPad" -> "android_pad"
            else -> throw MilkyApiException(-400, "Unknown protocol OS: $protocolOs")
        },
        milkyVersion = milkyVersion,
    )
}

val GetUserProfile = ApiEndpoint.GetUserProfile.define {
    bot.fetchUserInfoByUin(it.userId).toMilkyOutput()
}

val GetFriendList = ApiEndpoint.GetFriendList.define {
    val friends = bot.getFriends(forceUpdate = it.noCache)
    GetFriendListOutput(
        friends = friends.map { friend -> friend.toMilkyEntity() }
    )
}

val GetFriendInfo = ApiEndpoint.GetFriendInfo.define {
    val friend = bot.getFriend(it.userId, forceUpdate = it.noCache)
        ?: throw MilkyApiException(-404, "Friend not found")
    GetFriendInfoOutput(
        friend = friend.toMilkyEntity()
    )
}

val GetGroupList = ApiEndpoint.GetGroupList.define {
    val groups = bot.getGroups(forceUpdate = it.noCache)
    GetGroupListOutput(
        groups = groups.map { group -> group.toMilkyEntity() }
    )
}

val GetGroupInfo = ApiEndpoint.GetGroupInfo.define {
    val group = bot.getGroup(it.groupId, forceUpdate = it.noCache)
        ?: throw MilkyApiException(-404, "Group not found")
    GetGroupInfoOutput(
        group = group.toMilkyEntity()
    )
}

val GetGroupMemberList = ApiEndpoint.GetGroupMemberList.define {
    val group = bot.getGroup(it.groupId)
        ?: throw MilkyApiException(-404, "Group not found")
    val members = group.getMembers(forceUpdate = it.noCache)
    GetGroupMemberListOutput(
        members = members.map { member -> member.toMilkyEntity() }
    )
}

val GetGroupMemberInfo = ApiEndpoint.GetGroupMemberInfo.define {
    val group = bot.getGroup(it.groupId)
        ?: throw MilkyApiException(-404, "Group not found")
    val member = group.getMember(it.userId, forceUpdate = it.noCache)
        ?: throw MilkyApiException(-404, "Group member not found")
    GetGroupMemberInfoOutput(
        member = member.toMilkyEntity()
    )
}

val GetPeerPins = ApiEndpoint.GetPeerPins.define {
    val pins = bot.getPins()
    GetPeerPinsOutput(
        friends = pins.friendUins.map { friendUin ->
            async { bot.getFriend(friendUin)?.toMilkyEntity() }
        }.awaitAll().filterNotNull(),
        groups = pins.groupUins.map { groupUin ->
            async { bot.getGroup(groupUin)?.toMilkyEntity() }
        }.awaitAll().filterNotNull(),
    )
}

val SetPeerPin = ApiEndpoint.SetPeerPin.define {
    when (it.messageScene) {
        "friend" -> bot.setFriendPin(it.peerId, it.isPinned)
        "group" -> bot.setGroupPin(it.peerId, it.isPinned)
        else -> throw MilkyApiException(-400, "Unknown message scene: ${it.messageScene}")
    }
    SetPeerPinOutput()
}

val SetAvatar = ApiEndpoint.SetAvatar.define {
    bot.setAvatar(resolveUri(it.uri))
    SetAvatarOutput()
}

val SetNickname = ApiEndpoint.SetNickname.define {
    bot.setNickname(it.newNickname)
    SetNicknameOutput()
}

val SetBio = ApiEndpoint.SetBio.define {
    bot.setBio(it.newBio)
    SetBioOutput()
}

val GetCustomFaceUrlList = ApiEndpoint.GetCustomFaceUrlList.define {
    GetCustomFaceUrlListOutput(bot.getCustomFaceUrl())
}

val GetCookies = ApiEndpoint.GetCookies.define {
    GetCookiesOutput(
        cookies = bot.getCookies(it.domain).entries
            .joinToString("; ") { "${it.key}=${it.value}" }
    )
}

val GetCsrfToken = ApiEndpoint.GetCsrfToken.define {
    GetCsrfTokenOutput(
        csrfToken = bot.getCsrfToken().toString()
    )
}