package org.ntqqrev.acidify.internal.service.system

import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.AndroidFetchClientKeyReq
import org.ntqqrev.acidify.internal.proto.oidb.AndroidFetchClientKeyResp
import org.ntqqrev.acidify.internal.service.NoInputOidbService
import org.ntqqrev.acidify.internal.util.ensureKurome
import org.ntqqrev.acidify.internal.util.pbDecode

internal object AndroidFetchClientKey : NoInputOidbService<String>(0x9a2, 12, useReserved = true) {
    private val protobufModule = ProtoBuf {
        encodeDefaults = true
    }

    override fun buildOidb(client: AbstractClient, payload: Unit): ByteArray {
        client.ensureKurome()
        return protobufModule.encodeToByteArray(
            AndroidFetchClientKeyReq(
                appId = client.appInfo.appId,
                guid = client.guid.toHexString(),
                subAppId = client.appInfo.subAppId,
            )
        )
    }

    override fun parseOidb(client: AbstractClient, payload: ByteArray): String =
        payload.pbDecode<AndroidFetchClientKeyResp>().clientKey.toHexString()
}