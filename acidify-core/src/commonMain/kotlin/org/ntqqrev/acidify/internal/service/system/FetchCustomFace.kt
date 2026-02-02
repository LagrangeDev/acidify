package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.misc.FaceRoamRequest
import org.ntqqrev.acidify.internal.proto.misc.FaceRoamResponse
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object FetchCustomFace : NoInputService<List<String>>("Faceroam.OpReq") {
    private const val DEFAULT_KERNEL_VERSION = "10.0.19042.0"

    override fun build(client: LagrangeClient, payload: Unit): ByteArray = FaceRoamRequest(
        comm = FaceRoamRequest.PlatInfo(
            imPlat = 1,
            osVersion = DEFAULT_KERNEL_VERSION,
            qVersion = client.appInfo.currentVersion,
        ),
        selfUin = client.sessionStore.uin,
        subCmd = 1,
        field6 = 1,
    ).pbEncode()

    override fun parse(client: LagrangeClient, payload: ByteArray): List<String> {
        val resp = payload.pbDecode<FaceRoamResponse>()
        val retCode = resp.retCode
        if (retCode != 0) {
            val errMsg = resp.errMsg
            throw Exception("获取收藏表情失败: $retCode $errMsg")
        }

        val userInfo = resp.userInfo
        val bid = userInfo.bid
        val uin = client.sessionStore.uin
        return userInfo.fileName.map { fileName ->
            "https://p.qpic.cn/$bid/$uin/$fileName/0"
        }
    }
}
