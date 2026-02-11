package org.ntqqrev.acidify.event.internal

import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.event.AcidifyEvent

internal abstract class AbstractSignal(val cmd: String) {
    abstract suspend fun parse(bot: AbstractBot, payload: ByteArray): List<AcidifyEvent>
}