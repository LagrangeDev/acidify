package org.ntqqrev.acidify.runner

import com.github.ajalt.mordant.platform.MultiplatformSystem.readEnvironmentVariable
import kotlinx.coroutines.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import org.ntqqrev.acidify.AndroidBot
import org.ntqqrev.acidify.common.android.AndroidAppInfo
import org.ntqqrev.acidify.common.android.AndroidSessionStore
import org.ntqqrev.acidify.common.android.AndroidUrlSignProvider
import org.ntqqrev.acidify.event.AndroidSessionStoreUpdatedEvent
import org.ntqqrev.acidify.logging.LogLevel
import org.ntqqrev.acidify.logging.SimpleLogHandler
import org.ntqqrev.acidify.online
import org.ntqqrev.acidify.passwordLogin
import kotlin.time.Duration.Companion.milliseconds

fun main(): Unit = runBlocking {
    val bot = AndroidBot(
        appInfo = AndroidAppInfo.Bundled.AndroidPad,
        sessionStore = if (SystemFileSystem.exists(Path("session-store-android.json"))) {
            SystemFileSystem.source(Path("session-store-android.json")).buffered().use { source ->
                AndroidSessionStore.fromJson(source.readString())
            }
        } else {
            AndroidSessionStore.empty(
                uin = readEnvironmentVariable("LOGIN_UIN")!!.toLong(),
                password = readEnvironmentVariable("LOGIN_PASSWORD")!!,
            )
        },
        signProvider = AndroidUrlSignProvider(readEnvironmentVariable("SIGN_API_URL")!!),
        scope = CoroutineScope(Dispatchers.IO),
        minLogLevel = LogLevel.DEBUG,
        logHandler = SimpleLogHandler,
    )
    bot.launch {
        bot.eventFlow.collect {
            if (it is AndroidSessionStoreUpdatedEvent) {
                println("Session store updated, saving...")
                SystemFileSystem.sink(Path("session-store-android.json")).buffered().use { sink ->
                    sink.writeString(it.sessionStore.toJson())
                }
            } else {
                println(it)
            }
        }
    }
    if (bot.sessionStore.wloginSigs.a2.isEmpty()) {
        bot.passwordLogin(
            onRequireCaptchaTicket = { captchaUrl ->
                val queryParams = captchaUrl.split("?")[1].replace("uin=0", "uin=${bot.uin}")
                println("Captcha at: https://captcha.lagrangecore.org/?$queryParams")
                println("Please enter captcha ticket:")
                readln().trim()
            },
            onRequireSmsCode = { countryCode, phone, url ->
                println("Manual verify URL: $url")
                println("SMS has been sent to $countryCode-$phone")
                println("Please enter SMS code:")
                readln().trim()
            }
        )
    } else {
        bot.online()
    }
    delay(Long.MAX_VALUE.milliseconds)
}