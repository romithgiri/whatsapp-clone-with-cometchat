package com.cometchat.dto

import com.google.gson.annotations.SerializedName

class ConversationListDto (
    @SerializedName("avatar") val avatar : String,
    @SerializedName("blockedByMe") val blockedByMe : Boolean,
    @SerializedName("deactivatedAt") val deactivatedAt : Int,
    @SerializedName("hasBlockedMe") val hasBlockedMe : Boolean,
    @SerializedName("lastActiveAt") val lastActiveAt : Int,
    @SerializedName("name") val name : String,
    @SerializedName("role") val role : String,
    @SerializedName("status") val status : String,
    @SerializedName("uid") val uid : String
)