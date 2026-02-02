package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class Oidb0x8FCReq(
    @ProtoNumber(1) val groupCode: Long = 0L,
    @ProtoNumber(2) val showFlag: Int = 0,
    @ProtoNumber(3) val memLevelInfo: List<MemberInfo> = emptyList(),
    @ProtoNumber(4) val levelNames: List<LevelName> = emptyList(),
    @ProtoNumber(5) val updateTime: Long = 0L,
    @ProtoNumber(6) val officeMode: Int = 0,
    @ProtoNumber(7) val groupOpenAppid: Int = 0,
    @ProtoNumber(8) val client: ClientInfo = ClientInfo(),
    @ProtoNumber(9) val authKey: ByteArray = byteArrayOf(),
) {
    @Serializable
    internal class MemberInfo(
        @ProtoNumber(1) val uid: String = "",
        @ProtoNumber(2) val point: Int = 0,
        @ProtoNumber(3) val activeKey: Int = 0,
        @ProtoNumber(4) val level: Int = 0,
        @ProtoNumber(5) val specialTitle: ByteArray = byteArrayOf(),
        @ProtoNumber(6) val specialTitleExpireTime: Int = 0,
        @ProtoNumber(7) val uinName: ByteArray = byteArrayOf(),
        @ProtoNumber(8) val memberCardName: ByteArray = byteArrayOf(),
        @ProtoNumber(9) val phone: ByteArray = byteArrayOf(),
        @ProtoNumber(10) val email: ByteArray = byteArrayOf(),
        @ProtoNumber(11) val remark: ByteArray = byteArrayOf(),
    )

    @Serializable
    internal class LevelName(
        @ProtoNumber(1) val level: Int = 0,
        @ProtoNumber(2) val name: ByteArray = byteArrayOf(),
    )

    @Serializable
    internal class ClientInfo(
        @ProtoNumber(1) val implat: Int = 0,
        @ProtoNumber(2) val clientVer: String = "",
    )
}
