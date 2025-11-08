package org.ntqqrev.acidify.struct

import kotlin.js.JsExport

/**
 * 用户信息
 * @property uin 用户的 QQ 号
 * @property nickname 用户的昵称
 * @property bio 用户的个性签名
 * @property gender 用户的性别
 * @property remark 用户的备注
 * @property level 用户的 QQ 等级
 * @property country 用户所在国家
 * @property city 用户所在城市
 * @property school 用户所在学校
 * @property registerTime 用户注册的 Unix 时间戳（秒）
 * @property age 用户的年龄
 * @property qid 用户的 qid
 */
@JsExport
data class BotUserInfo(
    val uin: Long,
    val nickname: String,
    val bio: String,
    val gender: UserInfoGender,
    val remark: String,
    val level: Int,
    val country: String,
    val city: String,
    val school: String,
    val registerTime: Long,
    val age: Int,
    val qid: String,
)