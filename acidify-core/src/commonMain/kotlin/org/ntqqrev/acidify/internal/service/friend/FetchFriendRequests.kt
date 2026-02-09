package org.ntqqrev.acidify.internal.service.friend

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.*
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal abstract class FetchFriendRequests<R>(oidbCommand: Int, oidbService: Int, val isFiltered: Boolean) :
    OidbService<Int, R>(oidbCommand, oidbService) {
    object Normal : FetchFriendRequests<List<FriendRequestItem>>(0x5cf, 11, false) {
        override fun buildOidb(client: AbstractClient, payload: Int): ByteArray =
            FetchFriendRequestsReq(
                version = 1,
                type = 6,
                selfUid = client.uid,
                startIndex = 0,
                reqNum = payload,
                getFlag = 2,
                startTime = 0,
                needCommFriend = 1,
                field22 = 1,
            ).pbEncode()

        override fun parseOidb(client: AbstractClient, payload: ByteArray): List<FriendRequestItem> =
            payload.pbDecode<FetchFriendRequestsResp>().info.requests
    }

    object Filtered : FetchFriendRequests<List<FilteredFriendRequestItem>>(0xd69, 0, true) {
        override fun buildOidb(client: AbstractClient, payload: Int): ByteArray =
            FetchFilteredFriendRequestsReq(
                field1 = 1,
                field2 = FilteredRequestCount(count = payload),
            ).pbEncode()

        override fun parseOidb(client: AbstractClient, payload: ByteArray): List<FilteredFriendRequestItem> =
            payload.pbDecode<FetchFilteredFriendRequestsResp>().info.requests
    }
}
