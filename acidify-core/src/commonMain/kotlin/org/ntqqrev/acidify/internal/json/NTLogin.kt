package org.ntqqrev.acidify.internal.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class NTLoginGetFaceRequest(
    @SerialName("appid") val appId: Int,
    @SerialName("faceUpdateTime") val faceUpdateTime: Int,
    @SerialName("qrsig") val qrSig: String,
)

@Serializable
internal class NTLoginGetFaceResponse(
    @SerialName("retCode") val retCode: Int,
    @SerialName("errMsg") val errMsg: String,
    @SerialName("qrSig") val qrSig: String,
    @SerialName("uin") val uin: Long,
    @SerialName("faceUrl") val faceUrl: String,
    @SerialName("faceUpdateTime") val faceUpdateTime: Int,
)