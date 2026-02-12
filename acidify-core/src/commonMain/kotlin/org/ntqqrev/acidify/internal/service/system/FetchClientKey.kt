package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.FetchClientKeyResp
import org.ntqqrev.acidify.internal.service.NoInputOidbService
import org.ntqqrev.acidify.internal.util.ensureLagrange
import org.ntqqrev.acidify.internal.util.pbDecode

internal object FetchClientKey : NoInputOidbService<String>(0x102a, 1) {
    // It's actually empty!
    override fun buildOidb(client: AbstractClient, payload: Unit): ByteArray {
        client.ensureLagrange()
        return ByteArray(0)
    }

    override fun parseOidb(client: AbstractClient, payload: ByteArray): String =
        payload.pbDecode<FetchClientKeyResp>().clientKey
}
