package org.ntqqrev.acidify

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import org.ntqqrev.acidify.event.AndroidSessionStoreUpdatedEvent
import org.ntqqrev.acidify.event.QRCodeGeneratedEvent
import org.ntqqrev.acidify.event.QRCodeStateQueryEvent
import org.ntqqrev.acidify.event.SessionStoreUpdatedEvent
import org.ntqqrev.acidify.exception.SessionRenewalException
import org.ntqqrev.acidify.exception.WtLoginException
import org.ntqqrev.acidify.internal.crypto.pow.POW
import org.ntqqrev.acidify.internal.crypto.tea.TEA
import org.ntqqrev.acidify.internal.json.NTLoginGetFaceRequest
import org.ntqqrev.acidify.internal.json.NTLoginGetFaceResponse
import org.ntqqrev.acidify.internal.proto.system.AndroidThirdPartyLoginResponse
import org.ntqqrev.acidify.internal.service.system.KeyExchange
import org.ntqqrev.acidify.internal.service.system.NTLogin
import org.ntqqrev.acidify.internal.service.system.WtLogin
import org.ntqqrev.acidify.internal.util.*
import org.ntqqrev.acidify.struct.QRCodeState
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
suspend fun Bot.qrCodeLogin(
    queryInterval: Long = 3000L,
    preloadContacts: Boolean = false,
    unusualSig: ByteArray? = null // undocumented, for internal use only
) = loginMutex.withLock { qrCodeLoginInternal(queryInterval, preloadContacts, unusualSig) }

private suspend fun Bot.qrCodeLoginInternal(
    queryInterval: Long,
    preloadContacts: Boolean,
    unusualSig: ByteArray? = null,
) {
    require(queryInterval >= 1000L) { "查询间隔不能小于 1000 毫秒" }

    // Step 1: query QR code
    val qrCode = client.callService(
        WtLogin.TransEmp.FetchQRCode,
        WtLogin.TransEmp.FetchQRCode.Req(unusualSig),
    )
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

                    QRCodeState.WAITING_FOR_CONFIRMATION -> {
                        val resp = httpClient.post("https://ntlogin.qq.com/qr/getFace") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                NTLoginGetFaceRequest(
                                    appId = client.appInfo.appId,
                                    faceUpdateTime = 0,
                                    qrSig = qrCode.qrCodeString
                                )
                            )
                        }
                        val uin = resp.body<NTLoginGetFaceResponse>().uin
                        logger.i { "二维码等待用户确认，登录用户：$uin" }
                    }

                    else -> {} // pass
                }
            }
        }
        delay(queryInterval.milliseconds)
    }

    // Step 3: get login credentials and complete login
    if (unusualSig == null) {
        val result = client.callService(WtLogin.PCLogin)
        client.sessionStore.apply {
            uid = result.uid
            updateCredentials(result.encryptedA1, result.a2, result.d2, result.d2Key)
        }
    } else {
        when (val result = client.callService(NTLogin.UnusualEasyLogin)) {
            is NTLogin.UnusualEasyLogin.Result.Success -> {
                sessionStore.updateCredentials(
                    result.tickets.a1, result.tickets.a2, result.tickets.d2, result.tickets.d2Key
                )
            }

            is NTLogin.UnusualEasyLogin.Result.Failure -> throw WtLoginException(
                code = result.error.errCode.toInt(),
                tag = result.error.strTipsTitle,
                msg = result.error.strTipsContent,
            )
        }
    }
    sharedEventFlow.emit(SessionStoreUpdatedEvent(sessionStore))
    onlineInternal(preloadContacts)
}

/**
 * 使用 A1 更新登录凭据并在新连接上重新上线，不发送下线包，也不自动发起二维码验证。
 * 成功取得凭据时触发 [SessionStoreUpdatedEvent]，即使后续上线失败也保留新凭据。
 * @throws SessionRenewalException A1 不可用、被服务器拒绝或需要设备验证
 */
suspend fun Bot.renewSession() = loginMutex.withLock { renewSessionInternal() }

