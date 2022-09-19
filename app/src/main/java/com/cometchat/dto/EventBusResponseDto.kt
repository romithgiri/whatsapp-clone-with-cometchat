package com.cometchat.dto

import android.view.View
import com.cometchat.utils.EventBusOperation
import com.google.gson.annotations.SerializedName

data class EventBusResponseDto(
    @SerializedName("eventBusOperation") val eventBusOperation : EventBusOperation,
    @SerializedName("position") val position: Int,
    @SerializedName("chatId") val chatId: Int?,
    @SerializedName("uid") val uid: String?,
    @SerializedName("conversationID") val conversationID: String?,
    @SerializedName("view") val view: View
)
