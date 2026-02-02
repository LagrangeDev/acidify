package org.ntqqrev.acidify.internal.proto.oidb

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class ProfileLikeReq(
    @ProtoNumber(11) val targetUid: String = "",
    @ProtoNumber(12) val field2: Int = 0,
    @ProtoNumber(13) val field3: Int = 0,
)
