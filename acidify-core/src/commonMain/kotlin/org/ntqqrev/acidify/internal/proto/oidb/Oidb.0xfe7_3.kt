package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoPacked

@Serializable
internal class FetchGroupMembersReq(
    @ProtoNumber(1) val groupUin: Long = 0L,
    @ProtoNumber(2) val field2: Int = 0,
    @ProtoNumber(3) val field3: Int = 0,
    @ProtoNumber(4) val body: Body = Body(),
    @ProtoNumber(15) val cookie: ByteArray? = null,
) {
    @Serializable
    internal class Body(
        @ProtoNumber(10) val memberName: Boolean = false,
        @ProtoNumber(11) val memberCard: Boolean = false,
        @ProtoNumber(12) val level: Boolean = false,
        @ProtoNumber(13) val field13: Boolean = false,
        @ProtoNumber(16) val field16: Boolean = false,
        @ProtoNumber(17) val specialTitle: Boolean = false,
        @ProtoNumber(18) val field18: Boolean = false,
        @ProtoNumber(20) val field20: Boolean = false,
        @ProtoNumber(21) val field21: Boolean = false,
        @ProtoNumber(100) val joinTimestamp: Boolean = false,
        @ProtoNumber(101) val lastMsgTimestamp: Boolean = false,
        @ProtoNumber(102) val shutUpTimestamp: Boolean = false,
        @ProtoNumber(103) val field103: Boolean = false,
        @ProtoNumber(104) val field104: Boolean = false,
        @ProtoNumber(105) val field105: Boolean = false,
        @ProtoNumber(106) val field106: Boolean = false,
        @ProtoNumber(107) val permission: Boolean = false,
        @ProtoNumber(200) val field200: Boolean = false,
        @ProtoNumber(201) val field201: Boolean = false,
    )
}

@Serializable
internal class FetchGroupMembersResp(
    @ProtoNumber(1) val groupUin: Long = 0L,
    @ProtoNumber(2) val members: List<Member> = emptyList(),
    @ProtoNumber(3) val memberCount: Int = 0,
    @ProtoNumber(5) val memberListChangeSeq: Int = 0,
    @ProtoNumber(6) val memberCardSeq: Int = 0,
    @ProtoNumber(15) val cookie: ByteArray? = null,
) {
    @Serializable
    internal class Member(
        @ProtoNumber(1) val id: Id = Id(),
        @ProtoNumber(10) val memberName: String = "",
        @ProtoNumber(17) val specialTitle: String? = null,
        @ProtoNumber(11) val memberCard: Card = Card(),
        @ProtoNumber(12) val level: Level = Level(),
        @ProtoNumber(100) val joinTimestamp: Long = 0L,
        @ProtoNumber(101) val lastMsgTimestamp: Long = 0L,
        @ProtoNumber(102) val shutUpTimestamp: Long? = null,
        @ProtoNumber(107) val permission: Int = 0,
    ) {
        @Serializable
        internal class Id(
            @ProtoNumber(2) val uid: String = "",
            @ProtoNumber(4) val uin: Long = 0L,
        )

        @Serializable
        internal class Card(
            @ProtoNumber(2) val memberCard: String? = null,
        )

        @Serializable
        internal class Level(
            @ProtoNumber(1) @ProtoPacked val infos: List<Int> = emptyList(),
            @ProtoNumber(2) val level: Int = 0,
        )
    }
}
