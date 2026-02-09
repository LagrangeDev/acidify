package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.exception.BotOnlineException
import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.system.OnlineBusinessInfo
import org.ntqqrev.acidify.internal.proto.system.RegisterInfo
import org.ntqqrev.acidify.internal.proto.system.RegisterInfoResponse
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.ensureLagrange
import org.ntqqrev.acidify.internal.util.generateDeviceInfo
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object BotOnline : NoInputService<Unit>("trpc.qq_new_tech.status_svc.StatusService.Register") {
    override fun build(client: AbstractClient, payload: Unit): ByteArray {
        client.ensureLagrange()
        return RegisterInfo(
            guid = client.sessionStore.guid.toHexString(),
            currentVersion = client.appInfo.currentVersion,
            device = client.generateDeviceInfo(),
            businessInfo = OnlineBusinessInfo(
                notifySwitch = 1,
                bindUinNotifySwitch = 1,
            )
        ).pbEncode()
    }

    override fun parse(client: AbstractClient, payload: ByteArray) {
        val message = payload.pbDecode<RegisterInfoResponse>().message
        if (message != "register success") {
            throw BotOnlineException(message)
        }
    }
}