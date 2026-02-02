package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.FetchFaceDetailsReq
import org.ntqqrev.acidify.internal.proto.oidb.FetchFaceDetailsResp
import org.ntqqrev.acidify.internal.service.NoInputOidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.struct.BotFaceDetail

internal object FetchFaceDetails : NoInputOidbService<List<BotFaceDetail>>(0x9154, 1) {
    override fun buildOidb(client: LagrangeClient, payload: Unit): ByteArray = FetchFaceDetailsReq(
        field1 = 0,
        field2 = 7,
        field3 = "0",
    ).pbEncode()

    override fun parseOidb(client: LagrangeClient, payload: ByteArray): List<BotFaceDetail> {
        val resp = payload.pbDecode<FetchFaceDetailsResp>()

        val common = resp.commonFace
            .emojiList
            .flatMap { it.emojiDetail }
            .map { it.toBotFaceEntry() }

        val big = resp.specialBigFace
            .emojiList
            .flatMap { it.emojiDetail }
            .map { it.toBotFaceEntry() }

        val magic = resp.specialMagicFace
            .field1
            .emojiList
            .map { it.toBotFaceEntry() }

        return common + big + magic
    }

    private fun FetchFaceDetailsResp.Emoji.toBotFaceEntry() = BotFaceDetail(
        qSid = qSid,
        qDes = qDes,
        emCode = emCode,
        qCid = qCid,
        aniStickerType = aniStickerType,
        aniStickerPackId = aniStickerPackId,
        aniStickerId = aniStickerId,
        url = url.baseUrl,
        emojiNameAlias = emojiNameAlias,
        aniStickerWidth = aniStickerWidth,
        aniStickerHeight = aniStickerHeight,
    )
}
