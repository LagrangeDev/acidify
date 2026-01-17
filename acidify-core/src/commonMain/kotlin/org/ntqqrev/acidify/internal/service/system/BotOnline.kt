package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.ensureIsLagrange
import org.ntqqrev.acidify.internal.packet.system.OnlineBusinessInfo
import org.ntqqrev.acidify.internal.packet.system.RegisterInfo
import org.ntqqrev.acidify.internal.packet.system.RegisterInfoResponse
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.generateDeviceInfo

internal object BotOnline : NoInputService<String>("trpc.qq_new_tech.status_svc.StatusService.Register") {
    override fun build(client: IClient, payload: Unit): ByteArray = RegisterInfo {
        client.ensureIsLagrange()
        it[guid] = client.sessionStore.guid.toHexString()
        it[currentVersion] = client.appInfo.currentVersion
        it[device] = client.generateDeviceInfo()
        it[businessInfo] = OnlineBusinessInfo {
            it[notifySwitch] = 1
            it[bindUinNotifySwitch] = 1
        }
    }.toByteArray()

    override fun parse(client: IClient, payload: ByteArray): String =
        RegisterInfoResponse(payload).get { message }
}