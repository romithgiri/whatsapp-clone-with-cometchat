package com.cometchat.dto

import com.google.gson.annotations.SerializedName

data class RepliedOnDto (
    @SerializedName("chatID"        ) var chatID        : Int?    = null,
    @SerializedName("userName"      ) var userName      : String? = null,
    @SerializedName("userID"        ) var userID        : String? = null,
    @SerializedName("repliedOnText" ) var repliedOnText : String? = null
)