package com.example.collectorconnector.util

object Constants {

    val tagsArray =
        arrayOf("Coins", "Cards", "Stamps", "Comic Books", "Action Figures", "Vinyl Records")

    val MESSAGE_TYPE_TEXT = "TEXT"
    val MESSAGE_TYPE_IMAGE = "IMAGE"
    val MESSAGE_TYPE_TRADE = "TRADE"

    val TRADE_STATUS_OPEN = "OPEN"
    val TRADE_STATUS_CANCELED = "CANCELED"
    val TRADE_STATUS_ACCEPTED = "ACCEPTED"
    val TRADE_STATUS_REJECTED = "REJECTED"

    val ONE_HUNDRED_MEGABYTE: Long = 1024 * 1024 * 100
    val PICK_IMAGE_REQUEST = 22
}