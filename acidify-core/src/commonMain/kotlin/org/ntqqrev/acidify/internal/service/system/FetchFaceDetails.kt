package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.FetchFaceDetailsReq
import org.ntqqrev.acidify.internal.packet.oidb.FetchFaceDetailsResp
import org.ntqqrev.acidify.internal.protobuf.PbObject
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoInputOidbService
import org.ntqqrev.acidify.struct.BotFaceDetail

internal object FetchFaceDetails : NoInputOidbService<List<BotFaceDetail>>(0x9154, 1) {
    override fun buildOidb(client: IClient, payload: Unit): ByteArray = FetchFaceDetailsReq {
        it[field1] = 0
        it[field2] = 7
        it[field3] = "0"
    }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray): List<BotFaceDetail> {
        val resp = FetchFaceDetailsResp(payload)

        val common = resp.get { commonFace }
            .get { emojiList }
            .flatMap { it.get { emojiDetail } }
            .map { it.toBotFaceEntry() }

        val big = resp.get { specialBigFace }
            .get { emojiList }
            .flatMap { it.get { emojiDetail } }
            .map { it.toBotFaceEntry() }

        val magic = resp.get { specialMagicFace }
            .get { field1 }
            .get { emojiList }
            .map { it.toBotFaceEntry() }

        return common + big + magic
    }

    private fun PbObject<FetchFaceDetailsResp.Emoji>.toBotFaceEntry() = BotFaceDetail(
        qSid = this.get { qSid },
        qDes = this.get { qDes },
        emCode = this.get { emCode },
        qCid = this.get { qCid },
        aniStickerType = this.get { aniStickerType },
        aniStickerPackId = this.get { aniStickerPackId },
        aniStickerId = this.get { aniStickerId },
        url = this.get { url }.get { baseUrl },
        emojiNameAlias = this.get { emojiNameAlias },
        aniStickerWidth = this.get { aniStickerWidth },
        aniStickerHeight = this.get { aniStickerHeight },
    )
}