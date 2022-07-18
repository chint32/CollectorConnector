package com.example.collectorconnector.models

data class TradeMessage(
    override val messageId: String = "",
    override val senderId: String = "",
    override val senderScreenName: String = "",
    override val recipientId: String = "",
    override val recipientScreenName: String = "",
    override val time: String = "",
    override val type: String = "",
    val trade: Trade? = null,
    var tradeStatus: String = "OPEN",
    var tradeAcceptanceReceived: Boolean = false
) : Message(messageId, senderId, recipientId, time, type)