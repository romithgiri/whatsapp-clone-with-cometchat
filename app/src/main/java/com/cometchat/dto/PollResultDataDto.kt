package com.cometchat.dto

import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class PollResultDataDto(
    @SerializedName("text"  ) var text  : String? = null,
    @SerializedName("count" ) var count : Int?    = null,
    @SerializedName("voters" ) var voters : JSONObject?    = null
)
