package org.ntqqrev.acidify.internal.service.friend

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.misc.UserInfoKey
import org.ntqqrev.acidify.internal.proto.oidb.FetchFriendsCookie
import org.ntqqrev.acidify.internal.proto.oidb.IncPull
import org.ntqqrev.acidify.internal.proto.oidb.IncPullResp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.struct.BotFriendData
import org.ntqqrev.acidify.struct.UserInfoGender

internal object FetchFriends : OidbService<FetchFriends.Req, FetchFriends.Resp>(0xfd4, 1) {
    class Req(val nextUin: Long?)
    class Resp(
        val nextUin: Long?,
        val friendDataList: List<BotFriendData>,
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray = IncPull(
        reqCount = 300,
        cookie = FetchFriendsCookie(nextUin = payload.nextUin),
        flag = 1,
        requestBiz = listOf(
            IncPull.Biz(
                bizType = 1,
                bizData = IncPull.Biz.Busi(
                    extBusi = listOf(
                        UserInfoKey.BIO,
                        UserInfoKey.REMARK,
                        UserInfoKey.NICKNAME,
                        UserInfoKey.QID,
                        UserInfoKey.AGE,
                        UserInfoKey.GENDER
                    ).map { key -> key.number }
                )
            ),
            IncPull.Biz(
                bizType = 4,
                bizData = IncPull.Biz.Busi(
                    extBusi = listOf(100, 101, 102)
                )
            )
        )
    ).pbEncode()

    override fun parseOidb(client: LagrangeClient, payload: ByteArray): Resp {
        val resp = payload.pbDecode<IncPullResp>()
        val categories = resp.category.associate { it.categoryId to it.categoryName }
        return Resp(
            nextUin = resp.cookie?.nextUin,
            friendDataList = resp.friendList.map { friend ->
                val subBiz = friend.subBizMap
                    .find { it.key == 1 }!!
                    .value
                BotFriendData(
                    uin = friend.uin,
                    uid = friend.uid,
                    nickname = subBiz.stringProps[UserInfoKey.NICKNAME.number] ?: "",
                    remark = subBiz.stringProps[UserInfoKey.REMARK.number] ?: "",
                    bio = subBiz.stringProps[UserInfoKey.BIO.number] ?: "",
                    qid = subBiz.stringProps[UserInfoKey.QID.number] ?: "",
                    age = subBiz.numberProps[UserInfoKey.AGE.number] ?: 0,
                    gender = subBiz.numberProps[UserInfoKey.GENDER.number]?.let { UserInfoGender.from(it) }
                        ?: UserInfoGender.UNKNOWN,
                    categoryId = friend.categoryId,
                    categoryName = categories[friend.categoryId] ?: ""
                )
            },
        )
    }
}
