package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.ensureIsLagrange
import org.ntqqrev.acidify.internal.packet.system.RegisterInfoResponse
import org.ntqqrev.acidify.internal.packet.system.UnRegisterInfo
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.generateDeviceInfo

internal object BotOffline : NoInputService<String>("trpc.qq_new_tech.status_svc.StatusService.UnRegister") {
    override fun build(client: IClient, payload: Unit): ByteArray = UnRegisterInfo {
        client.ensureIsLagrange()
        it[device] = client.generateDeviceInfo()
    }.toByteArray()

    override fun parse(client: IClient, payload: ByteArray): String =
        RegisterInfoResponse(payload).get { message }
}