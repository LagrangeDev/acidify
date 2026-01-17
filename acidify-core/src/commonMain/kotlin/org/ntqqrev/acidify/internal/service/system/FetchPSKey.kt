package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.FetchPsKeyReq
import org.ntqqrev.acidify.internal.packet.oidb.FetchPsKeyResp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService

internal object FetchPSKey : OidbService<List<String>, Map<String, String>>(0x102a, 0) {
    override fun buildOidb(client: IClient, payload: List<String>): ByteArray =
        FetchPsKeyReq {
            it[domains] = payload
        }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray): Map<String, String> =
        FetchPsKeyResp(payload)
            .get { psKeyEntries }
            .associate { it.get { domain } to it.get { key } }
}