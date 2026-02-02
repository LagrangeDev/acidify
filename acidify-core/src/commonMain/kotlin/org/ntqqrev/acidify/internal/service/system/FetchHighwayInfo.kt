package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.message.media.FetchHighwayInfoReq
import org.ntqqrev.acidify.internal.proto.message.media.FetchHighwayInfoResp
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.internal.util.toIpString

internal object FetchHighwayInfo : NoInputService<FetchHighwayInfo.Resp>("HttpConn.0x6ff_501") {
    class Resp(
        val sigSession: ByteArray,
        val servers: Map<Int, List<Pair<String, Int>>>
    )

    override fun build(client: LagrangeClient, payload: Unit): ByteArray = FetchHighwayInfoReq(
        reqBody = FetchHighwayInfoReq.Body(
            uin = client.sessionStore.uin,
            idcId = 0,
            appid = 16,
            loginSigType = 1,
            loginSigTicket = client.sessionStore.tgtgt,
            requestFlag = 3,
            serviceTypes = listOf(1, 5, 10, 21),
            bid = 2,
            field9 = 9,
            field10 = 8,
            field11 = 0,
            version = "1.0.1",
        )
    ).pbEncode()

    override fun parse(client: LagrangeClient, payload: ByteArray): Resp {
        val rsp = payload.pbDecode<FetchHighwayInfoResp>().respBody
        val sigSession = rsp.sigSession
        val servers = mutableMapOf<Int, List<Pair<String, Int>>>()

        rsp.addrs.forEach { srvAddresses ->
            val serviceType = srvAddresses.serviceType
            servers[serviceType] = srvAddresses.addrs.map { ipAddr ->
                val ip = ipAddr.ip
                val port = ipAddr.port
                val ipString = ip.toIpString()
                ipString to port
            }
        }

        return Resp(sigSession, servers)
    }
}
