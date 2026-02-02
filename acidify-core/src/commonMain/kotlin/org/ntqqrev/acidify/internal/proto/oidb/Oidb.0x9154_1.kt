package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FetchFaceDetailsReq(
    @ProtoNumber(1) val field1: Int = 0,
    @ProtoNumber(2) val field2: Int = 0,
    @ProtoNumber(3) val field3: String = "",
)

@Serializable
internal class FetchFaceDetailsResp(
    @ProtoNumber(1) val field1: Int = 0,
    @ProtoNumber(2) val commonFace: ResponseContent = ResponseContent(),
    @ProtoNumber(3) val specialBigFace: ResponseContent = ResponseContent(),
    @ProtoNumber(4) val specialMagicFace: MagicEmojiContent = MagicEmojiContent(),
) {
    @Serializable
    internal class ResponseContent(
        @ProtoNumber(1) val emojiList: List<EmojiList> = emptyList(),
        @ProtoNumber(2) val resourceUrl: ResourceUrl = ResourceUrl(),
    )

    @Serializable
    internal class MagicEmojiContent(
        @ProtoNumber(1) val field1: MagicEmojiList = MagicEmojiList(),
        @ProtoNumber(2) val resourceUrl: ResourceUrl = ResourceUrl(),
    )

    @Serializable
    internal class MagicEmojiList(
        @ProtoNumber(2) val emojiList: List<Emoji> = emptyList(),
    )

    @Serializable
    internal class EmojiList(
        @ProtoNumber(1) val emojiPackName: String = "",
        @ProtoNumber(2) val emojiDetail: List<Emoji> = emptyList(),
    )

    @Serializable
    internal class Emoji(
        @ProtoNumber(1) val qSid: String = "",
        @ProtoNumber(2) val qDes: String = "",
        @ProtoNumber(3) val emCode: String = "",
        @ProtoNumber(4) val qCid: Int = 0,
        @ProtoNumber(5) val aniStickerType: Int = 0,
        @ProtoNumber(6) val aniStickerPackId: Int = 0,
        @ProtoNumber(7) val aniStickerId: Int = 0,
        @ProtoNumber(8) val url: ResourceUrl = ResourceUrl(),
        @ProtoNumber(9) val emojiNameAlias: List<String> = emptyList(),
        @ProtoNumber(10) val unknown10: Int = 0,
        @ProtoNumber(13) val aniStickerWidth: Int = 0,
        @ProtoNumber(14) val aniStickerHeight: Int = 0,
    )

    @Serializable
    internal class ResourceUrl(
        @ProtoNumber(1) val baseUrl: String = "",
        @ProtoNumber(2) val advUrl: String = "",
    )
}
