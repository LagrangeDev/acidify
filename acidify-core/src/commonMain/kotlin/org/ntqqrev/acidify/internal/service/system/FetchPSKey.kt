package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.FetchPsKeyReq
import org.ntqqrev.acidify.internal.proto.oidb.FetchPsKeyResp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object FetchPSKey : OidbService<List<String>, Map<String, String>>(0x102a, 0) {
    override fun buildOidb(client: LagrangeClient, payload: List<String>): ByteArray =
        FetchPsKeyReq(domains = payload).pbEncode()

    override fun parseOidb(client: LagrangeClient, payload: ByteArray): Map<String, String> =
        payload.pbDecode<FetchPsKeyResp>().psKeyEntries
}
