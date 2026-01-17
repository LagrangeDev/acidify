package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.FetchGroupsReq
import org.ntqqrev.acidify.internal.packet.oidb.FetchGroupsResp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoInputOidbService
import org.ntqqrev.acidify.struct.BotGroupData

internal object FetchGroups : NoInputOidbService<List<BotGroupData>>(0xfe5, 2) {
    override fun buildOidb(client: IClient, payload: Unit): ByteArray = FetchGroupsReq {
        it[config] = FetchGroupsReq.Config {
            it[config1] = FetchGroupsReq.Config.Config1 { c1 ->
                listOf(
                    groupOwner,
                    field2,
                    memberMax,
                    memberCount,
                    groupName,
                    field8,
                    field9,
                    field10,
                    field11,
                    field12,
                    field13,
                    field14,
                    field15,
                    field16,
                    field17,
                    field18,
                    question,
                    field20,
                    field22,
                    field23,
                    field24,
                    field25,
                    field26,
                    field27,
                    field28,
                    field29,
                    field30,
                    field31,
                    field32,
                    field5001,
                    field5002,
                    field5003
                ).forEach { field -> c1[field] = true }
            }
            it[config2] = FetchGroupsReq.Config.Config2 { c2 ->
                listOf(
                    field1,
                    field2,
                    field3,
                    field4,
                    field5,
                    field6,
                    field7,
                    field8
                ).forEach { field -> c2[field] = true }
            }
            it[config3] = FetchGroupsReq.Config.Config3 { c3 ->
                c3[field5] = true
                c3[field6] = true
            }
        }
    }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray): List<BotGroupData> {
        val resp = FetchGroupsResp(payload)

        return resp.get { groups }.map { group ->
            val info = group.get { info }
            BotGroupData(
                uin = group.get { groupUin },
                name = info.get { groupName },
                memberCount = info.get { memberCount },
                capacity = info.get { memberMax }
            )
        }
    }
}