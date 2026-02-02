package org.ntqqrev.acidify.internal.proto.misc

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoPacked

@Serializable
internal class FaceRoamRequest(
    @ProtoNumber(1) val comm: PlatInfo = PlatInfo(),
    @ProtoNumber(2) val selfUin: Long = 0L,
    @ProtoNumber(3) val subCmd: Int = 0,
    @ProtoNumber(6) val field6: Int = 0,
) {
    @Serializable
    internal class PlatInfo(
        @ProtoNumber(1) val imPlat: Int = 0,
        @ProtoNumber(2) val osVersion: String = "",
        @ProtoNumber(3) val qVersion: String = "",
    )
}

@Serializable
internal class FaceRoamResponse(
    @ProtoNumber(1) val retCode: Int = 0,
    @ProtoNumber(2) val errMsg: String = "",
    @ProtoNumber(3) val subCmd: Int = 0,
    @ProtoNumber(4) val userInfo: UserInfo = UserInfo(),
) {
    @Serializable
    internal class UserInfo(
        @ProtoNumber(1) val fileName: List<String> = emptyList(),
        @ProtoNumber(2) val deleteFile: List<String> = emptyList(),
        @ProtoNumber(3) val bid: String = "",
        @ProtoNumber(4) val maxRoamSize: Int = 0,
        @ProtoNumber(5) @ProtoPacked val emojiType: List<Int> = emptyList(),
    )
}
