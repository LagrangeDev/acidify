package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.system.RegisterInfoResponse
import org.ntqqrev.acidify.internal.proto.system.UnRegisterInfo
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.ensureLagrange
import org.ntqqrev.acidify.internal.util.generateDeviceInfo
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object BotOffline : NoInputService<String>("trpc.qq_new_tech.status_svc.StatusService.UnRegister") {
    override fun build(client: AbstractClient, payload: Unit): ByteArray = UnRegisterInfo(
        device = client.ensureLagrange().generateDeviceInfo()
    ).pbEncode()

    override fun parse(client: AbstractClient, payload: ByteArray): String =
        payload.pbDecode<RegisterInfoResponse>().message
}