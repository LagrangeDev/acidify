package org.ntqqrev.acidify.internal.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class GroupAnnounceImage(
    @SerialName("h") val height: String = "",
    @SerialName("w") val width: String = "",
    @SerialName("id") val id: String = ""
)

@Serializable
internal class GroupAnnounceMessage(
    @SerialName("text") val text: String = "",
    @SerialName("pics") val images: List<GroupAnnounceImage> = emptyList()
)

@Serializable
internal class GroupAnnounceFeed(
    @SerialName("fid") val noticeId: String = "",
    @SerialName("u") val senderId: Long = 0,
    @SerialName("pubt") val publishTime: Long = 0,
    @SerialName("msg") val message: GroupAnnounceMessage = GroupAnnounceMessage()
)

@Serializable
internal class GroupAnnounceResponse(
    @SerialName("feeds") val feeds: List<GroupAnnounceFeed> = emptyList(),
    @SerialName("inst") val inst: List<GroupAnnounceFeed> = emptyList()
)

@Serializable
internal class GroupAnnounceSendResponse(
    @SerialName("new_fid") val noticeId: String = ""
)