package org.ntqqrev.acidify.internal.proto.system

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SsoReservedFields(
    @ProtoNumber(15) val trace: String = "",
    @ProtoNumber(16) val uid: String? = null,
    @ProtoNumber(21) val msgType: Int? = null,
    @ProtoNumber(24) val secureInfo: SsoSecureInfo? = null,
    @ProtoNumber(26) val ntCoreVersion: Int? = null,
)
