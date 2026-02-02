package org.ntqqrev.acidify.internal.proto.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import org.ntqqrev.acidify.internal.proto.message.elem.*

@Serializable
internal class Elem(
    @ProtoNumber(1) val text: Text? = null,
    @ProtoNumber(2) val face: Face? = null,
    @ProtoNumber(4) val notOnlineImage: NotOnlineImage? = null,
    @ProtoNumber(5) val transElemInfo: TransElem? = null,
    @ProtoNumber(6) val marketFace: MarketFace? = null,
    @ProtoNumber(8) val customFace: CustomFace? = null,
    @ProtoNumber(12) val richMsg: RichMsg? = null,
    @ProtoNumber(16) val extraInfo: ExtraInfo? = null,
    @ProtoNumber(19) val videoFile: VideoFile? = null,
    @ProtoNumber(37) val generalFlags: GeneralFlags? = null,
    @ProtoNumber(45) val srcMsg: SourceMsg? = null,
    @ProtoNumber(51) val lightAppElem: LightAppElem? = null,
    @ProtoNumber(53) val commonElem: CommonElem? = null,
)
