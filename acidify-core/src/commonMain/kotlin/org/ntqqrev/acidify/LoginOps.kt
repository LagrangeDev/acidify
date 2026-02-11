package org.ntqqrev.acidify

import kotlinx.coroutines.delay
import org.ntqqrev.acidify.event.AndroidSessionStoreUpdatedEvent
import org.ntqqrev.acidify.event.QRCodeGeneratedEvent
import org.ntqqrev.acidify.event.QRCodeStateQueryEvent
import org.ntqqrev.acidify.event.SessionStoreUpdatedEvent
import org.ntqqrev.acidify.exception.WtLoginException
import org.ntqqrev.acidify.internal.crypto.pow.POW
import org.ntqqrev.acidify.internal.proto.system.AndroidThirdPartyLoginResponse
import org.ntqqrev.acidify.internal.service.system.WtLogin
import org.ntqqrev.acidify.internal.util.*
import org.ntqqrev.acidify.struct.QRCodeState

/**
 * 发起二维码登录请求。过程中会触发事件：
 * - [QRCodeGeneratedEvent]：当二维码生成时触发，包含二维码链接和 PNG 图片数据
 * - [QRCodeStateQueryEvent]：每次查询二维码状态时触发，包含当前二维码状态（例如未扫码、已扫码未确认、已确认等）
 * @param queryInterval 查询间隔（单位 ms），不能小于 `1000`
 * @param preloadContacts 是否在登录成功后预加载好友和群信息以初始化内存缓存
 * @throws org.ntqqrev.acidify.exception.WtLoginException 当二维码扫描成功，但后续登录失败时抛出
 * @throws IllegalStateException 当二维码过期或用户取消登录时抛出
 * @see QRCodeState
 */
suspend fun Bot.qrCodeLogin(queryInterval: Long = 3000L, preloadContacts: Boolean = false) {
    require(queryInterval >= 1000L) { "查询间隔不能小于 1000 毫秒" }

    // Step 1: query QR code
    val qrCode = client.callService(WtLogin.TransEmp.FetchQRCode)
    client.sessionStore.qrSig = qrCode.qrSig
    logger.i { "二维码 URL：${qrCode.qrCodeUrl}" }
    sharedEventFlow.emit(QRCodeGeneratedEvent(qrCode.qrCodeUrl, qrCode.qrCodePng))

    // Step 2: poll QR code state until confirmed / error
    while (true) {
        val result = client.callService(WtLogin.TransEmp.QueryQRCodeState)
        val state = result.state
        logger.d { "二维码状态：${state.name} (${state.value})" }
        sharedEventFlow.emit(QRCodeStateQueryEvent(state))
        when (result) {
            is WtLogin.TransEmp.QueryQRCodeState.Result.Success -> {
                logger.i { "二维码已确认，登录用户：${result.uin}" }
                client.sessionStore.apply {
                    uin = result.uin
                    tgtgt = result.tgtgt
                    encryptedA1 = result.encryptedA1
                    noPicSig = result.noPicSig
                }
                break
            }

            is WtLogin.TransEmp.QueryQRCodeState.Result.Other -> {
                when (state) {
                    QRCodeState.CODE_EXPIRED -> throw IllegalStateException("二维码已过期")
                    QRCodeState.CANCELLED -> throw IllegalStateException("用户取消了登录")
                    QRCodeState.UNKNOWN -> throw IllegalStateException("未知的二维码状态")
                    else -> {} // pass
                }
            }
        }
        delay(queryInterval)
    }

    // Step 3: get login credentials and complete login
    val result = client.callService(WtLogin.PCLogin)
    client.sessionStore.apply {
        uid = result.uid
        a2 = result.a2
        d2 = result.d2
        d2Key = result.d2Key
        encryptedA1 = result.encryptedA1
    }
    sharedEventFlow.emit(SessionStoreUpdatedEvent(sessionStore))
    online(preloadContacts)
}


/**
 * 如果 Session 为空则调用 [qrCodeLogin] 进行登录。
 * 如果 Session 不为空则尝试使用现有的 Session 信息登录，若失败则调用 [qrCodeLogin] 重新登录。
 * @param queryInterval 查询间隔（单位 ms），不能小于 `1000`
 * @param preloadContacts 是否预加载好友和群信息以初始化内存缓存
 */
suspend fun Bot.login(queryInterval: Long = 3000L, preloadContacts: Boolean = false) {
    if (sessionStore.a2.isEmpty()) {
        logger.i { "Session 为空，尝试二维码登录" }
        qrCodeLogin(queryInterval, preloadContacts)
    } else {
        try {
            try {
                online(preloadContacts)
            } catch (e: Exception) {
                logger.w(e) { "使用现有 Session 登录失败，尝试刷新 DeviceGuid 后重新登录" }
                sessionStore.refreshDeviceGuid()
                online(preloadContacts)
            }
        } catch (e: Exception) {
            logger.w(e) { "使用现有 Session 登录失败，尝试二维码登录" }
            sessionStore.clear()
            // sharedEventFlow.emit(SessionStoreUpdatedEvent(sessionStore))
            qrCodeLogin(queryInterval, preloadContacts)
        }
    }
}

