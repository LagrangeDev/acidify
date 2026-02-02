package org.ntqqrev.acidify.internal.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal class GroupEssenceMsgItem(
    @SerialName("group_code") val groupCode: String = "",
    @SerialName("msg_seq") val msgSeq: Long = 0,
    @SerialName("msg_random") val msgRandom: Long = 0,
    @SerialName("sender_uin") val senderUin: String = "",
    @SerialName("sender_nick") val senderNick: String = "",
    @SerialName("sender_time") val senderTime: Long = 0,
    @SerialName("add_digest_uin") val addDigestUin: String = "",
    @SerialName("add_digest_nick") val addDigestNick: String = "",
    @SerialName("add_digest_time") val addDigestTime: Long = 0,
    @SerialName("msg_content") val msgContent: List<JsonObject> = emptyList(),
    @SerialName("can_be_removed") val canBeRemoved: Boolean = false
)

@Serializable
internal class GroupEssenceData(
    @SerialName("msg_list") val msgList: List<GroupEssenceMsgItem>? = null,
    @SerialName("is_end") val isEnd: Boolean = false,
    @SerialName("group_role") val groupRole: Int = 0,
    @SerialName("config_page_url") val configPageUrl: String? = null
)

@Serializable
internal class GroupEssenceResponse(
    @SerialName("retcode") val retcode: Int = 0,
    @SerialName("retmsg") val retmsg: String = "",
    @SerialName("data") val data: GroupEssenceData = GroupEssenceData()
)