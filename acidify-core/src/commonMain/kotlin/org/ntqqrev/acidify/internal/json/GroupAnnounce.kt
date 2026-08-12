package org.ntqqrev.acidify.internal.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class GroupAnnounceFeed(
    @SerialName("fid") val noticeId: String = "",
    @SerialName("u") val senderId: Long = 0,
    @SerialName("pubt") val publishTime: Long = 0,
    @SerialName("msg") val message: Message = Message(),
    @SerialName("type") val type: Int = 0,
    @SerialName("settings") val settings: Settings = Settings(),
    @SerialName("pinned") val pinned: Int = 0,
) {
    @Serializable
    internal class Message(
        @SerialName("text") val text: String = "",
        @SerialName("pics") val images: List<Image> = emptyList()
    ) {
        @Serializable
        internal class Image(
            @SerialName("h") val height: String = "",
            @SerialName("w") val width: String = "",
            @SerialName("id") val id: String = ""
        )
    }

    @Serializable
    internal class Settings(
        @SerialName("is_show_edit_card") val showEditCard: Int = 0,
        @SerialName("tip_window_type") val tipWindowType: Int = 0,
        @SerialName("confirm_required") val confirmRequired: Int = 0,
    )
}

@Serializable
internal class GroupAnnounceResponse(
    @SerialName("feeds") val feeds: List<GroupAnnounceFeed> = emptyList(),
    @SerialName("inst") val inst: List<GroupAnnounceFeed> = emptyList()
)

@Serializable
internal class GroupAnnounceSendResponse(
    @SerialName("new_fid") val noticeId: String = ""
)

@Serializable
internal class GroupAnnounceUploadResponse(
    @SerialName("ec") val errorCode: Int = -1,
    @SerialName("id") val imageInfo: String = ""
)