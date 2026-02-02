package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class RichText(
    @ProtoNumber(1) val attr: Attr = Attr(),
    @ProtoNumber(2) val elems: List<Elem> = emptyList(),
    @ProtoNumber(3) val notOnlineFile: NotOnlineFile = NotOnlineFile(),
    @ProtoNumber(4) val ptt: Ptt = Ptt(),
    @ProtoNumber(5) val tmpPtt: TmpPtt = TmpPtt(),
    @ProtoNumber(6) val trans211TmpMsg: Trans211TmpMsg = Trans211TmpMsg(),
)
