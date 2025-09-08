package org.ntqqrev.acidify.common.struct

import org.ntqqrev.acidify.common.enum.UserInfoGender

/**
 * Bot 好友数据
 * @property uin 好友的 uin（QQ 号）
 * @property uid 好友的 uid
 * @property nickname 好友的昵称
 * @property remark 好友的备注
 * @property bio 好友的个性签名
 * @property qid 好友的 qid
 * @property age 好友的年龄
 * @property gender 好友的性别
 * @property categoryId 好友所在的分组 ID
 */
data class BotFriendData(
    val uin: Long,
    val uid: String,
    val nickname: String,
    val remark: String,
    val bio: String,
    val qid: String,
    val age: Int,
    val gender: UserInfoGender,
    val categoryId: Int,
)