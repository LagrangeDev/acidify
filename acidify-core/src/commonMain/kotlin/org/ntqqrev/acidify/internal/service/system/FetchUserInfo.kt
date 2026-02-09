package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.AbstractClient
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
        return BotUserInfo(
            uin = body.uin,
            nickname = properties.stringProps[UserInfoKey.NICKNAME.number] ?: "",
            bio = properties.stringProps[UserInfoKey.BIO.number] ?: "",
            gender = properties.numberProps[UserInfoKey.GENDER.number]?.let { UserInfoGender.from(it) }
                ?: UserInfoGender.UNKNOWN,
            remark = properties.stringProps[UserInfoKey.REMARK.number] ?: "",
            level = properties.numberProps[UserInfoKey.LEVEL.number] ?: 0,
            country = properties.stringProps[UserInfoKey.COUNTRY.number] ?: "",
            city = properties.stringProps[UserInfoKey.CITY.number] ?: "",
            school = properties.stringProps[UserInfoKey.SCHOOL.number] ?: "",
            registerTime = properties.numberProps[UserInfoKey.REGISTER_TIME.number]?.toLong() ?: 0L,
            age = properties.numberProps[UserInfoKey.AGE.number] ?: 0,
            qid = properties.stringProps[UserInfoKey.QID.number] ?: "",
        )
    }

    internal object ByUin : OidbService<Long, BotUserInfo>(0xfe1, 2, true) {
        override fun buildOidb(client: AbstractClient, payload: Long): ByteArray = FetchUserInfoByUinReq(
            uin = payload,
            keys = fetchKeys,
        ).pbEncode()

        override fun parseOidb(client: AbstractClient, payload: ByteArray): BotUserInfo =
            parseUserInfo(payload)
    }

    internal object ByUid : OidbService<String, BotUserInfo>(0xfe1, 2) {
        override fun buildOidb(client: AbstractClient, payload: String): ByteArray = FetchUserInfoByUidReq(
            uid = payload,
            keys = fetchKeys,
        ).pbEncode()

        override fun parseOidb(client: AbstractClient, payload: ByteArray): BotUserInfo =
            parseUserInfo(payload)
    }
}
