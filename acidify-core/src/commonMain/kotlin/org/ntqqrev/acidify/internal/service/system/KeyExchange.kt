package org.ntqqrev.acidify.internal.service.system

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.writeUInt
import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.crypto.aes.AesGcmProvider
import org.ntqqrev.acidify.internal.crypto.ecdh.Ecdh
import org.ntqqrev.acidify.internal.crypto.hash.SHA256
import org.ntqqrev.acidify.internal.proto.login.KeyExchangeRequest
import org.ntqqrev.acidify.internal.proto.login.KeyExchangeRequestBuf
import org.ntqqrev.acidify.internal.proto.login.KeyExchangeResponse
import org.ntqqrev.acidify.internal.proto.login.KeyExchangeResponseSecret
import org.ntqqrev.acidify.internal.service.EncryptType
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.ensureLagrange
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.internal.util.writeBytes
import kotlin.time.Clock

internal object KeyExchange : NoInputService<KeyExchange.Result>(
    "trpc.login.ecdh.EcdhService.SsoKeyExchange"
) {
    private val verifyHashKey =
        "e2733bf403149913cbf80c7a95168bd4ca6935ee53cd39764beebe2e007e3aee".hexToByteArray()
    private val serverPublicKey =
        "049d1423332735980edabe7e9ea451b3395b6f35250db8fc56f25889f628cbae3e8e73077914071eeebc108f4e0170057792bb17aa303af652313d17c1ac815e79".hexToByteArray()
    private val prime256V1 = Ecdh.generateKeyPair(Ecdh.Prime256V1)

    override val ssoEncryptType = EncryptType.WithEmptyKey

    class Result(
        val sessionTicket: ByteArray,
        val sessionKey: ByteArray,
    )

    override fun build(client: AbstractClient, payload: Unit): ByteArray {
        client.ensureLagrange()
        val publicKey = prime256V1.packPublic(false)
        val sharedKey = prime256V1.keyExchange(serverPublicKey, false)
        val timestamp = Clock.System.now().epochSeconds.toUInt()
        val secret = AesGcmProvider.encrypt(
            KeyExchangeRequestBuf(
                uin = client.uin.toString(),
                guid = client.guid,
            ).pbEncode(),
            sharedKey,
        )

        val verifyPacket = Buffer().apply {
            writeBytes(publicKey)
            writeInt(1)
            writeBytes(secret)
            writeInt(0)
            writeUInt(timestamp)
        }.readByteArray()

        return KeyExchangeRequest(
            publicKey = publicKey,
            type = 1u,
            secret = secret,
            timestamp = timestamp.toLong(),
            verifyHash = AesGcmProvider.encrypt(SHA256.hash(verifyPacket), verifyHashKey),
        ).pbEncode()
    }

    override fun parse(client: AbstractClient, payload: ByteArray): Result {
        client.ensureLagrange()
        val response = payload.pbDecode<KeyExchangeResponse>()
        val sharedKey = prime256V1.keyExchange(response.publicKey, false)
        val secret = AesGcmProvider.decrypt(response.secret, sharedKey)
            .pbDecode<KeyExchangeResponseSecret>()
        return Result(
            sessionTicket = secret.sessionTicket,
            sessionKey = secret.sessionKey,
        )
    }
}
