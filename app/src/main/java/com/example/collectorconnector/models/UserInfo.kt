package com.example.collectorconnector.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class UserInfo(
    var uid: String = "",
    var screenName: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var city: String = "",
    var state: String = "",
    var isLocationSet: Boolean = false,
    var searchDistance: Int = 0,
    var interests: ArrayList<String> = ArrayList<String>(),
    var collectibles: ArrayList<Collectible> = ArrayList<Collectible>(),
    var totalRatingStars: Float = 0f,
    var totalRates: Int = 0,
    var rating: Float = 0f,
    var profileImgUrl: String = "",
    var favoriteCollectibles: ArrayList<Collectible> = ArrayList<Collectible>()
) : Parcelable