private suspend fun Bot.renewSessionInternal(preloadContacts: Boolean = false) {
    if (sessionStore.encryptedA1.isEmpty()) {
        throw SessionRenewalException(-1, "MissingA1", "没有可用于续约的 A1，请重新登录")
    }
    withTimeout(60.seconds) {
        client.packetContext.reconnectAndAuthenticate {
            val keyExchange = client.callService(KeyExchange)
            sessionStore.keySig = keyExchange.sessionTicket
            sessionStore.exchangeKey = keyExchange.sessionKey
            sessionStore.unusualCookies = null
            when (val result = client.callService(NTLogin.EasyLogin)) {
                is NTLogin.EasyLogin.Result.Success -> {
                    // Commit and publish before registration, so a later failure cannot lose fresh tickets.
                    withContext(NonCancellable) {
                        sessionStore.updateCredentials(
                            result.tickets.a1, result.tickets.a2, result.tickets.d2, result.tickets.d2Key
                        )
                        sharedEventFlow.emit(SessionStoreUpdatedEvent(sessionStore))
                    }
                }

                is NTLogin.EasyLogin.Result.UnusualDevice -> throw SessionRenewalException(
                    140022011, "UnusualDevice", "续约需要异常设备确认，请重新登录", result.unusualSig
                )

                is NTLogin.EasyLogin.Result.Failure -> throw SessionRenewalException(
                    result.error.errCode.toInt(), result.error.strTipsTitle, result.error.strTipsContent
                )
            }
        }
    }
    completeOnline(preloadContacts)
}

/**
 * 优先使用现有的 D2/A2 上线。Session 失效但 A1 可用时尝试 NTLogin，
 * 无可用凭据或 NTLogin 失败时回退到 [qrCodeLogin]。
 * @param queryInterval 查询间隔（单位 ms），不能小于 `1000`
 * @param preloadContacts 是否预加载好友和群信息以初始化内存缓存
 */
suspend fun Bot.login(queryInterval: Long = 3000L, preloadContacts: Boolean = false) = loginMutex.withLock {
    require(queryInterval >= 1000L) { "查询间隔不能小于 1000 毫秒" }

    if (sessionStore.a2.isNotEmpty()) {
        try {
            onlineInternal(preloadContacts)
            return@withLock
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.w(e) { "使用现有 Session 登录失败，尝试 EasyLogin" }
        }
    }

    if (sessionStore.encryptedA1.isNotEmpty()) {
        val previousA2 = sessionStore.a2
        try {
            renewSessionInternal(preloadContacts)
            return@withLock
        } catch (e: CancellationException) {
            throw e
        } catch (e: SessionRenewalException) {
            if (e.unusualSig != null) {
                logger.i { "EasyLogin 需要异常设备确认，开始二维码验证" }
                qrCodeLoginInternal(queryInterval, preloadContacts, e.unusualSig)
                return@withLock
            }
            logger.w(e) { "EasyLogin 被拒绝，回退到二维码登录" }
        } catch (e: Exception) {
            if (sessionStore.a2 !== previousA2) throw e
            logger.w(e) { "初始化 NTLogin 或 EasyLogin 失败，回退到二维码登录" }
        }
    }

    logger.i { "无可用登录票据，尝试二维码登录" }
    sessionStore.clear()
    qrCodeLoginInternal(queryInterval, preloadContacts)
}

/**
 * 使用 [org.ntqqrev.acidify.common.android.AndroidSessionStore] 中的密码进行登录。
 * @param onRequireCaptchaTicket 当需要验证码时的回调，参数为验证码 URL，返回值为验证码 Ticket
 * @param onRequireSmsCode 当需要短信验证码时的回调，参数为国家码、手机号和短信验证 URL，返回值为短信验证码
 * @param preloadContacts 是否预加载好友和群信息以初始化内存缓存
 */
