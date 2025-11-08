@file:OptIn(ExperimentalSerializationApi::class)

package org.ntqqrev.acidify

import io.ktor.util.decodeBase64String
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.io.decodeFromSource
import kotlinx.serialization.json.io.encodeToSink
import org.ntqqrev.acidify.common.SessionStore
import org.ntqqrev.acidify.common.UrlSignProvider
import org.ntqqrev.acidify.event.MessageReceiveEvent
import org.ntqqrev.acidify.event.SessionStoreUpdatedEvent
import org.ntqqrev.acidify.logging.LogLevel
import org.ntqqrev.acidify.logging.SimpleLogHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BotTest {
    val defaultSignProvider = UrlSignProvider(
        "aHR0cHM6Ly9hcGkubnRxcXJldi5vcmcvc2lnbi8zOTAzOA==".decodeBase64String()
    )
    val defaultScope = CoroutineScope(Dispatchers.IO)
    val sessionStorePath = Path("acidify-core-test-data", "session.json").also {
        SystemFileSystem.createDirectories(it.parent!!)
    }

    private val session = if (SystemFileSystem.exists(sessionStorePath)) {
        SystemFileSystem.source(sessionStorePath).buffered().use {
            Json.decodeFromSource<SessionStore>(it)
        }
    } else {
        SessionStore.empty()
    }
    private val bot = runBlocking {
        Bot.create(
            appInfo = defaultSignProvider.getAppInfo()!!,
            sessionStore = session,
            signProvider = defaultSignProvider,
            scope = defaultScope,
            minLogLevel = LogLevel.VERBOSE,
            logHandler = SimpleLogHandler,
        )
    }

    init {
        bot.launch {
            bot.eventFlow.collect {
                when (it) {
                    is SessionStoreUpdatedEvent -> {
                        SystemFileSystem.sink(sessionStorePath).buffered().use { sink ->
                            Json.encodeToSink(it.sessionStore, sink)
                        }
                    }
                }
            }
        }
        runBlocking {
            if (session.a2.isEmpty()) {
                bot.qrCodeLogin()
            } else {
                bot.tryLogin()
            }
        }
    }

    @Test
    fun logLevelTest() {
        val logger = bot.createLogger(this)
        logger.v { "Verbose (trace) message" }
        logger.d { "Debug message" }
        logger.i { "Info message" }
        logger.w { "Warning message" }
        logger.e { "Error message" }
    }

    @Test
    fun fetchFriendsTest() = runBlocking {
        val friends = bot.fetchFriends()
        assertTrue(friends.isNotEmpty())
        friends.forEach { println(it) }
    }

    @Test
    fun fetchGroupsTest() = runBlocking {
        val groups = bot.fetchGroups()
        assertTrue(groups.isNotEmpty())
        groups.forEach { println(it) }
    }

    @Test
    fun fetchGroupMembersTest() = runBlocking {
        val groups = bot.fetchGroups()
        assertTrue(groups.isNotEmpty())
        val group = groups.first()
        val members = bot.fetchGroupMembers(group.uin)
        assertTrue(members.isNotEmpty())
        members.forEach { println(it) }
    }

    @Test
    fun fetchUserInfoTest() = runBlocking {
        val friends = bot.fetchFriends()
        assertTrue(friends.isNotEmpty())
        val friend = friends.first()
        val userInfoByUin = bot.fetchUserInfoByUin(friend.uin)
        val userInfoByUid = bot.fetchUserInfoByUid(friend.uid)
        assertEquals(userInfoByUin, userInfoByUid)
        println(userInfoByUin)
    }

    @Test
    fun fetchCredentialsTest() = runBlocking {
        val sKey = bot.getSKey()
        println("sKey: $sKey")
        assertTrue(sKey.isNotEmpty())

        val csrfToken = bot.getCsrfToken()
        println("CSRF Token: $csrfToken")

        val domain = "docs.qq.com"
        val psKey = bot.getPSKey(domain)
        println("PSKey for $domain: $psKey")
        assertTrue(psKey.isNotEmpty())
    }

    @Test
    fun sendMessageTest() = runBlocking {
        val friendResult = bot.sendFriendMessage(0) { // replace 0 with a valid friend UIN
            text("Hello world from Acidify!")
        }
        println("Friend send result: seq=${friendResult.sequence}")

        val groupResult = bot.sendGroupMessage(0) { // replace 0 with a valid group UIN
            text("Hello world from Acidify!")
        }
        println("Group send result: seq=${groupResult.sequence}")
    }

    @Test
    fun getMessageTest() = runBlocking {
        val friendMessage = bot.getFriendHistoryMessages(0, 30) // replace 0 with a valid friend UIN
        println("Friend messages size: ${friendMessage.messages.size}")

        val groupMessage = bot.getGroupHistoryMessages(0, 30) // replace 0 with a valid group UIN
        println("Group messages size: ${groupMessage.messages.size}")
    }

    @Test
    fun messageReceivingTest() = runBlocking {
        val logger = bot.createLogger(this)
        bot.launch {
            bot.sharedEventFlow.filterIsInstance<MessageReceiveEvent>().collect {
                logger.i {
                    "Received: ${it.message.scene} ${it.message.peerUin} ${it.message.senderUin} ${it.message.segments.joinToString("")}"
                }
            }
        }
        delay(30_000L)
    }
}