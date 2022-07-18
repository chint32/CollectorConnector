package com.example.collectorconnector.models

data class ImageMessage(
    override val messageId: String = "",
    override val senderId: String = "",
    override val senderScreenName: String = "",
    override val recipientId: String = "",
    override val recipientScreenName: String = "",
    override val time: String = "",
    override val type: String = "",
    var image: ByteArray? = null
) : Message(messageId, senderId, recipientId, time, type)
