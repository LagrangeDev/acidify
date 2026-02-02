package org.ntqqrev.acidify.internal.proto.system

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SsoSecureInfo(
    @ProtoNumber(1) val sign: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val token: ByteArray = byteArrayOf(),
    @ProtoNumber(3) val extra: ByteArray = byteArrayOf(),
)
