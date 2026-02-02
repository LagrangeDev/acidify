package org.ntqqrev.acidify.internal.proto.message

internal enum class PushMsgType(val value: Int) {
    FriendMessage(166),
    GroupMessage(82),
    TempMessage(141),
    Event0x210(0x210),                 // friend related event
    Event0x2DC(0x2DC),                 // group related event
    FriendRecordMessage(208),
    FriendFileMessage(529),
    GroupInvitedJoinRequest(525),      // from group member invitation
    GroupJoinRequest(84),              // directly entered
    GroupInvitation(87),               // the bot self is being invited
    GroupAdminChange(44),              // admin change, both on and off
    GroupMemberIncrease(33),
    GroupMemberDecrease(34);

    companion object {
        fun from(value: Int): PushMsgType? {
            return entries.firstOrNull { it.value == value }
        }
    }
}