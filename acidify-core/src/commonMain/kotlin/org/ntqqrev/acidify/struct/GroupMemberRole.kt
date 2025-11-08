package org.ntqqrev.acidify.struct

import kotlin.js.JsExport

/**
 * 群成员角色（权限等级）枚举
 */
@JsExport
enum class GroupMemberRole(val value: Int) {
    MEMBER(0),
    ADMIN(2),
    OWNER(1); // it's weird that OWNER's value is 1 while ADMIN's is 2; but that's how it is in the protocol

    companion object {
        fun from(value: Int): GroupMemberRole {
            return entries.firstOrNull { it.value == value } ?: MEMBER
        }
    }
}