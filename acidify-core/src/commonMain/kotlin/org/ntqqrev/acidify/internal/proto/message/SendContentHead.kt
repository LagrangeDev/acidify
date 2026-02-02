package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SendContentHead(
    @ProtoNumber(1) val pkgNum: Int = 0,
    @ProtoNumber(2) val pkgIndex: Int = 0,
    @ProtoNumber(3) val divSeq: Int = 0,
    @ProtoNumber(4) val autoReply: Int = 0,
)
