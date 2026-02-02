package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class MessageBody(
    @ProtoNumber(1) val richText: RichText = RichText(),
    @ProtoNumber(2) val msgContent: ByteArray = byteArrayOf(),
    @ProtoNumber(3) val msgEncryptContent: ByteArray = byteArrayOf(),
)
