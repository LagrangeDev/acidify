package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.FetchGroupsReq
import org.ntqqrev.acidify.internal.proto.oidb.FetchGroupsResp
import org.ntqqrev.acidify.internal.service.NoInputOidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.struct.BotGroupData

internal object FetchGroups : NoInputOidbService<List<BotGroupData>>(0xfe5, 2) {
    override fun buildOidb(client: AbstractClient, payload: Unit): ByteArray = FetchGroupsReq(
        config = FetchGroupsReq.Config(
            config1 = FetchGroupsReq.Config.Config1(
                groupOwner = true,
                field2 = true,
                memberMax = true,
                memberCount = true,
                groupName = true,
                field8 = true,
                field9 = true,
                field10 = true,
                field11 = true,
                field12 = true,
                field13 = true,
                field14 = true,
                field15 = true,
                field16 = true,
                field17 = true,
                field18 = true,
                question = true,
                field20 = true,
                field22 = true,
                field23 = true,
                field24 = true,
                field25 = true,
                field26 = true,
                field27 = true,
                field28 = true,
                field29 = true,
                field30 = true,
                field31 = true,
                field32 = true,
                field5001 = true,
                field5002 = true,
                field5003 = true,
            ),
            config2 = FetchGroupsReq.Config.Config2(
                field1 = true,
                field2 = true,
                field3 = true,
                field4 = true,
                field5 = true,
                field6 = true,
                field7 = true,
                field8 = true,
            ),
            config3 = FetchGroupsReq.Config.Config3(
                field5 = true,
                field6 = true,
            )
        )
    ).pbEncode()

    override fun parseOidb(client: AbstractClient, payload: ByteArray): List<BotGroupData> {
        val resp = payload.pbDecode<FetchGroupsResp>()

        return resp.groups.map { group ->
            val info = group.info
            BotGroupData(
                uin = group.groupUin,
                name = info.groupName,
                memberCount = info.memberCount,
                capacity = info.memberMax
            )
        }
    }
}
