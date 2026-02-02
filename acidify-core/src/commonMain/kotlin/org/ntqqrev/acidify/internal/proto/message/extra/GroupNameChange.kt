package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class GroupNameChange(
    @ProtoNumber(2) val name: String = "",
)
