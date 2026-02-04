package org.ntqqrev.yogurt.script

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.define
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.ntqqrev.yogurt.api.handler.*
import org.ntqqrev.yogurt.util.defineJsApi
import kotlin.time.TimeMark
import kotlin.time.TimeSource

suspend fun Application.createScriptEnvironment() = QuickJs.create(jobDispatcher = Dispatchers.Default).apply {
    evaluate<Any?>(
        """
            const $rootHandle = {
                $apiHandle: {},
                $eventHandle: {},
            };
        """.trimIndent()
    )

    define(internalApiHandle) {
        defineJsApi(this@apply, this, GetLoginInfo)
        defineJsApi(this@apply, this, GetImplInfo)
        defineJsApi(this@apply, this, GetUserProfile)
        defineJsApi(this@apply, this, GetFriendList)
        defineJsApi(this@apply, this, GetFriendInfo)
        defineJsApi(this@apply, this, GetGroupList)
        defineJsApi(this@apply, this, GetGroupInfo)
        defineJsApi(this@apply, this, GetGroupMemberList)
        defineJsApi(this@apply, this, GetGroupMemberInfo)
        defineJsApi(this@apply, this, SetAvatar)
        defineJsApi(this@apply, this, SetNickname)
        defineJsApi(this@apply, this, SetBio)
        defineJsApi(this@apply, this, GetCustomFaceUrlList)
        defineJsApi(this@apply, this, GetCookies)
        defineJsApi(this@apply, this, GetCsrfToken)

        defineJsApi(this@apply, this, SendPrivateMessage)
        defineJsApi(this@apply, this, SendGroupMessage)
        defineJsApi(this@apply, this, RecallPrivateMessage)
        defineJsApi(this@apply, this, RecallGroupMessage)
        defineJsApi(this@apply, this, GetMessage)
        defineJsApi(this@apply, this, GetHistoryMessages)
        defineJsApi(this@apply, this, GetResourceTempUrl)
        defineJsApi(this@apply, this, GetForwardedMessages)
        defineJsApi(this@apply, this, MarkMessageAsRead)

        defineJsApi(this@apply, this, SendFriendNudge)
        defineJsApi(this@apply, this, SendProfileLike)
        defineJsApi(this@apply, this, DeleteFriend)
        defineJsApi(this@apply, this, GetFriendRequests)
        defineJsApi(this@apply, this, AcceptFriendRequest)
        defineJsApi(this@apply, this, RejectFriendRequest)

        defineJsApi(this@apply, this, SetGroupName)
        defineJsApi(this@apply, this, SetGroupAvatar)
        defineJsApi(this@apply, this, SetGroupMemberCard)
        defineJsApi(this@apply, this, SetGroupMemberSpecialTitle)
        defineJsApi(this@apply, this, SetGroupMemberAdmin)
        defineJsApi(this@apply, this, SetGroupMemberMute)
        defineJsApi(this@apply, this, SetGroupWholeMute)
        defineJsApi(this@apply, this, KickGroupMember)
        defineJsApi(this@apply, this, GetGroupAnnouncements)
        defineJsApi(this@apply, this, SendGroupAnnouncement)
        defineJsApi(this@apply, this, DeleteGroupAnnouncement)
        defineJsApi(this@apply, this, GetGroupEssenceMessages)
        defineJsApi(this@apply, this, SetGroupEssenceMessage)
        defineJsApi(this@apply, this, QuitGroup)
        defineJsApi(this@apply, this, SendGroupMessageReaction)
        defineJsApi(this@apply, this, SendGroupNudge)
        defineJsApi(this@apply, this, GetGroupNotifications)
        defineJsApi(this@apply, this, AcceptGroupRequest)
        defineJsApi(this@apply, this, RejectGroupRequest)
        defineJsApi(this@apply, this, AcceptGroupInvitation)
        defineJsApi(this@apply, this, RejectGroupInvitation)

        defineJsApi(this@apply, this, UploadPrivateFile)
        defineJsApi(this@apply, this, UploadGroupFile)
        defineJsApi(this@apply, this, GetPrivateFileDownloadUrl)
        defineJsApi(this@apply, this, GetGroupFileDownloadUrl)
        defineJsApi(this@apply, this, GetGroupFiles)
        defineJsApi(this@apply, this, MoveGroupFile)
        defineJsApi(this@apply, this, RenameGroupFile)
        defineJsApi(this@apply, this, DeleteGroupFile)
        defineJsApi(this@apply, this, CreateGroupFolder)
        defineJsApi(this@apply, this, RenameGroupFolder)
        defineJsApi(this@apply, this, DeleteGroupFolder)
    }

    evaluate<Any?>(
        $$"""
            const $$internalEventMapHandle = new Map();
            
            $$rootHandle.$$eventHandle.on = (eventName, listener) => {
                if (!$$internalEventMapHandle.has(eventName)) {
                    $$internalEventMapHandle.set(eventName, []);
                }
                $$internalEventMapHandle.get(eventName).push(listener);
            };
            
            function $$internalEmitHandle(event) {
                const eventName = event.event_type;
                if ($$internalEventMapHandle.has(eventName)) {
                    for (const listener of $$internalEventMapHandle.get(eventName)) {
                        try {
                            listener(event);
                        } catch (error) {
                            console.error(`Error in event listener for ${eventName}:`, error);
                        }
                    }
                }
            }
        """.trimIndent()
    )

    define("console") {
        val consoleTimers = mutableMapOf<String, TimeMark>()
        val consoleCounts = mutableMapOf<String, Int>()
        var consoleGroupDepth = 0

        fun indent(): String = "  ".repeat(consoleGroupDepth.coerceAtLeast(0))

        fun formatArgs(args: Array<Any?>): String {
            if (args.isEmpty()) return ""
            return args.joinToString(" ") { it?.toString() ?: "null" }
        }

        fun printLine(prefix: String?, args: Array<Any?>) {
            val message = formatArgs(args)
            val space = if (prefix.isNullOrBlank() || message.isBlank()) "" else " "
            println("${indent()}${prefix.orEmpty()}$space$message")
        }

        fun tablePrint(data: Any?) {
            when (data) {
                null -> println("${indent()}null")
                is Map<*, *> -> {
                    println("${indent()}Key\tValue")
                    data.forEach { (key, value) ->
                        println("${indent()}${key}\t${value}")
                    }
                }

                is Array<*> -> {
                    tablePrint(data.asList())
                }

                is Iterable<*> -> {
                    val items = data.toList()
                    if (items.all { it is Map<*, *> } && items.isNotEmpty()) {
                        val keys = (items.first() as Map<*, *>).keys.toList()
                        println("${indent()}${keys.joinToString("\t")}")
                        items.forEach { item ->
                            val row = item as Map<*, *>
                            val values = keys.joinToString("\t") { key -> row[key].toString() }
                            println("${indent()}$values")
                        }
                    } else {
                        println("${indent()}Index\tValue")
                        items.forEachIndexed { index, value ->
                            println("${indent()}$index\t${value}")
                        }
                    }
                }

                else -> println("${indent()}${data}")
            }
        }

        function("log") { args ->
            printLine(null, args)
        }

        function("info") { args ->
            printLine("INFO", args)
        }

        function("warn") { args ->
            printLine("WARN", args)
        }

        function("error") { args ->
            printLine("ERROR", args)
        }

        function("debug") { args ->
            printLine("DEBUG", args)
        }

        function("assert") { args ->
            val condition = args.firstOrNull() as? Boolean ?: false
            if (!condition) {
                val messageArgs = if (args.size > 1) args.copyOfRange(1, args.size) else emptyArray()
                val message =
                    if (messageArgs.isEmpty()) "Assertion failed" else "Assertion failed: ${formatArgs(messageArgs)}"
                printLine("ERROR", arrayOf(message))
            }
        }

        function("trace") { args ->
            val prefixArgs = if (args.isEmpty()) arrayOf<Any?>("Trace") else arrayOf<Any?>("Trace:", formatArgs(args))
            printLine("TRACE", prefixArgs)
        }

        function("group") { args ->
            if (args.isNotEmpty()) {
                printLine(null, args)
            }
            consoleGroupDepth += 1
        }

        function("groupEnd") { _ ->
            consoleGroupDepth = (consoleGroupDepth - 1).coerceAtLeast(0)
        }

        function("time") { args ->
            val label = args.firstOrNull()?.toString() ?: "default"
            consoleTimers[label] = TimeSource.Monotonic.markNow()
        }

        function("timeEnd") { args ->
            val label = args.firstOrNull()?.toString() ?: "default"
            val start = consoleTimers.remove(label)
            if (start == null) {
                printLine("WARN", arrayOf("No such label '$label' for timeEnd"))
            } else {
                val duration = start.elapsedNow()
                printLine(null, arrayOf("$label: ${duration.inWholeMilliseconds}ms"))
            }
        }

        function("count") { args ->
            val label = args.firstOrNull()?.toString() ?: "default"
            val next = (consoleCounts[label] ?: 0) + 1
            consoleCounts[label] = next
            printLine(null, arrayOf("$label: $next"))
        }

        function("table") { args ->
            tablePrint(args.firstOrNull())
        }
    }
}