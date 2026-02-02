package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FetchFriendRequestsReq(
    @ProtoNumber(1) val version: Int = 0,
    @ProtoNumber(3) val type: Int = 0,
    @ProtoNumber(4) val selfUid: String = "",
    @ProtoNumber(5) val startIndex: Int = 0,
    @ProtoNumber(6) val reqNum: Int = 0,
    @ProtoNumber(8) val getFlag: Int = 0,
    @ProtoNumber(9) val startTime: Int = 0,
    @ProtoNumber(12) val needCommFriend: Int = 0,
    @ProtoNumber(22) val field22: Int = 0,
)

@Serializable
internal class FetchFriendRequestsResp(
    @ProtoNumber(1) val field1: Int = 0,
    @ProtoNumber(2) val field2: Int = 0,
    @ProtoNumber(3) val info: FriendRequestInfo = FriendRequestInfo(),
)

@Serializable
internal class FriendRequestInfo(
    @ProtoNumber(2) val field2: Int = 0,
    @ProtoNumber(3) val count: Int = 0,
    @ProtoNumber(7) val requests: List<FriendRequestItem> = emptyList(),
)

@Serializable
internal class FriendRequestItem(
    @ProtoNumber(1) val targetUid: String = "",
    @ProtoNumber(2) val sourceUid: String = "",
    @ProtoNumber(3) val state: Int = 0,
    @ProtoNumber(4) val timestamp: Long = 0L,
    @ProtoNumber(5) val comment: String = "",
    @ProtoNumber(6) val source: String = "",
    @ProtoNumber(7) val sourceId: Int = 0,
    @ProtoNumber(8) val subSourceId: Int = 0,
)
