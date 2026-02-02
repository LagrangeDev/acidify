package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoPacked

@Serializable
internal class IncPull(
    @ProtoNumber(2) val reqCount: Int = 0,
    @ProtoNumber(3) val time: Long = 0L,
    @ProtoNumber(4) val localSeq: Int = 0,
    @ProtoNumber(5) val cookie: FetchFriendsCookie = FetchFriendsCookie(),
    @ProtoNumber(6) val flag: Int = 0,
    @ProtoNumber(7) val proxySeq: Int = 0,
    @ProtoNumber(10001) val requestBiz: List<Biz> = emptyList(),
    @ProtoNumber(10002) @ProtoPacked val extSnsFlagKey: List<Int> = emptyList(),
    @ProtoNumber(10003) @ProtoPacked val extPrivateIdListKey: List<Int> = emptyList(),
) {
    @Serializable
    internal class Biz(
        @ProtoNumber(1) val bizType: Int = 0,
        @ProtoNumber(2) val bizData: Busi = Busi(),
    ) {
        @Serializable
        internal class Busi(
            @ProtoNumber(1) @ProtoPacked val extBusi: List<Int> = emptyList(),
        )
    }
}

@Serializable
internal class IncPullResp(
    @ProtoNumber(1) val seq: Int = 0,
    @ProtoNumber(2) val cookie: FetchFriendsCookie? = null,
    @ProtoNumber(3) val isEnd: Boolean = false,
    @ProtoNumber(6) val time: Long = 0L,
    @ProtoNumber(7) val selfUin: Long = 0L,
    @ProtoNumber(8) val smallSeq: Int = 0,
    @ProtoNumber(101) val friendList: List<Friend> = emptyList(),
    @ProtoNumber(102) val category: List<Category> = emptyList(),
) {
    @Serializable
    internal class Friend(
        @ProtoNumber(1) val uid: String = "",
        @ProtoNumber(2) val categoryId: Int = 0,
        @ProtoNumber(3) val uin: Long = 0L,
        @ProtoNumber(10001) val subBizMap: List<SubBizEntry> = emptyList(),
    ) {
        @Serializable
        internal class SubBizEntry(
            @ProtoNumber(1) val key: Int = 0,
            @ProtoNumber(2) val value: SubBiz = SubBiz(),
        )

        @Serializable
        internal class SubBiz(
            @ProtoNumber(1) val numberProps: Map<Int, Int> = emptyMap(),
            @ProtoNumber(2) val stringProps: Map<Int, String> = emptyMap(),
        )
    }

    @Serializable
    internal class Category(
        @ProtoNumber(1) val categoryId: Int = 0,
        @ProtoNumber(2) val categoryName: String = "",
        @ProtoNumber(3) val categoryMemberCount: Int = 0,
        @ProtoNumber(4) val categorySortId: Int = 0,
    )
}

@Serializable
internal class FetchFriendsCookie(
    @ProtoNumber(1) val nextUin: Long? = null,
)
