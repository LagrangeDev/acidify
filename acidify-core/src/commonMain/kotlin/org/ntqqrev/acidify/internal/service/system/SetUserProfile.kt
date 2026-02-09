package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.misc.UserInfoKey
import org.ntqqrev.acidify.internal.proto.oidb.SetUserProfileReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object SetUserProfile : NoOutputOidbService<SetUserProfile.Req>(0x112a, 2) {
    class Req(
        val stringProps: Map<UserInfoKey, String> = mapOf(),
        val numberProps: Map<UserInfoKey, Int> = mapOf(),
    )

    override fun buildOidb(client: AbstractClient, payload: Req) = SetUserProfileReq(
        uin = client.uin,
        stringProps = payload.stringProps.map { (key, value) ->
            SetUserProfileReq.StringProp(
                key = key.number,
                value = value,
            )
        },
        numberProps = payload.numberProps.map { (key, value) ->
            SetUserProfileReq.NumberProp(
                key = key.number,
                value = value,
            )
        },
    ).pbEncode()
}
