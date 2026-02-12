package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.exception.BotOnlineException
import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.system.DeviceInfo
import org.ntqqrev.acidify.internal.proto.system.OnlineBusinessInfo
import org.ntqqrev.acidify.internal.proto.system.RegisterInfo
import org.ntqqrev.acidify.internal.proto.system.SsoInfoSyncReq
import org.ntqqrev.acidify.internal.proto.system.SsoInfoSyncResp
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.ensureKurome
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import kotlin.random.Random

internal object AndroidInfoSync : NoInputService<Unit>("trpc.msg.register_proxy.RegisterProxy.SsoInfoSync") {
    override fun build(client: AbstractClient, payload: Unit): ByteArray {
        client.ensureKurome()
        return SsoInfoSyncReq(
            syncFlag = 735,
            reqRandom = Random.nextInt(),
            curActiveStatus = 2,
            groupLastMsgTime = 0,
            c2cSyncInfo = SsoInfoSyncReq.C2CSyncInfo(
                c2cMsgCookie = SsoInfoSyncReq.C2CSyncInfo.MsgCookie(
                    c2cLastMsgTime = 0
                ),
                c2cLastMsgTime = 0,
                lastC2CMsgCookie = SsoInfoSyncReq.C2CSyncInfo.MsgCookie(
                    c2cLastMsgTime = 0
                )
            ),
            normalConfig = SsoInfoSyncReq.NormalConfig(
                intCfg = mapOf(
                    46 to 0,
                    283 to 0
                )
            ),
            registerInfo = RegisterInfo(
                guid = client.guid.toHexString(),
                kickPc = false,
                currentVersion = client.appInfo.currentVersion,
                isFirstRegisterProxyOnline = false,
                localeId = 2052,
                device = DeviceInfo(
                    devName = client.sessionStore.deviceName,
                    devType = "alioth",
                    osVer = "13",
                    brand = "MIUI",
                    vendorOsName = "V816"
                ),
                setMute = 0,
                registerVendorType = 6,
                regType = 1,
                businessInfo = OnlineBusinessInfo(
                    notifySwitch = 1,
                    bindUinNotifySwitch = 1
                ),
                batteryStatus = 0
            ),
            unknown = mapOf(
                0 to 1
            ),
            appState = SsoInfoSyncReq.CurAppState(
                isDelayRequest = 0,
                appStatus = 1,
                silenceStatus = 0
            )
        ).pbEncode()
    }

    override fun parse(client: AbstractClient, payload: ByteArray) {
        val message = payload.pbDecode<SsoInfoSyncResp>().registerInfoResponse.message
        if (message != "register success") {
            throw BotOnlineException(message)
        }
    }
}