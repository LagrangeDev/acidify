package org.ntqqrev.acidify.entity

import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.struct.BotFriendData
import org.ntqqrev.acidify.struct.UserInfoGender
import kotlin.js.JsExport

/**
 * 好友实体
 */
@JsExport
class BotFriend internal constructor(
    bot: AbstractBot,
    data: BotFriendData,
) : BotEntity<BotFriendData>(bot, data) {

    /**
     * 好友的 QQ 号
     */
    val uin: Long
        get() = data.uin

    /**
     * 好友的 uid
     */
    val uid: String
        get() = data.uid

    /**
     * 好友的昵称
     */
    val nickname: String
        get() = data.nickname

    /**
     * 好友的备注
     */
    val remark: String
        get() = data.remark

    /**
     * 好友的个性签名
     */
    val bio: String
        get() = data.bio

    /**
     * 好友的 qid
     */
    val qid: String
        get() = data.qid

    /**
     * 好友的年龄
     */
    val age: Int
        get() = data.age

    /**
     * 好友的性别
     */
    val gender: UserInfoGender
        get() = data.gender

    /**
     * 好友所在的分组 ID
     */
    val categoryId: Int
        get() = data.categoryId

    /**
     * 好友所在的分组名称
     */
    val categoryName: String
        get() = data.categoryName

    override fun toString(): String {
        return "${remark.ifEmpty { nickname }} ($uin)"
    }
}