/**
 * 使用 [org.ntqqrev.acidify.common.android.AndroidSessionStore] 中的密码进行登录。
 * @param preloadContacts 是否预加载好友和群信息以初始化内存缓存
 */
suspend fun AndroidBot.login(
    onRequireCaptchaTicket: suspend (captchaUrl: String) -> String,
    onRequireSmsCode: suspend (countryCode: String, phone: String, smsUrl: String) -> String,
    preloadContacts: Boolean = false
) {
    var result: WtLogin.AndroidLogin.Resp = client.callService(
        WtLogin.AndroidLogin.Tgtgt,
        WtLogin.AndroidLogin.Tgtgt.Req(
            energy = client.getEnergyFor(WtLogin.AndroidLogin.Tgtgt),
            debugXwid = client.getDebugXwidFor(WtLogin.AndroidLogin.Tgtgt),
        )
    )

    if (result.state == 2u.toUByte()) { // Need captcha verify
        result.tlvPack[0x104u]?.let {
            sessionStore.state.tlv104 = it
        }
        result.tlvPack[0x546u]?.let {
            sessionStore.state.tlv547 = POW.generateTlv547(it)
        }
        val captchaUrl = result.tlvPack[0x192u]!!.decodeToString()
        val ticket = onRequireCaptchaTicket(captchaUrl)
        result = client.callService(
            WtLogin.AndroidLogin.SubmitCaptchaTicket,
            WtLogin.AndroidLogin.SubmitCaptchaTicket.Req(
                energy = client.getEnergyFor(WtLogin.AndroidLogin.SubmitCaptchaTicket),
                debugXwid = client.getDebugXwidFor(WtLogin.AndroidLogin.SubmitCaptchaTicket),
                ticket = ticket,
            )
        )
    }

    if (result.state == 239u.toUByte()) { // Device lock via SMS code
        result.tlvPack[0x104u]?.let {
            sessionStore.state.tlv104 = it
        }
        result.tlvPack[0x174u]?.let {
            sessionStore.state.tlv174 = it
        }
        val smsUrl = result.tlvPack[0x204u]!!.decodeToString()
        val tlv178Reader = result.tlvPack[0x178u]!!.reader()
        val countryCode = tlv178Reader.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
        val phone = tlv178Reader.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)

        result = client.callService(
            WtLogin.AndroidLogin.FetchSMSCode,
            WtLogin.AndroidLogin.FetchSMSCode.Req(
                debugXwid = client.getDebugXwidFor(WtLogin.AndroidLogin.FetchSMSCode),
            )
        )

        if (result.state == 160u.toUByte()) { // SMS required
            result.tlvPack[0x104u]?.let {
                sessionStore.state.tlv104 = it
            }
            val smsCode = onRequireSmsCode(countryCode, phone, smsUrl)
            result = client.callService(
                WtLogin.AndroidLogin.SubmitSMSCode,
                WtLogin.AndroidLogin.SubmitSMSCode.Req(
                    energy = client.getEnergyFor(WtLogin.AndroidLogin.SubmitSMSCode),
                    debugXwid = client.getDebugXwidFor(WtLogin.AndroidLogin.SubmitSMSCode),
                    smsCode = smsCode,
                )
            )
        }
    }

    if (result.state != 0u.toUByte()) { // fallback; the error should be in tlv 146
        throw WtLoginException(result.state.toInt(), "", "")
    }

    sessionStore.apply {
        wloginSigs.stWeb = result.tlvPack[0x103u]!!
        wloginSigs.d2 = result.tlvPack[0x143u]!!
        wloginSigs.ksid = result.tlvPack[0x108u]!!
        wloginSigs.a2 = result.tlvPack[0x10Au]!!
        wloginSigs.a1Key = result.tlvPack[0x10Cu]!!
        wloginSigs.a2Key = result.tlvPack[0x10Du]!!
        wloginSigs.stKey = result.tlvPack[0x10Eu]!!
        wloginSigs.st = result.tlvPack[0x114u]!!
        // result.tlvPack[0x11Au]!!let { /* save age, gender, nickname */ }
        wloginSigs.sKey = result.tlvPack[0x120u]!!
        wloginSigs.wtSessionTicket = result.tlvPack[0x133u]!!
        wloginSigs.wtSessionTicketKey = result.tlvPack[0x134u]!!
        wloginSigs.d2Key = result.tlvPack[0x305u]!!
        wloginSigs.a1 = result.tlvPack[0x106u]!!
        wloginSigs.noPicSig = result.tlvPack[0x16Au]!!
        wloginSigs.superKey = result.tlvPack[0x16Du]!!
        wloginSigs.psKey = mutableMapOf<String, String>().apply {
            val tlv512Reader = result.tlvPack[0x512u]!!.reader()
            val domainCount = tlv512Reader.readUShort()
            repeat(domainCount.toInt()) {
                val domain = tlv512Reader.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                val key = tlv512Reader.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                val pt4Token = tlv512Reader.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                this[domain] = key
            }
        }
        uid = result.tlvPack[0x543u]!!.pbDecode<AndroidThirdPartyLoginResponse>().commonInfo.rspNT.uid
    }
    sharedEventFlow.emit(AndroidSessionStoreUpdatedEvent(sessionStore))
    // TODO: send online packet
}