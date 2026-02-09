package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.misc.FaceRoamRequest
import org.ntqqrev.acidify.internal.proto.misc.FaceRoamResponse
import org.ntqqrev.acidify.internal.service.NoInputService
import org.ntqqrev.acidify.internal.util.checkRetCode
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode

internal object FetchCustomFace : NoInputService<List<String>>("Faceroam.OpReq") {
    override fun build(client: AbstractClient, payload: Unit): ByteArray = FaceRoamRequest(
        comm = FaceRoamRequest.PlatInfo(
            imPlat = 1, // TODO: adapt to multi platform
            osVersion = "",
            qVersion = "",
        ),
        selfUin = client.uin,
        subCmd = 1,
        field6 = 1,
    ).pbEncode()

    override fun parse(client: AbstractClient, payload: ByteArray): List<String> {
        val resp = payload.pbDecode<FaceRoamResponse>()
        checkRetCode(resp.retCode, resp.errMsg)

        val userInfo = resp.userInfo
        val bid = userInfo.bid
        val uin = client.uin
        return userInfo.fileName.map { fileName ->
            "https://p.qpic.cn/$bid/$uin/$fileName/0"
        }
    }
}
