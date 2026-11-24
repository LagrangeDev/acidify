package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.crypto.aes.AesGcmProvider
import org.ntqqrev.acidify.internal.proto.login.*
import org.ntqqrev.acidify.internal.service.EncryptType
import org.ntqqrev.acidify.internal.service.Service
import org.ntqqrev.acidify.internal.util.ensureLagrange
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal abstract class NTLogin<T, R>(cmd: String) : Service<T, R>(cmd) {
    override val ssoEncryptType = EncryptType.WithEmptyKey

    final override fun build(client: AbstractClient, payload: T): ByteArray {
        val sessionTicket = requireNotNull(client.ntLoginSessionTicket) {
            "NTLogin session ticket is not initialized"
        }
        val sessionKey = requireNotNull(client.ntLoginSessionKey) {
            "NTLogin session key is not initialized"
        }
        val common = NTLoginCommon(
            head = NTLoginHead(
                userInfo = NTLoginUserInfo(account = client.uin.toString()),
                clientInfo = NTLoginClientInfo(
                    deviceType = client.ntLoginDeviceType,
                    deviceName = client.deviceName,
                    platform = client.ntLoginPlatform,
                    guid = client.guid,
                ),
                appInfo = NTLoginAppInfo(
                    version = client.ntLoginVersion,
                    appId = client.appId,
                    appName = client.packageName,
                    qua = client.qua,
                ),
                cookie = NTLoginCookie(
                    cookieContent = client.ntLoginCookie.orEmpty(),
                ),
                sdkInfo = NTLoginSdkInfo(version = 1u),
            ),
            body = buildBody(client, payload),
        )
        return NTLoginForwardRequest(
            sessionTicket = sessionTicket,
            buffer = AesGcmProvider.encrypt(common.pbEncode(), sessionKey),
            type = 1u,
        ).pbEncode()
    }

    final override fun parse(client: AbstractClient, payload: ByteArray): R {
        val sessionKey = requireNotNull(client.ntLoginSessionKey) {
            "NTLogin session key is not initialized"
        }
        val forward = payload.pbDecode<NTLoginForwardRequest>()
        val common = AesGcmProvider.decrypt(forward.buffer, sessionKey)
            .pbDecode<NTLoginCommon>()
        common.head.cookie.cookieContent.takeIf(String::isNotEmpty)?.let {
            client.ntLoginCookie = it
        }
        return parseBody(client, common)
    }

    protected abstract fun buildBody(client: AbstractClient, payload: T): ByteArray

    protected abstract fun parseBody(client: AbstractClient, payload: NTLoginCommon): R

    object EasyLogin : NTLogin<Unit, EasyLogin.Result>("trpc.login.ecdh.EcdhService.SsoNTLoginEasyLogin") {
        private const val UNUSUAL_DEVICE = 140022011uL

        sealed class Result {
            class Success(val tickets: NTLoginTickets) : Result()

            class UnusualDevice(val unusualSig: ByteArray) : Result()

            class Failure(val error: NTLoginErrorInfo) : Result()
        }

        override fun buildBody(client: AbstractClient, payload: Unit): ByteArray {
            client.ensureLagrange()
            require(client.sessionStore.encryptedA1.isNotEmpty()) { "A1 is not initialized" }
            return NTLoginEasyLoginReqBody(client.sessionStore.encryptedA1).pbEncode()
        }

        override fun parseBody(client: AbstractClient, payload: NTLoginCommon): Result {
            val error = payload.head.errorInfo ?: NTLoginErrorInfo()
            if (error.errCode == 0uL) {
                val response = payload.body.pbDecode<NTLoginEasyLoginRspBody>()
                return Result.Success(response.tickets)
            }
            if (error.errCode == UNUSUAL_DEVICE) {
                val response = payload.body.pbDecode<NTLoginEasyLoginRspBody>()
                val unusualSig = response.secProtect.unusualDeviceCheckSig
                if (unusualSig.isNotEmpty()) return Result.UnusualDevice(unusualSig)
            }
            return Result.Failure(error)
        }
    }

    object UnusualEasyLogin :
        NTLogin<Unit, UnusualEasyLogin.Result>("trpc.login.ecdh.EcdhService.SsoNTLoginEasyLoginUnusualDevice") {
        sealed class Result {
            class Success(val tickets: NTLoginTickets) : Result()

            class Failure(val error: NTLoginErrorInfo) : Result()
        }

        override fun buildBody(client: AbstractClient, payload: Unit): ByteArray {
            client.ensureLagrange()
            require(client.sessionStore.encryptedA1.isNotEmpty()) { "A1 is not initialized" }
            return NTLoginEasyLoginUnusualDeviceReqBody(
                a1 = client.sessionStore.encryptedA1,
            ).pbEncode()
        }

        override fun parseBody(client: AbstractClient, payload: NTLoginCommon): Result {
            val error = payload.head.errorInfo ?: NTLoginErrorInfo()
            if (error.errCode != 0uL) return Result.Failure(error)

            val response = payload.body.pbDecode<NTLoginEasyLoginUnusualDeviceRspBody>()
            return Result.Success(response.tickets)
        }
    }
}