suspend fun AndroidBot.passwordLogin(
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

    suspend fun handleSms(countryCode: String, phone: String, smsUrl: String): WtLogin.AndroidLogin.Resp {
        val smsCode = onRequireSmsCode(countryCode, phone, smsUrl)
        return if (smsCode.isNotEmpty()) {
            client.callService(
                WtLogin.AndroidLogin.SubmitSMSCode,
                WtLogin.AndroidLogin.SubmitSMSCode.Req(
                    energy = client.getEnergyFor(WtLogin.AndroidLogin.SubmitSMSCode),
                    debugXwid = client.getDebugXwidFor(WtLogin.AndroidLogin.SubmitSMSCode),
                    smsCode = smsCode,
                )
            )
        } else {
            client.callService(
                WtLogin.AndroidLogin.Tgtgt,
                WtLogin.AndroidLogin.Tgtgt.Req(
                    energy = client.getEnergyFor(WtLogin.AndroidLogin.Tgtgt),
                    debugXwid = client.getDebugXwidFor(WtLogin.AndroidLogin.Tgtgt),
                )
            )
        }
    }

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
        if (result.state == 160u.toUByte()) { // SMS required
            result.tlvPack[0x104u]?.let {
                sessionStore.state.tlv104 = it
            }
            val (countryCode, phone, smsUrl) = result.readSmsInfo()
            result = handleSms(countryCode, phone, smsUrl)
        }
    }

    if (result.state == 239u.toUByte()) { // Device lock via SMS code
        result.tlvPack[0x104u]?.let {
            sessionStore.state.tlv104 = it
        }
        result.tlvPack[0x174u]?.let {
            sessionStore.state.tlv174 = it
        }
        val (countryCode, phone, smsUrl) = result.readSmsInfo()
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
            result = handleSms(countryCode, phone, smsUrl)
        }
    }

    if (result.state != 0u.toUByte()) { // fallback; the error should be in tlv 146
        throw WtLoginException(result.state.toInt(), "", "")
    }

    val internalTlvPack = TEA.decrypt(result.tlvPack[0x119u]!!, client.sessionStore.wloginSigs.tgtgtKey)
        .parseTlv()

    sessionStore.apply {
        internalTlvPack[0x103u]?.let { wloginSigs.stWeb = it }
        internalTlvPack[0x143u]?.let { wloginSigs.d2 = it }
        internalTlvPack[0x108u]?.let { wloginSigs.ksid = it }
        internalTlvPack[0x10Au]?.let { wloginSigs.a2 = it }
        internalTlvPack[0x10Cu]?.let { wloginSigs.a1Key = it }
        internalTlvPack[0x10Du]?.let { wloginSigs.a2Key = it }
        internalTlvPack[0x10Eu]?.let { wloginSigs.stKey = it }
        internalTlvPack[0x114u]?.let { wloginSigs.st = it }
        // internalTlvPack[0x11Au]?.let { /* save age, gender, nickname */ }
        internalTlvPack[0x120u]?.let { wloginSigs.sKey = it }
        internalTlvPack[0x133u]?.let { wloginSigs.wtSessionTicket = it }
        internalTlvPack[0x134u]?.let { wloginSigs.wtSessionTicketKey = it }
        internalTlvPack[0x305u]?.let { wloginSigs.d2Key = it }
        internalTlvPack[0x106u]?.let { wloginSigs.a1 = it }
        internalTlvPack[0x16Au]?.let { wloginSigs.noPicSig = it }
        internalTlvPack[0x16Du]?.let { wloginSigs.superKey = it }
        internalTlvPack[0x512u]?.let {
            wloginSigs.psKey = mutableMapOf<String, String>().apply {
                val tlv512Reader = it.reader()
                val domainCount = tlv512Reader.readUShort()
                repeat(domainCount.toInt()) {
                    val domain = tlv512Reader.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                    val key = tlv512Reader.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                    val pt4Token = tlv512Reader.readPrefixedString(Prefix.UINT_16 or Prefix.LENGTH_ONLY)
                    this[domain] = key
                }
            }
        }
        internalTlvPack[0x543u]?.let {
            uid = it.pbDecode<AndroidThirdPartyLoginResponse>().commonInfo.rspNT.uid
        }
    }
    sharedEventFlow.emit(AndroidSessionStoreUpdatedEvent(sessionStore))
    online(preloadContacts)
}

/**
 * 如果 Session 为空则调用 [passwordLogin] 进行登录。
 * 如果 Session 不为空则尝试使用现有的 Session 信息登录，若失败则调用 [passwordLogin] 重新登录。
 * @param onRequireCaptchaTicket 当需要验证码时的回调，参数为验证码 URL，返回值为验证码 Ticket
 * @param onRequireSmsCode 当需要短信验证码时的回调，参数为国家码、手机号和短信验证 URL，返回值为短信验证码
 * @param preloadContacts 是否预加载好友和群信息以初始化内存缓存
 */
suspend fun AndroidBot.login(
    onRequireCaptchaTicket: suspend (captchaUrl: String) -> String,
    onRequireSmsCode: suspend (countryCode: String, phone: String, smsUrl: String) -> String,
    preloadContacts: Boolean = false
) {
    if (sessionStore.wloginSigs.a2.isEmpty()) {
        logger.i { "Session 为空，尝试密码登录" }
        passwordLogin(onRequireCaptchaTicket, onRequireSmsCode, preloadContacts)
    } else {
        try {
            online(preloadContacts)
        } catch (e: Exception) {
            logger.w(e) { "使用现有 Session 登录失败，尝试密码登录" }
            sessionStore.clear()
            // sharedEventFlow.emit(AndroidSessionStoreUpdatedEvent(sessionStore))
            passwordLogin(onRequireCaptchaTicket, onRequireSmsCode, preloadContacts)
        }
    }
}
