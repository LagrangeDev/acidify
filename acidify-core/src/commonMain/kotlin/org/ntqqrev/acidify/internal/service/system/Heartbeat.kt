package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.system.SsoHeartBeat
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object Heartbeat : NoInputService<Unit>("trpc.qq_new_tech.status_svc.StatusService.SsoHeartBeat") {
    override fun build(client: AbstractClient, payload: Unit): ByteArray =
        SsoHeartBeat(type = 1).pbEncode()

    override fun parse(client: AbstractClient, payload: ByteArray) = Unit
}
