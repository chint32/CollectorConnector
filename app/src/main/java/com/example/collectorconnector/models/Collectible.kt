package com.example.collectorconnector.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Collectible (
    val uid: String = "",
    var name: String = "",
    var description: String = "",
    var condition: String = "",
    var imageByteArray: ByteArray? = null,
    var tags: ArrayList<String> = ArrayList(),
    val ownerId: String = ""
) : Parcelable


