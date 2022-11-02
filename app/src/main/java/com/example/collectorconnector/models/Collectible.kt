package com.example.collectorconnector.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Collectible (
    var uid: String = "",
    var name: String = "",
    var description: String = "",
    var condition: String = "",
    var imageUrl: String = "",
    var timesViewed: String = "0",
    var tags: ArrayList<String> = ArrayList(),
    var ownerId: String = ""
) : Parcelable


