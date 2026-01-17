package org.ntqqrev.acidify.internal.service.system

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.ensureIsLagrange
import org.ntqqrev.acidify.internal.packet.misc.FaceRoamRequest
import org.ntqqrev.acidify.internal.packet.misc.FaceRoamResponse
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoInputService

internal object FetchCustomFace : NoInputService<List<String>>("Faceroam.OpReq") {
    private const val DEFAULT_KERNEL_VERSION = "10.0.19042.0"

    override fun build(client: IClient, payload: Unit): ByteArray = FaceRoamRequest {
        client.ensureIsLagrange()
        it[comm] = FaceRoamRequest.PlatInfo { plat ->
            plat[imPlat] = 1
            plat[osVersion] = DEFAULT_KERNEL_VERSION
            plat[qVersion] = client.appInfo.currentVersion
        }
        it[selfUin] = client.sessionStore.uin
        it[subCmd] = 1
        it[field6] = 1
    }.toByteArray()

    override fun parse(client: IClient, payload: ByteArray): List<String> {
        val resp = FaceRoamResponse(payload)
        val retCode = resp.get { retCode }
        if (retCode != 0) {
            val errMsg = resp.get { errMsg }
            throw Exception("获取收藏表情失败: $retCode $errMsg")
        }

        val userInfo = resp.get { userInfo }
        val bid = userInfo.get { bid }
        val uin = client.uin
        return userInfo.get { fileName }.map { fileName ->
            "https://p.qpic.cn/$bid/$uin/$fileName/0"
        }
    }
}
