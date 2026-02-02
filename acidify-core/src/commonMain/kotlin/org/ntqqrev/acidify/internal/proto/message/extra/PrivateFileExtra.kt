package org.ntqqrev.acidify.internal.proto.message.extra

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import org.ntqqrev.acidify.internal.proto.message.NotOnlineFile

@Serializable
internal class PrivateFileExtra(
    @ProtoNumber(1) val notOnlineFile: NotOnlineFile = NotOnlineFile(),
)
