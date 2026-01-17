package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.system.SsoHeartBeat
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoInputService

internal object Heartbeat : NoInputService<Unit>("trpc.qq_new_tech.status_svc.StatusService.SsoHeartBeat") {
    override fun build(client: IClient, payload: Unit): ByteArray = SsoHeartBeat {
        it[type] = 1
    }.toByteArray()

    override fun parse(client: IClient, payload: ByteArray) = Unit
}