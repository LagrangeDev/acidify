package org.ntqqrev.yogurt.util

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.entity.BotFriend
import org.ntqqrev.acidify.entity.BotGroup
import org.ntqqrev.acidify.entity.BotGroupMember
import org.ntqqrev.acidify.event.*
import org.ntqqrev.acidify.getFriend
import org.ntqqrev.acidify.getGroup
import org.ntqqrev.acidify.logging.LogHandler
import org.ntqqrev.acidify.logging.LogLevel
import org.ntqqrev.acidify.logging.shortenPackageName
import org.ntqqrev.acidify.message.MessageScene
import org.ntqqrev.yogurt.YogurtApp
import kotlin.time.Clock
import kotlin.time.Instant

val timeFormat = LocalDateTime.Format {
    hour()
    char(':')
    minute()
    char(':')
    second()
}

fun formatColoredLog(
    level: LogLevel,
    tag: String,
    message: String,
    stackTrace: String?
): String {
    val b = StringBuilder()
    val now: Instant = Clock.System.now()
    val localNow: LocalDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
    b.append(bold(green(timeFormat.format(localNow))))
    b.append(" ")
    b.append(
        when (level) {
            LogLevel.VERBOSE -> gray("TRACE")
            LogLevel.DEBUG -> brightBlue("DEBUG")
            LogLevel.INFO -> green(" INFO")
            LogLevel.WARN -> brightYellow(" WARN")
            LogLevel.ERROR -> brightRed("ERROR")
        }
    )
    b.append(" ")
    b.append(
        when (level) {
            LogLevel.VERBOSE -> gray
            LogLevel.ERROR -> brightRed
            else -> cyan
        }(shortenPackageName(tag))
    )
    b.append(" ")
    b.append(
        when (level) {
            LogLevel.VERBOSE -> gray(message)
            LogLevel.ERROR -> brightRed(message)
            else -> message
        }
    )
    if (stackTrace != null) {
        b.append("\n")
        b.append(
            when (level) {
                LogLevel.ERROR -> brightRed
                LogLevel.WARN -> brightYellow
                else -> gray
            }(stackTrace)
        )
    }
    return b.toString()
}

expect val YogurtApp.logHandler: LogHandler

private val BotFriend.displayName
    get() = remark.ifBlank { nickname }

private val BotGroupMember.displayName
    get() = card.ifBlank { nickname }.joinToSingleLine()

private val BotFriend.displayString: String
    get() = yellow("$displayName ($uin)")

private val BotGroup.displayString: String
    get() = brightGreen("$name ($uin)")

private val BotGroupMember.displayString: String
    get() = brightCyan("$displayName ($uin)")

