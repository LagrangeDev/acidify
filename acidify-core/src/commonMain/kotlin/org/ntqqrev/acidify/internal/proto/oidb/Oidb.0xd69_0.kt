package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FetchFilteredFriendRequestsReq(
    @ProtoNumber(1) val field1: Int = 0,
    @ProtoNumber(2) val field2: FilteredRequestCount = FilteredRequestCount(),
)

@Serializable
internal class FilteredRequestCount(
    @ProtoNumber(1) val count: Int = 0,
)

@Serializable
internal class FetchFilteredFriendRequestsResp(
    @ProtoNumber(2) val info: FilteredFriendRequestInfo = FilteredFriendRequestInfo(),
)

@Serializable
internal class FilteredFriendRequestInfo(
    @ProtoNumber(1) val requests: List<FilteredFriendRequestItem> = emptyList(),
)

@Serializable
internal class FilteredFriendRequestItem(
    @ProtoNumber(1) val sourceUid: String = "",
    @ProtoNumber(2) val sourceNickname: String = "",
    @ProtoNumber(5) val comment: String = "",
    @ProtoNumber(6) val source: String = "",
    @ProtoNumber(7) val warningInfo: String = "",
    @ProtoNumber(8) val timestamp: Long = 0L,
)
