package com.cometchat.dto

import com.google.gson.annotations.SerializedName

data class SpanIndexDto (
    @SerializedName("start") var start: Int    = 0,
    @SerializedName("end")   var end: Int     = 0,
    @SerializedName("metadata")   var metadataDto: GroupMentionedUserDto
)
