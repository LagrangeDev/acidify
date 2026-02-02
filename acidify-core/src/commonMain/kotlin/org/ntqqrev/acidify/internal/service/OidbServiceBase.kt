package org.ntqqrev.acidify.internal.service

import org.ntqqrev.acidify.exception.OidbException
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal abstract class OidbService<T, R>(
    val oidbCommand: Int,
    val oidbService: Int,
    val isReserved: Boolean = false
) : Service<T, R>("OidbSvcTrpcTcp.0x${oidbCommand.toString(16)}_$oidbService") {
    abstract fun buildOidb(client: LagrangeClient, payload: T): ByteArray
    abstract fun parseOidb(client: LagrangeClient, payload: ByteArray): R

    override fun build(client: LagrangeClient, payload: T): ByteArray = Oidb(
        command = oidbCommand,
        service = oidbService,
        body = buildOidb(client, payload),
        reserved = isReserved
    ).pbEncode()

    override fun parse(client: LagrangeClient, payload: ByteArray): R {
        val response = payload.pbDecode<Oidb>()
        val oidbResult = response.result
        if (oidbResult != 0) {
            throw OidbException(oidbCommand, oidbService, oidbResult, response.message)
        }
        return parseOidb(client, response.body)
    }
}

internal abstract class NoInputOidbService<R>(
    oidbCmd: Int,
    oidbSubCmd: Int,
    useReserved: Boolean = false
) : OidbService<Unit, R>(oidbCmd, oidbSubCmd, useReserved)

internal abstract class NoOutputOidbService<T>(
    oidbCmd: Int,
    oidbSubCmd: Int,
    useReserved: Boolean = false
) : OidbService<T, Unit>(oidbCmd, oidbSubCmd, useReserved) {
    override fun parseOidb(client: LagrangeClient, payload: ByteArray) = Unit
}