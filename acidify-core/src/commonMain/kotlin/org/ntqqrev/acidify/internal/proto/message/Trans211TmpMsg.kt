package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class Trans211TmpMsg(
    @ProtoNumber(1) val msgBody: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val c2CCmd: Int = 0,
)
