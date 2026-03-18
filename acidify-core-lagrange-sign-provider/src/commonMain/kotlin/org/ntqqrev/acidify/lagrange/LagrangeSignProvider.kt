package org.ntqqrev.acidify.lagrange

import org.ntqqrev.acidify.common.SignProvider
import org.ntqqrev.acidify.common.SignResult
import org.ntqqrev.acidify.lagrange.sign.Sign

class LagrangeSignProvider(
    address: String,
    port: Int,
    useHttps: Boolean,
    val uinProvider: () -> Long,
    val guid: ByteArray,
    val qua: String,
) : SignProvider {
    private val channel = io.github.timortel.kmpgrpc.core.Channel.Builder
        .forAddress(address, port)
        .apply {
            if (!useHttps) {
                usePlaintext()
            }
        }
        .build()
    private val signStub = Sign.SignStub(channel)

    override suspend fun sign(
        cmd: String,
        seq: Int,
        src: ByteArray
    ): SignResult {
        val response = signStub.getSecSign(
            Sign.GetSecSignRequest(
                uin = uinProvider(),
                command = cmd,
                seq = seq,
                body = src,
                guid = guid,
                qua = qua,
            )
        )
        return SignResult(
            sign = response.secSign,
            token = response.secToken,
            extra = response.secExtra,
        )
    }
}