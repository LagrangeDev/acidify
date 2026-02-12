package org.ntqqrev.acidify.internal.proto.system

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SsoInfoSyncResp(
    @ProtoNumber(7) val registerInfoResponse: RegisterInfoResponse,
)