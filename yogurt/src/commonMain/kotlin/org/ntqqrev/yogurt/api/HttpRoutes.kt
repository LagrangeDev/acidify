package org.ntqqrev.yogurt.api

import io.ktor.server.plugins.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.encodeToJsonElement
import org.ntqqrev.acidify.exception.OidbException
import org.ntqqrev.acidify.exception.ServiceException
import org.ntqqrev.acidify.util.log.Logger
import org.ntqqrev.milky.ApiGeneralResponse
import org.ntqqrev.milky.milkyJsonModule
import org.ntqqrev.yogurt.api.message.GetResourceTempUrl
import org.ntqqrev.yogurt.api.system.*
import kotlin.time.DurationUnit
import kotlin.time.measureTime

private inline fun <reified T : Any, reified R : Any> Route.serve(handler: MilkyApiHandler<T, R>) {
    post(handler.path) {
        val logger = application.dependencies.resolve<Logger>()
        try {
            val payload = call.receive<T>()
            call.respond(
                try {
                    var result: R
                    val duration = measureTime {
                        result = with(handler) { call(payload) }
                    }
                    logger.i {
                        "${call.request.local.remoteAddress} 调用 API ${handler.path}（成功 ${
                            duration.toString(
                                DurationUnit.MILLISECONDS
                            )
                        }）"
                    }
                    ApiGeneralResponse(
                        status = "ok",
                        retcode = 0,
                        data = milkyJsonModule.encodeToJsonElement(result)
                    )
                } catch (e: MilkyApiException) {
                    logger.w { "${call.request.local.remoteAddress} 调用 API ${handler.path}（${e.retcode} ${e.message}）" }
                    ApiGeneralResponse(
                        status = "failed",
                        retcode = e.retcode,
                        message = e.message
                    )
                } catch (e: Exception) {
                    logger.e(e) { "${call.request.local.remoteAddress} 调用 API ${handler.path}（失败 ${e::class.simpleName}）" }
                    when (e) {
                        is OidbException -> ApiGeneralResponse(
                            status = "failed",
                            retcode = e.oidbResult,
                            message = e.message
                        )

                        is ServiceException -> ApiGeneralResponse(
                            status = "failed",
                            retcode = e.retCode,
                            message = e.message
                        )

                        else -> ApiGeneralResponse(
                            status = "failed",
                            retcode = -500,
                            message = "Internal error: ${e.message}"
                        )
                    }
                }
            )
        } catch (e: BadRequestException) {
            logger.w { "${call.request.local.remoteAddress} 调用 API ${handler.path}（Bad Request）" }
            call.respond(
                ApiGeneralResponse(
                    status = "failed",
                    retcode = -400,
                    message = "Bad request: ${e.message}" +
                            (e.cause?.let { " (Caused by ${it::class.simpleName}: ${it.message})" } ?: "")
                )
            )
        } catch (e: Exception) {
            logger.e(e) { "${call.request.local.remoteAddress} 调用 API ${handler.path}（失败 ${e::class.simpleName}）" }
            call.respond(
                ApiGeneralResponse(
                    status = "failed",
                    retcode = -500,
                    message = "Internal error: ${e.message}"
                )
            )
        }
    }
}

fun Route.configureMilkyApiHttpRoutes() {
    serve(GetLoginInfo)
    serve(GetImplInfo)
    serve(GetUserProfile)
    serve(GetFriendList)
    serve(GetFriendInfo)
    serve(GetGroupList)
    serve(GetGroupInfo)
    serve(GetGroupMemberList)
    serve(GetGroupMemberInfo)
    serve(GetCookies)
    serve(GetCsrfToken)

    serve(GetResourceTempUrl)
}