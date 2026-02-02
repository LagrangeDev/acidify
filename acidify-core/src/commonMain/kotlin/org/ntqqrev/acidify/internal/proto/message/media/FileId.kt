package org.ntqqrev.acidify.internal.proto.message.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class FileId(
    @ProtoNumber(4) val appId: Int = 0,
    @ProtoNumber(10) val ttl: Int = 0,
)
