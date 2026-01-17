package org.ntqqrev.acidify.internal.service

import org.ntqqrev.acidify.exception.OidbException
import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.Oidb
import org.ntqqrev.acidify.internal.protobuf.invoke

internal abstract class OidbService<T, R>(
    val oidbCommand: Int,
    val oidbService: Int,
    val isReserved: Boolean = false
) : Service<T, R>("OidbSvcTrpcTcp.0x${oidbCommand.toString(16)}_$oidbService") {
    abstract fun buildOidb(client: IClient, payload: T): ByteArray
    abstract fun parseOidb(client: IClient, payload: ByteArray): R

    override fun build(client: IClient, payload: T): ByteArray = Oidb {
        it[command] = oidbCommand
        it[service] = oidbService
        it[body] = buildOidb(client, payload)
        it[reserved] = isReserved
    }.toByteArray()

    override fun parse(client: IClient, payload: ByteArray): R {
        val response = Oidb(payload)
        val oidbResult = response.get { result }
        if (oidbResult != 0) {
            throw OidbException(oidbCommand, oidbService, oidbResult, response.get { message })
        }
        return parseOidb(client, response.get { body })
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
    override fun parseOidb(client: IClient, payload: ByteArray) = Unit
}