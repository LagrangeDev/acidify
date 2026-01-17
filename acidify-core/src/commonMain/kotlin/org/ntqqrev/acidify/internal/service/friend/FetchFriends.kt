package org.ntqqrev.acidify.internal.service.friend

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.misc.UserInfoKey
import org.ntqqrev.acidify.internal.packet.oidb.FetchFriendsCookie
import org.ntqqrev.acidify.internal.packet.oidb.IncPull
import org.ntqqrev.acidify.internal.packet.oidb.IncPullResp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.struct.BotFriendData
import org.ntqqrev.acidify.struct.UserInfoGender

internal object FetchFriends : OidbService<FetchFriends.Req, FetchFriends.Resp>(0xfd4, 1) {
    class Req(val nextUin: Long?)
    class Resp(
        val nextUin: Long?,
        val friendDataList: List<BotFriendData>,
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray = IncPull {
        it[reqCount] = 300
        it[cookie] = FetchFriendsCookie {
            it[nextUin] = payload.nextUin
        }
        it[flag] = 1
        it[requestBiz] = listOf(
            IncPull.Biz {
                it[bizType] = 1
                it[bizData] = IncPull.Biz.Busi {
                    it[extBusi] = listOf(
                        UserInfoKey.BIO,
                        UserInfoKey.REMARK,
                        UserInfoKey.NICKNAME,
                        UserInfoKey.QID,
                        UserInfoKey.AGE,
                        UserInfoKey.GENDER
                    ).map { key -> key.number }
                }
            },
            IncPull.Biz {
                it[bizType] = 4
                it[bizData] = IncPull.Biz.Busi {
                    it[extBusi] = listOf(100, 101, 102)
                }
            }
        )
    }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray): Resp {
        val resp = IncPullResp(payload)
        val categories = resp.get { category }.associate { it.get { categoryId } to it.get { categoryName } }
        return Resp(
            nextUin = resp.get { cookie }?.get { nextUin },
            friendDataList = resp.get { friendList }.map { friend ->
                val subBiz = friend.get { subBizMap }
                    .find { it.get { key } == 1 }!!
                    .get { value }
                val numMap = subBiz.get { numDataMap }.associate { it.get { key } to it.get { value } }
                val strMap = subBiz.get { dataMap }.associate { it.get { key } to it.get { value } }
                BotFriendData(
                    uin = friend.get { uin },
                    uid = friend.get { uid },
                    nickname = strMap[UserInfoKey.NICKNAME.number] ?: "",
                    remark = strMap[UserInfoKey.REMARK.number] ?: "",
                    bio = strMap[UserInfoKey.BIO.number] ?: "",
                    qid = strMap[UserInfoKey.QID.number] ?: "",
                    age = numMap[UserInfoKey.AGE.number] ?: 0,
                    gender = numMap[UserInfoKey.GENDER.number]?.let { UserInfoGender.from(it) }
                        ?: UserInfoGender.UNKNOWN,
                    categoryId = friend.get { categoryId },
                    categoryName = categories[friend.get { categoryId }] ?: ""
                )
            },
        )
    }
}