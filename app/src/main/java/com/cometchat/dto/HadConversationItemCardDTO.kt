package com.cometchat.dto

class HadConversationItemCardDTO(
    var uid: String?,
    var conversationId: String?,
    var avatar: String?,
    var name: String?,
    var isOnline: Boolean,
    var isTyping: Boolean,
    var conversationType: String,
    var hasJoined: Boolean,
    var lastMessageText: String?,
    var lastMessageSenderId: String?,
    var lastMessageType: String?,
    var lastMessageDeliveredAtTimeStamp: Long,
    var lastMessageDeliveredToMeAtStamp: Long,
    var lastMessageSentAtStamp: Long,
    var lastMessageReadAtStamp: Long,
    var unreadMessageCount: Int
) {

}