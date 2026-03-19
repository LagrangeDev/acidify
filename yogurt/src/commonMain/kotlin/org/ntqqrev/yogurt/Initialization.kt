package org.ntqqrev.yogurt

import com.github.ajalt.mordant.rendering.TextColors
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import org.ntqqrev.acidify.*
import org.ntqqrev.acidify.common.*
import org.ntqqrev.acidify.common.android.*
import org.ntqqrev.acidify.exception.UnstableNetworkException
import org.ntqqrev.acidify.exception.WtLoginException
import org.ntqqrev.milky.Event
import org.ntqqrev.yogurt.YogurtApp.config
import org.ntqqrev.yogurt.YogurtApp.t
import org.ntqqrev.yogurt.transform.transformAcidifyEvent
import org.ntqqrev.yogurt.util.logHandler

suspend fun Application.initializePC(): Bot {
    val sessionStore: SessionStore = if (SystemFileSystem.exists(sessionStorePath)) {
        SystemFileSystem.source(sessionStorePath).buffered().use {
            SessionStore.fromJson(it.readString())
        }
    } else SessionStore.empty()

    var signProvider: SignProvider
    var appInfo: AppInfo

    fun readCustomAppInfo(): AppInfo {
        return if (SystemFileSystem.exists(customAppInfoPath)) {
            SystemFileSystem.source(customAppInfoPath).buffered().use {
                AppInfo.fromJson(it.readString())
            }
        } else {
            throw IllegalStateException("未在 $customAppInfoPath 下找到自定义 AppInfo 文件")
        }
    }

    fun readBundledAppInfo(): AppInfo {
        return bundledPCAppInfo["${config.protocol.os}/${config.protocol.version}"]
            ?: throw IllegalStateException("未找到匹配的内置 AppInfo，请检查配置的 OS 和 Version 是否正确")
    }

    if (config.protocol.pcLagrangeSignToken.isNotEmpty()) {
        require(config.protocol.uin != 0L) {
            "使用 Lagrange Sign API 时，请在配置文件中填写 uin 字段"
        }
        appInfo = when (config.protocol.version) {
            "fetched" -> throw IllegalStateException("在使用 Lagrange Sign API 时，必须显式指定 AppInfo 版本或自行提供 AppInfo 文件，无法使用 fetched 版本")
            "custom" -> readCustomAppInfo()
            else -> readBundledAppInfo()
        }
        signProvider = LagrangeUrlSignProvider(
            url = config.protocol.signApiUrl,
            token = config.protocol.pcLagrangeSignToken,
            uin = config.protocol.uin,
            guid = sessionStore.guid.toHexString(),
            qua = "V1_${
                when (config.protocol.os) {
                    "Windows" -> "WIN"
                    "Mac" -> "MAC"
                    "Linux" -> "LNX"
                    else -> throw IllegalStateException()
                }
            }_NQ_${appInfo.currentVersion.replace('-', '_')}_GW_B",
        )
    } else {
        signProvider = UrlSignProvider(config.protocol.signApiUrl)
        appInfo = when (config.protocol.version) {
            "fetched" -> signProvider.getAppInfo()
                ?: throw IllegalStateException("通过 Sign API 获取 AppInfo 失败，请检查地址是否正确并且支持获取 AppInfo 功能")

            "custom" -> readCustomAppInfo()
            else -> readBundledAppInfo()
        }
    }

    t.println("使用协议 ${appInfo.os} ${appInfo.currentVersion} (AppId: ${appInfo.subAppId})")
    val bot = Bot(
        appInfo = appInfo,
        sessionStore = sessionStore,
        signProvider = signProvider,
        scope = this@initializePC, // application is a CoroutineScope
        minLogLevel = config.logging.coreLogLevel,
        logHandler = YogurtApp.logHandler,
    )
    dependencies {
        provide<AbstractBot> { bot } cleanup { runBlocking { it.offline() } }
        provide<SharedFlow<Event>> {
            bot.eventFlow
                .map(this@initializePC::transformAcidifyEvent)
                .filterNotNull()
                .shareIn(
                    scope = this@initializePC,
                    started = SharingStarted.Lazily,
                )
        }
    }
    return bot
}