@Suppress("duplicatedCode")
fun Application.configureEventLogging() = launch {
    val bot = dependencies.resolve<AbstractBot>()
    val logger = bot.createLogger("Logging")

    bot.eventFlow.collect {
        when (it) {
            is MessageReceiveEvent -> {
                val b = StringBuilder()
                when (it.message.scene) {
                    MessageScene.FRIEND -> {
                        val friend = bot.getFriend(it.message.peerUin) ?: return@collect
                        b.append("[${friend.displayString}]")
                    }

                    MessageScene.GROUP -> {
                        val group = bot.getGroup(it.message.peerUin) ?: return@collect
                        val member = group.getMember(it.message.senderUin) ?: return@collect
                        b.append("[${group.displayString}] [${member.displayString}]")
                    }

                    else -> {
                        b.append("[(${it.message.peerUin})]")
                    }
                }
                b.append(" ")
                b.append(
                    it.message.segments.joinToString("")
                        .joinToSingleLine()
                        .shorten(50)
                )

                logger.d { b.toString() }
            }

            is MessageRecallEvent -> {
                val b = StringBuilder()
                when (it.scene) {
                    MessageScene.FRIEND -> {
                        val friend = bot.getFriend(it.peerUin) ?: return@collect
                        b.append("[${friend.displayString}] ")
                        if (it.senderUin == bot.uin) {
                            b.append("你撤回了一条消息")
                        } else {
                            b.append("撤回了一条消息")
                        }
                    }

                    MessageScene.GROUP -> {
                        val group = bot.getGroup(it.peerUin) ?: return@collect
                        val sender = group.getMember(it.senderUin) ?: return@collect
                        val operator = group.getMember(it.operatorUin) ?: return@collect

                        b.append("[${group.displayString}] ")
                        b.append("[${sender.displayString}] ")

                        if (it.senderUin == it.operatorUin) {
                            b.append("撤回了一条消息")
                        } else {
                            b.append("的消息被 [${operator.displayString}] 撤回")
                        }
                    }

                    else -> return@collect
                }
                if (it.displaySuffix.isNotBlank()) {
                    b.append("，${it.displaySuffix}")
                }

                logger.d { b.toString() }
            }

            is BotOfflineEvent -> {
                logger.e { "Bot 已离线，原因：${it.reason}" }
            }

            is FriendNudgeEvent -> {
                val b = StringBuilder()
                val friend = bot.getFriend(it.userUin) ?: return@collect

                if (it.isSelfSend) {
                    b.append("你")
                    b.append(it.displayAction)
                    if (it.isSelfReceive) {
                        b.append("自己")
                    } else {
                        b.append(friend.displayString)
                        b.append(' ')
                    }
                    b.append(it.displaySuffix)
                } else {
                    b.append(friend.displayString)
                    b.append(' ')
                    b.append(it.displayAction)
                    if (it.isSelfReceive) {
                        b.append("你")
                    } else {
                        b.append("自己")
                    }
                    b.append(it.displaySuffix)
                }

                logger.d { b.toString() }
            }

            is FriendRequestEvent -> {
                logger.d { "收到来自 ${it.initiatorUin} 的好友请求，附加信息：${it.comment}" }
            }

            is GroupAdminChangeEvent -> {
                val b = StringBuilder()
                val group = bot.getGroup(it.groupUin) ?: return@collect
                val operator = group.getMember(it.userUin) ?: return@collect

                b.append("[${group.displayString}] [${operator.displayString}] ")
                b.append(if (it.isSet) "被设置为" else "被取消")
                b.append("管理员")

                logger.d { b.toString() }
            }

            is GroupEssenceMessageChangeEvent -> {
                val b = StringBuilder()
                val group = bot.getGroup(it.groupUin) ?: return@collect

                b.append("[${group.displayString}] ")
                b.append("消息 #${it.messageSeq} ")
                b.append(if (it.isSet) "被设置为" else "被取消")
                b.append("精华消息")

                logger.d { b.toString() }
            }

            is GroupInvitationEvent -> {
                logger.d { "${it.initiatorUin} 邀请自己加入群 ${it.groupUin}" }
            }

            is GroupInvitedJoinRequestEvent -> {
                val b = StringBuilder()
                val group = bot.getGroup(it.groupUin) ?: return@collect
                val initiator = group.getMember(it.initiatorUin) ?: return@collect

                b.append("[${group.displayString}] ")
                b.append("[${initiator.displayString}] ")
                b.append("邀请 ${it.targetUserUin} 加入群聊")

                logger.d { b.toString() }
            }

            is GroupJoinRequestEvent -> {
                val b = StringBuilder()
                val group = bot.getGroup(it.groupUin) ?: return@collect

                b.append("[${group.displayString}] ")
                b.append("收到 ${it.initiatorUin} 的入群申请，附加信息：${it.comment} ")

                logger.d { b.toString() }
            }

            is GroupMemberIncreaseEvent -> {
                val b = StringBuilder()
                val group = bot.getGroup(it.groupUin) ?: return@collect

                b.append("[${group.displayString}] ")
                b.append("${it.userUin} ")

                when {
                    it.operatorUin != null -> {
                        val operator = group.getMember(it.operatorUin!!) ?: return@collect
                        b.append("被 [${operator.displayString}] 同意加入群聊")
                    }

                    it.invitorUin != null -> {
                        val invitor = group.getMember(it.invitorUin!!) ?: return@collect
                        b.append("被 [${invitor.displayString}] 邀请加入群聊")
                    }

                    else -> {
                        b.append("加入了群聊")
                    }
                }

                logger.d { b.toString() }
            }

            is GroupMemberDecreaseEvent -> {
                val b = StringBuilder()
                val group = bot.getGroup(it.groupUin) ?: return@collect

                b.append("[${group.displayString}] ")
                b.append("${it.userUin} ")

                if (it.operatorUin != null && it.operatorUin != it.userUin) {
                    val operator = group.getMember(it.operatorUin!!) ?: return@collect
                    b.append("被 [${operator.displayString}] 移出群聊")
                } else {
                    b.append("退出了群聊")
                }

                logger.d { b.toString() }
            }

            is GroupDisbandEvent -> {
                val b = StringBuilder()
                val group = bot.getGroup(it.groupUin) ?: return@collect

                b.append("[${group.displayString}] ")
                b.append("${it.operatorUin} 解散了群聊")

                logger.d { b.toString() }
            }

            is GroupNameChangeEvent -> {
                val b = StringBuilder()
                val group = bot.getGroup(it.groupUin) ?: return@collect
                val operator = group.getMember(it.operatorUin) ?: return@collect

                b.append("[${group.displayString}] ")
                b.append("[${operator.displayString}] ")
                b.append("将群名称修改为：${it.newGroupName}")

                logger.d { b.toString() }
            }

            is GroupMessageReactionEvent -> {
                val b = StringBuilder()
                val group = bot.getGroup(it.groupUin) ?: return@collect
                val user = group.getMember(it.userUin) ?: return@collect

                b.append("[${group.displayString}] ")
                b.append("[${user.displayString}] ")

                if (it.isAdd) {
                    b.append("对消息 #${it.messageSeq} 添加了表情回应")
                } else {
                    b.append("取消了对消息 #${it.messageSeq} 的表情回应")
                }

                b.append(" ")
                bot.faceDetailMap[it.faceId]?.let { detail ->
                    b.append(detail.qDes)
                    b.append(" ")
                }
                b.append("(#${it.faceId}, type=${it.type})")

                logger.d { b.toString() }
            }

            is GroupMuteEvent -> {
                val b = StringBuilder()
                val group = bot.getGroup(it.groupUin) ?: return@collect
                val user = group.getMember(it.userUin) ?: return@collect
                val operator = group.getMember(it.operatorUin) ?: return@collect

                b.append("[${group.displayString}] ")
                b.append("[${user.displayString}] ")

                if (it.duration == 0) {
                    b.append("被 [${operator.displayString}] 解除禁言")
                } else {
                    b.append("被 [${operator.displayString}] 禁言 ${it.duration} 秒")
                }

                logger.d { b.toString() }
            }

            is GroupWholeMuteEvent -> {
                val b = StringBuilder()
                val group = bot.getGroup(it.groupUin) ?: return@collect
                val operator = group.getMember(it.operatorUin) ?: return@collect

                b.append("[${group.displayString}] ")
                b.append("[${operator.displayString}] ")

                if (it.isMute) {
                    b.append("开启了全员禁言")
                } else {
                    b.append("关闭了全员禁言")
                }

                logger.d { b.toString() }
            }

            is GroupNudgeEvent -> {
                val b = StringBuilder()
                val group = bot.getGroup(it.groupUin) ?: return@collect
                val sender = group.getMember(it.senderUin) ?: return@collect
                val receiver = group.getMember(it.receiverUin) ?: return@collect

                b.append("[${group.displayString}] ")
                if (it.senderUin == bot.uin) {
                    b.append("你")
                } else {
                    b.append(sender.displayString)
                    b.append(' ')
                }
                b.append(it.displayAction)
                if (it.receiverUin == bot.uin) {
                    if (it.senderUin == bot.uin) {
                        b.append("自己")
                    } else {
                        b.append("你")
                    }
                } else {
                    b.append(receiver.displayString)
                    b.append(' ')
                }
                b.append(it.displaySuffix)

                logger.d { b.toString() }
            }
        }
    }
}

fun String.joinToSingleLine(): String {
    val b = StringBuilder()
    for (c in this) {
        if (c == '\n' || c == '\r') {
            b.append(' ')
        } else {
            b.append(c)
        }
    }
    return b.toString()
}

fun String.shorten(maxLength: Int, ellipsis: String = "..."): String {
    if (this.length <= maxLength) return this
    if (maxLength <= ellipsis.length) return this.take(maxLength)
    return this.take(maxLength - ellipsis.length) + ellipsis
}
