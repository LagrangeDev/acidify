package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FetchGroupsReq(
    @ProtoNumber(1) val config: Config = Config(),
) {
    @Serializable
    internal class Config(
        @ProtoNumber(1) val config1: Config1 = Config1(),
        @ProtoNumber(2) val config2: Config2 = Config2(),
        @ProtoNumber(3) val config3: Config3 = Config3(),
    ) {
        @Serializable
        internal class Config1(
            @ProtoNumber(1) val groupOwner: Boolean = false,
            @ProtoNumber(2) val field2: Boolean = false,
            @ProtoNumber(3) val memberMax: Boolean = false,
            @ProtoNumber(4) val memberCount: Boolean = false,
            @ProtoNumber(5) val groupName: Boolean = false,
            @ProtoNumber(8) val field8: Boolean = false,
            @ProtoNumber(9) val field9: Boolean = false,
            @ProtoNumber(10) val field10: Boolean = false,
            @ProtoNumber(11) val field11: Boolean = false,
            @ProtoNumber(12) val field12: Boolean = false,
            @ProtoNumber(13) val field13: Boolean = false,
            @ProtoNumber(14) val field14: Boolean = false,
            @ProtoNumber(15) val field15: Boolean = false,
            @ProtoNumber(16) val field16: Boolean = false,
            @ProtoNumber(17) val field17: Boolean = false,
            @ProtoNumber(18) val field18: Boolean = false,
            @ProtoNumber(19) val question: Boolean = false,
            @ProtoNumber(20) val field20: Boolean = false,
            @ProtoNumber(22) val field22: Boolean = false,
            @ProtoNumber(23) val field23: Boolean = false,
            @ProtoNumber(24) val field24: Boolean = false,
            @ProtoNumber(25) val field25: Boolean = false,
            @ProtoNumber(26) val field26: Boolean = false,
            @ProtoNumber(27) val field27: Boolean = false,
            @ProtoNumber(28) val field28: Boolean = false,
            @ProtoNumber(29) val field29: Boolean = false,
            @ProtoNumber(30) val field30: Boolean = false,
            @ProtoNumber(31) val field31: Boolean = false,
            @ProtoNumber(32) val field32: Boolean = false,
            @ProtoNumber(5001) val field5001: Boolean = false,
            @ProtoNumber(5002) val field5002: Boolean = false,
            @ProtoNumber(5003) val field5003: Boolean = false,
        )

        @Serializable
        internal class Config2(
            @ProtoNumber(1) val field1: Boolean = false,
            @ProtoNumber(2) val field2: Boolean = false,
            @ProtoNumber(3) val field3: Boolean = false,
            @ProtoNumber(4) val field4: Boolean = false,
            @ProtoNumber(5) val field5: Boolean = false,
            @ProtoNumber(6) val field6: Boolean = false,
            @ProtoNumber(7) val field7: Boolean = false,
            @ProtoNumber(8) val field8: Boolean = false,
        )

        @Serializable
        internal class Config3(
            @ProtoNumber(5) val field5: Boolean = false,
            @ProtoNumber(6) val field6: Boolean = false,
        )
    }
}

@Serializable
internal class FetchGroupsResp(
    @ProtoNumber(2) val groups: List<Group> = emptyList(),
) {
    @Serializable
    internal class Group(
        @ProtoNumber(3) val groupUin: Long = 0L,
        @ProtoNumber(4) val info: Info = Info(),
        @ProtoNumber(5) val customInfo: CustomInfo = CustomInfo(),
    ) {
        @Serializable
        internal class Info(
            @ProtoNumber(1) val groupOwner: Member = Member(),
            @ProtoNumber(2) val createdTime: Long = 0L,
            @ProtoNumber(3) val memberMax: Int = 0,
            @ProtoNumber(4) val memberCount: Int = 0,
            @ProtoNumber(5) val groupName: String = "",
            @ProtoNumber(18) val description: String = "",
            @ProtoNumber(19) val question: String = "",
            @ProtoNumber(30) val announcement: String = "",
        ) {
            @Serializable
            internal class Member(
                @ProtoNumber(3) val remark: String = "",
            )
        }

        @Serializable
        internal class CustomInfo(
            @ProtoNumber(1) val lastSpeakTime: Long = 0L,
            @ProtoNumber(5) val lastestSeq: Int = 0,
        )
    }
}