suspend fun Application.initializeAndroid(): AndroidBot {
    require(config.protocol.uin != 0L && config.protocol.password.isNotEmpty()) {
        "使用 Android 协议登录时，请在配置文件中填写 uin 和 password 字段"
    }
    val sessionStore: AndroidSessionStore = if (SystemFileSystem.exists(androidSessionStorePath)) {
        SystemFileSystem.source(androidSessionStorePath).buffered().use {
            AndroidSessionStore.fromJson(it.readString())
        }.takeIf {
            it.uin == config.protocol.uin && it.password == config.protocol.password
        } ?: run {
            t.println("找到的 SessionStore 与配置的 uin 不匹配，正在创建新的 SessionStore...")
            AndroidSessionStore.empty(
                uin = config.protocol.uin,
                password = config.protocol.password
            )
        }
    } else AndroidSessionStore.empty(
        uin = config.protocol.uin,
        password = config.protocol.password
    ).also {
        t.println("未找到 Android SessionStore，正在创建新的 SessionStore 并保存到文件...")
        SystemFileSystem.sink(androidSessionStorePath).buffered().use { sink ->
            sink.writeString(it.toJson())
        }
    }
    val signProvider: AndroidSignProvider = if (!config.protocol.androidUseLegacySign) {
        AndroidUrlSignProvider(config.protocol.signApiUrl)
    } else {
        val (fullVersion, fekitVersion) = bundledAndroidLegacyAppInfo[config.protocol.version]
            ?: throw IllegalStateException(
                "未找到协议版本 ${config.protocol.version} 对应的 fullVersion 和 fekitVersion，请检查配置或联系开发者添加支持"
            )
        AndroidLegacyUrlSignProvider(
            url = config.protocol.signApiUrl,
            fullVersion = fullVersion,
            fekitVersion = fekitVersion,
            androidId = sessionStore.androidId,
            qimei36 = sessionStore.qimei,
        )
    }
    val appInfo: AndroidAppInfo = when (config.protocol.version) {
        "fetched" -> throw IllegalStateException("Android 协议不支持通过 Sign API 获取 AppInfo，请使用内置版本或自定义版本")

        "custom" -> if (SystemFileSystem.exists(customAppInfoPath)) {
            SystemFileSystem.source(customAppInfoPath).buffered().use {
                AndroidAppInfo.fromJson(it.readString())
            }
        } else {
            throw IllegalStateException("未在 $customAppInfoPath 下找到自定义 AppInfo 文件")
        }

        else -> bundledAndroidAppInfo["${config.protocol.os}/${config.protocol.version}"]
            ?: throw IllegalStateException("未找到匹配的内置 AppInfo，请检查配置的 OS 和 Version 是否正确")
    }
    t.println("使用协议 ${config.protocol.os} ${appInfo.ptVersion} (AppId: ${appInfo.subAppId})")
    val androidBot = AndroidBot(
        appInfo = appInfo,
        sessionStore = sessionStore,
        signProvider = signProvider,
        scope = this@initializeAndroid, // application is a CoroutineScope
        minLogLevel = config.logging.coreLogLevel,
        logHandler = YogurtApp.logHandler,
    )
    dependencies {
        provide<AbstractBot> { androidBot } cleanup { runBlocking { it.offline() } }
        provide<SharedFlow<Event>> {
            androidBot.eventFlow
                .map(this@initializeAndroid::transformAcidifyEvent)
                .filterNotNull()
                .shareIn(
                    scope = this@initializeAndroid,
                    started = SharingStarted.Lazily,
                )
        }
    }
    return androidBot
}

suspend fun Application.botLogin() {
    val bot = dependencies.resolve<AbstractBot>()
    when (bot) {
        is Bot -> bot.login(preloadContacts = config.milky.preloadContacts)
        is AndroidBot -> {
            fun onRequireCaptchaTicket(captchaUrl: String): String {
                val queryParams = captchaUrl.split("?")[1].replace("uin=0", "uin=${bot.uin}")
                t.println("https://yogurt-captcha.ntqqrev.org/?$queryParams")
                t.println("请打开网页完成验证码后输入 Ticket，并按 Enter 提交：")
                return readln().trim()
            }

            fun onRequireSmsCode(countryCode: String, phone: String, url: String): String {
                t.println("短信已发送到 $countryCode-$phone，请输入收到的验证码，并按 Enter 提交。")
                t.println("如果未收到验证码，也可以进行通过下面的 URL 进行手动验证，然后直接按 Enter 以继续登录。")
                t.println(url)
                return readln().trim()
            }

            try {
                bot.login(
                    ::onRequireCaptchaTicket,
                    ::onRequireSmsCode,
                    preloadContacts = config.milky.preloadContacts,
                )
            } catch (e: UnstableNetworkException) {
                t.println("发生 code=237 错误，可能是 Ticket 验证失败或网络环境不稳定，请更换网络环境后重试登录；")
                t.println("或通过下面的 URL 进行手动验证，验证完毕后按 Enter 重新登录。")
                t.println(e.manualVerifyUrl)
                readln()
                botLogin()
            } catch (e: WtLoginException) {
                t.println(TextColors.red("${e.tag} (code=${e.code})：${e.msg}"))
                throw e
            }
        }
    }
    if (bot.uin != config.protocol.uin) {
        throw IllegalStateException(
            "实际登录的 uin ${bot.uin} 与配置文件中指定的 uin ${config.protocol.uin} 不匹配，请检查配置文件或重新登录。"
        )
    }
}