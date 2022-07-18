package com.example.collectorconnector.models

open class Message(
    open val messageId: String = "",
    open val senderId: String = "",
    open val senderScreenName: String= "",
    open val recipientId: String = "",
    open val recipientScreenName: String = "",
    open val time: String = "",
    open val type: String = ""
)
