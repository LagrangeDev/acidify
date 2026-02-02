package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class RoutingHead(
    @ProtoNumber(1) val fromUin: Long = 0L,
    @ProtoNumber(2) val fromUid: String = "",
    @ProtoNumber(3) val fromAppId: Int = 0,
    @ProtoNumber(4) val fromInstId: Int = 0,
    @ProtoNumber(5) val toUin: Long = 0L,
    @ProtoNumber(6) val toUid: String = "",
    @ProtoNumber(7) val commonC2C: CommonC2C = CommonC2C(),
    @ProtoNumber(8) val group: CommonGroup = CommonGroup(),
) {
    @Serializable
    internal class CommonC2C(
        @ProtoNumber(1) val c2CType: Int = 0,
        @ProtoNumber(2) val serviceType: Int = 0,
        @ProtoNumber(3) val sig: ByteArray = byteArrayOf(),
        @ProtoNumber(4) val fromTinyId: Long = 0L,
        @ProtoNumber(5) val toTinyId: Long = 0L,
        @ProtoNumber(6) val name: String = "",
    )

    @Serializable
    internal class CommonGroup(
        @ProtoNumber(1) val groupCode: Long = 0L,
        @ProtoNumber(2) val groupType: Int = 0,
        @ProtoNumber(3) val groupInfoSeq: Long = 0L,
        @ProtoNumber(4) val groupCard: String = "",
        @ProtoNumber(5) val groupCardType: Int = 0,
        @ProtoNumber(6) val groupLevel: Int = 0,
        @ProtoNumber(7) val groupName: String = "",
        @ProtoNumber(8) val extGroupKeyInfo: String = "",
        @ProtoNumber(9) val msgFlag: Int = 0,
    )
}
