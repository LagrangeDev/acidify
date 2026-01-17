package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.misc.UserInfoKey
import org.ntqqrev.acidify.internal.packet.oidb.SetUserProfileReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object SetUserProfile : NoOutputOidbService<SetUserProfile.Req>(0x112a, 2) {
    class Req(
        val stringProps: Map<UserInfoKey, String> = mapOf(),
        val numberProps: Map<UserInfoKey, Int> = mapOf(),
    )

    override fun buildOidb(client: IClient, payload: Req) = SetUserProfileReq {
        it[uin] = client.uin
        it[stringProps] = payload.stringProps.map { (key, value) ->
            SetUserProfileReq.StringProp {
                it[this.key] = key.number
                it[this.value] = value
            }
        }
        it[numberProps] = payload.numberProps.map { (key, value) ->
            SetUserProfileReq.NumberProp {
                it[this.key] = key.number
                it[this.value] = value
            }
        }
    }.toByteArray()
}