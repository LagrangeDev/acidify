package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.misc.UserInfoKey
import org.ntqqrev.acidify.internal.proto.oidb.FetchUserInfoByUidReq
import org.ntqqrev.acidify.internal.proto.oidb.FetchUserInfoByUinReq
import org.ntqqrev.acidify.internal.proto.oidb.FetchUserInfoReqKey
import org.ntqqrev.acidify.internal.proto.oidb.FetchUserInfoResp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.struct.BotUserInfo
import org.ntqqrev.acidify.struct.UserInfoGender

internal object FetchUserInfo {
    private val fetchKeys = listOf(
        UserInfoKey.NICKNAME,
        UserInfoKey.BIO,
        UserInfoKey.GENDER,
        UserInfoKey.REMARK,
        UserInfoKey.LEVEL,
        UserInfoKey.COUNTRY,
        UserInfoKey.CITY,
        UserInfoKey.SCHOOL,
        UserInfoKey.REGISTER_TIME,
        UserInfoKey.AGE,
        UserInfoKey.QID,
    ).map { enumKey -> FetchUserInfoReqKey(key = enumKey.number) }

    private fun parseUserInfo(payload: ByteArray): BotUserInfo {
        val body = payload.pbDecode<FetchUserInfoResp>().body
        val properties = body.properties
        val numMap = properties.numberProps.associate { it.key to it.value }
        val strMap = properties.stringProps.associate { it.key to it.value }
        return BotUserInfo(
            uin = body.uin,
            nickname = strMap[UserInfoKey.NICKNAME.number] ?: "",
            bio = strMap[UserInfoKey.BIO.number] ?: "",
            gender = numMap[UserInfoKey.GENDER.number]?.let { UserInfoGender.from(it) }
                ?: UserInfoGender.UNKNOWN,
            remark = strMap[UserInfoKey.REMARK.number] ?: "",
            level = numMap[UserInfoKey.LEVEL.number] ?: 0,
            country = strMap[UserInfoKey.COUNTRY.number] ?: "",
            city = strMap[UserInfoKey.CITY.number] ?: "",
            school = strMap[UserInfoKey.SCHOOL.number] ?: "",
            registerTime = numMap[UserInfoKey.REGISTER_TIME.number]?.toLong() ?: 0L,
            age = numMap[UserInfoKey.AGE.number] ?: 0,
            qid = strMap[UserInfoKey.QID.number] ?: "",
        )
    }

    internal object ByUin : OidbService<Long, BotUserInfo>(0xfe1, 2, true) {
        override fun buildOidb(client: LagrangeClient, payload: Long): ByteArray = FetchUserInfoByUinReq(
            uin = payload,
            keys = fetchKeys,
        ).pbEncode()

        override fun parseOidb(client: LagrangeClient, payload: ByteArray): BotUserInfo =
            parseUserInfo(payload)
    }

    internal object ByUid : OidbService<String, BotUserInfo>(0xfe1, 2) {
        override fun buildOidb(client: LagrangeClient, payload: String): ByteArray = FetchUserInfoByUidReq(
            uid = payload,
            keys = fetchKeys,
        ).pbEncode()

        override fun parseOidb(client: LagrangeClient, payload: ByteArray): BotUserInfo =
            parseUserInfo(payload)
    }
}
