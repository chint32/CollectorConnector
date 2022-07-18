package com.example.collectorconnector.models

data class Conversation(
    val otherUserId: String,
    val otherUserScreenName: String?,
    val otherUserProfileImgUrl: String?,
    val lastMessage: String?,
    val time: String
)
