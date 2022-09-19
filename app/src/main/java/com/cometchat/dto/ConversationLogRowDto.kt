package com.cometchat.dto

import android.content.Context
import android.view.View
import com.cometchat.pro.models.Attachment
import com.cometchat.utils.EventBusOperation
import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class ConversationLogRowDto(
    @SerializedName("context") val context: Context,
    @SerializedName("text") val text: String?,
    @SerializedName("chatID") val chatID: Int?,
    @SerializedName("uid") val uId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("attachment") val attachment: Attachment?,
    @SerializedName("deliveredAt") val deliveredAt: Long,
    @SerializedName("readAt") val readAt: Long,
    @SerializedName("textedBy") val textedBy: String,
    @SerializedName("conversationType") val conversationType: String,
    @SerializedName("tempShareAnyData") val tempShareAnyData: String?,
    @SerializedName("repliedMessagePosition") val repliedMessagePosition: Int,
    @SerializedName("metadata") var metadata: JSONObject?
)

/*data class ConversationLogRowDto(
    @SerializedName("context") val context: Context,
    @SerializedName("text") val text: String?,
    @SerializedName("chatID") val chatID: Int?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("attachment") val attachment: Attachment?,
    @SerializedName("deliveredAt") val deliveredAt: Long,
    @SerializedName("readAt") val readAt: Long,
    @SerializedName("textedBy") val textedBy: String,
    @SerializedName("conversationType") val conversationType: String,
    @SerializedName("metadata") val metadata: JSONObject?
){

    var quote: String = ""
    var quotePos: Int = -1

    constructor(
        context: Context,
        text: String?,
        chatID: Int?,
        avatar: String?,
        timestamp: Long,
        attachment: Attachment?,
        deliveredAt: Long,
        readAt: Long,
        textedBy: String,
        conversationType: String,
        metadata: JSONObject?,
        quote: String,
        quotePos: Int
    ) : this(context, text, chatID, avatar, timestamp, attachment, deliveredAt, readAt, textedBy, conversationType, metadata) {
        this.quote = quote
        this.quotePos = quotePos
    }

}*/
