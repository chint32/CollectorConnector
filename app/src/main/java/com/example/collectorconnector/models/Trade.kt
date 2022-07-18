package com.example.collectorconnector.models


data class Trade(
    val tradeId: String = "",
    val senderUid: String = "",
    val receiverUid: String = "",
    val senderCollectibles: ArrayList<Collectible> = ArrayList(),
    val receiverCollectibles: ArrayList<Collectible> = ArrayList()
    )
