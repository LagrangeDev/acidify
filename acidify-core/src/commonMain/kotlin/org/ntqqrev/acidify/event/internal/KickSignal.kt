package org.ntqqrev.acidify.event.internal

import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.event.AcidifyEvent
import org.ntqqrev.acidify.event.BotOfflineEvent
import org.ntqqrev.acidify.internal.proto.system.KickNT
import org.ntqqrev.acidify.internal.util.pbDecode

internal object KickSignal : AbstractSignal("trpc.qq_new_tech.status_svc.StatusService.KickNT") {
    override suspend fun parse(bot: Bot, payload: ByteArray): List<AcidifyEvent> {
        val kickNT = payload.pbDecode<KickNT>()
        return listOf(
            BotOfflineEvent("${kickNT.title}: ${kickNT.tip}")
        )
    }
}