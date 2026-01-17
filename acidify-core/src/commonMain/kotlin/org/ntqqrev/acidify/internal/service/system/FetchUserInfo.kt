package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.misc.UserInfoKey
import org.ntqqrev.acidify.internal.packet.oidb.FetchUserInfoByUidReq
import org.ntqqrev.acidify.internal.packet.oidb.FetchUserInfoByUinReq
import org.ntqqrev.acidify.internal.packet.oidb.FetchUserInfoReqKey
import org.ntqqrev.acidify.internal.packet.oidb.FetchUserInfoResp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService
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
    ).map { enumKey -> FetchUserInfoReqKey { it[key] = enumKey.number } }

    private fun parseUserInfo(payload: ByteArray): BotUserInfo {
        val body = FetchUserInfoResp(payload).get { body }
        val properties = body.get { properties }
        val numMap = properties.get { numberProps }.associate { it.get { key } to it.get { value } }
        val strMap = properties.get { stringProps }.associate { it.get { key } to it.get { value } }
        return BotUserInfo(
            uin = body.get { uin },
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
        override fun buildOidb(client: IClient, payload: Long): ByteArray = FetchUserInfoByUinReq {
            it[uin] = payload
            it[keys] = fetchKeys
        }.toByteArray()

        override fun parseOidb(client: IClient, payload: ByteArray): BotUserInfo =
            parseUserInfo(payload)
    }

    internal object ByUid : OidbService<String, BotUserInfo>(0xfe1, 2) {
        override fun buildOidb(client: IClient, payload: String): ByteArray = FetchUserInfoByUidReq {
            it[uid] = payload
            it[keys] = fetchKeys
        }.toByteArray()

        override fun parseOidb(client: IClient, payload: ByteArray): BotUserInfo =
            parseUserInfo(payload)
    }
}