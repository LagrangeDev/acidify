package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class CommonMessage(
    @ProtoNumber(1) val routingHead: RoutingHead = RoutingHead(),
    @ProtoNumber(2) val contentHead: ContentHead = ContentHead(),
    @ProtoNumber(3) val messageBody: MessageBody = MessageBody(),
)
