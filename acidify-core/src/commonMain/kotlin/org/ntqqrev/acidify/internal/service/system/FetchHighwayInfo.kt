package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.packet.media.FetchHighwayInfoReq
import org.ntqqrev.acidify.internal.packet.media.FetchHighwayInfoResp
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.toIpString
import org.ntqqrev.acidify.pb.invoke

internal object FetchHighwayInfo : NoInputService<FetchHighwayInfo.Resp>("HttpConn.0x6ff_501") {
    class Resp(
        val sigSession: ByteArray,
        val servers: Map<Int, List<Pair<String, Int>>>
    )

    override fun build(client: LagrangeClient, payload: Unit): ByteArray = FetchHighwayInfoReq {
        it[reqBody] = FetchHighwayInfoReq.Body {
            it[uin] = client.sessionStore.uin
            it[idcId] = 0
            it[appid] = 16
            it[loginSigType] = 1
            it[loginSigTicket] = client.sessionStore.tgtgt
            it[requestFlag] = 3
            it[serviceTypes] = listOf(1, 5, 10, 21)
            it[bid] = 2
            it[field9] = 9
            it[field10] = 8
            it[field11] = 0
            it[version] = "1.0.1"
        }
    }.toByteArray()

    override fun parse(client: LagrangeClient, payload: ByteArray): Resp {
        val rsp = FetchHighwayInfoResp(payload).get { rspBody }
        val sigSession = rsp.get { sigSession }
        val servers = mutableMapOf<Int, List<Pair<String, Int>>>()

        rsp.get { addrs }.forEach { srvAddresses ->
            val serviceType = srvAddresses.get { serviceType }
            servers[serviceType] = srvAddresses.get { addrs }.map { ipAddr ->
                val ip = ipAddr.get { ip }
                val port = ipAddr.get { port }
                val ipString = ip.toIpString()
                ipString to port
            }
        }

        return Resp(sigSession, servers)
    }
}