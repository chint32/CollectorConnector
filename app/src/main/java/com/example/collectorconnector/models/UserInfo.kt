package com.example.collectorconnector.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class UserInfo(
    val uid: String = "",
    val screenName: String = "",
    var city: String = "",
    var state: String = "",
    var tags: ArrayList<String> = ArrayList<String>(),
    var totalRatingStars: Float = 0f,
    var totalRates: Float = 0f,
    var rating: Float = 0f,
    var profileImgUrl: String = ""
) : Parcelable
