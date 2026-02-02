package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FetchGroupNotificationsReq(
    @ProtoNumber(1) val count: Int = 0,
    @ProtoNumber(2) val startSeq: Long = 0L,
)

@Serializable
internal class FetchGroupNotificationsResp(
    @ProtoNumber(1) val notifications: List<GroupNotification> = emptyList(),
    @ProtoNumber(2) val nextStartSeq: Long = 0L,
)

@Serializable
internal class GroupNotification(
    @ProtoNumber(1) val sequence: Long = 0L,
    @ProtoNumber(2) val notifyType: Int = 0,
    @ProtoNumber(3) val requestState: Int = 0,
    @ProtoNumber(4) val group: Group = Group(),
    @ProtoNumber(5) val user1: User = User(),
    @ProtoNumber(6) val user2: User? = null,
    @ProtoNumber(7) val user3: User? = null,
    @ProtoNumber(8) val time: Int = 0,
    @ProtoNumber(10) val comment: String = "",
) {
    @Serializable
    internal class User(
        @ProtoNumber(1) val uid: String = "",
        @ProtoNumber(2) val nickname: String = "",
    )

    @Serializable
    internal class Group(
        @ProtoNumber(1) val groupUin: Long = 0L,
        @ProtoNumber(2) val groupName: String = "",
    )
}
