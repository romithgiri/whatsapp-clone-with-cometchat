package com.cometchat.utils

object AppConstant {
    const val selectImageCode = 200
    const val cameraImageCode = 201
    const val filePickerCode = 202

    const val notificationMessageChannelName = "messageNotification"
    const val notificationMessageChannelID = "messageNotificationID"
    const val notificationMessageChannelGroupID = "notificationMessageChannelGroupID"

    object TextedBy {
        const val SENDER = "Sender"
        const val RECEIVER = "Receiver"
    }

    object FetchMessageType {
        const val OLD_MESSAGES = "oldMessages"
        const val MISSED_MESSAGES = "missedMessages"
    }

    object OperationType {
        const val IncomingCall = "IncomingCall"
        const val OutgoingCall = "OutgoingCall"
    }

    object IntentStrings {
        const val NAME = "name"
        const val TYPE = "type"
        const val SESSION_ID = "sessionId"
        const val ID = "id"
        const val CALL_TYPE = "call_type"
    }


}