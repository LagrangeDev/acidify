package org.ntqqrev.yogurt.debug

import io.ktor.server.plugins.di.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.refreshFaceDetailsMap

fun Route.configureDebugFaceDetailsApi() {
    get("/getFaceDetails") {
        val bot = application.dependencies.resolve<AbstractBot>()
        bot.refreshFaceDetailsMap()

        call.respond(
            bot.faceDetailMap.entries.sortedWith { (k1), (k2) ->
                val k1IntOrNull = k1.toIntOrNull()
                val k2IntOrNull = k2.toIntOrNull()
                return@sortedWith if (k1IntOrNull != null && k2IntOrNull != null) {
                    k1IntOrNull.compareTo(k2IntOrNull)
                } else if (k1IntOrNull == null && k2IntOrNull != null) {
                    1
                } else if (k1IntOrNull != null && k2IntOrNull == null) {
                    -1
                } else {
                    0
                }
            }.associate { it.key to it.value }
        )
    }
}