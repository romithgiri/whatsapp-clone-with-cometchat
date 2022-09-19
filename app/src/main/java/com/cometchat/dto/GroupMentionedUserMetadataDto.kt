package com.cometchat.dto

import com.google.gson.annotations.SerializedName

data class GroupMentionedUserMetadataDto (
    @SerializedName("mentionedUser" ) var mentionedUser : ArrayList<GroupMentionedUserDto> = arrayListOf()
)