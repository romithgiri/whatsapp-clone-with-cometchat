package com.cometchat.dto

import com.google.gson.annotations.SerializedName

data class CallInitiator (
    @SerializedName("blockedByMe") var blockedByMe: Boolean? = null,
    @SerializedName("deactivatedAt") var deactivatedAt: Int?     = null,
    @SerializedName("hasBlockedMe") var hasBlockedMe: Boolean? = null,
    @SerializedName("lastActiveAt") var lastActiveAt: Int?     = null,
    @SerializedName("name") var name: String?  = null,
    @SerializedName("role") var role: String?  = null,
    @SerializedName("status") var status: String?  = null,
    @SerializedName("uid") var uid: String?  = null
)
