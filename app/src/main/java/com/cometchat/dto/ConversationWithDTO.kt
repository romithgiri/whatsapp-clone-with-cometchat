package com.cometchat.dto

import com.google.gson.annotations.SerializedName

data class ConversationWithDTO (
    @SerializedName("avatar") val avatar : String?,
    @SerializedName("blockedByMe") val blockedByMe : Boolean?,
    @SerializedName("deactivatedAt") val deactivatedAt : Int?,
    @SerializedName("hasBlockedMe") val hasBlockedMe : Boolean?,
    @SerializedName("lastActiveAt") val lastActiveAt : Int?,
    @SerializedName("name") val name : String?,
    @SerializedName("role") val role : String?,
    @SerializedName("status") val status : String?,
    @SerializedName("uid") val uid : String?,
    @SerializedName("createdAt") val createdAt : Int?,
    @SerializedName("guid") val guid : String?,
    @SerializedName("hasJoined") val hasJoined : Boolean?,
    @SerializedName("icon") val icon : String?,
    @SerializedName("joinedAt") val joinedAt : Int?,
    @SerializedName("membersCount") val membersCount : Int?,
    @SerializedName("owner") val owner : String?,
    @SerializedName("scope") val scope : String?,
    @SerializedName("type") val type : String?,
    @SerializedName("updatedAt") val updatedAt : Int?
)