package com.cometchat.dto

import com.google.gson.annotations.SerializedName

data class GroupMentionedUserDto (
    @SerializedName("userID") var userID: String,
    @SerializedName("userName") var userName: String
)
