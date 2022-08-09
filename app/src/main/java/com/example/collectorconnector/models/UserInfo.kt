package com.example.collectorconnector.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class UserInfo(
    val uid: String = "",
    val screenName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    var searchDistance: Int = 0,
    var interests: ArrayList<String> = ArrayList<String>(),
    var totalRatingStars: Float = 0f,
    var totalRates: Int = 0,
    var rating: Float = 0f,
    var profileImgUrl: String = "",
    val favoriteCollectibles: ArrayList<FavoriteCollectible> = ArrayList()
) : Parcelable
