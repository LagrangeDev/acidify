package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.SetMemberAdminReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object SetMemberAdmin : NoOutputOidbService<SetMemberAdmin.Req>(0x1096, 1) {
    class Req(
        val groupUin: Long,
        val memberUid: String,
        val isAdmin: Boolean
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        SetMemberAdminReq(
            groupCode = payload.groupUin,
            targetUid = payload.memberUid,
            isAdmin = payload.isAdmin,
        ).pbEncode()
}
