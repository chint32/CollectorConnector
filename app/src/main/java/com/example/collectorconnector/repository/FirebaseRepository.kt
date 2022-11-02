package com.example.collectorconnector.repository

import android.net.Uri
import com.example.collectorconnector.models.*
import com.example.collectorconnector.util.Constants
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storageMetadata
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat


object FirebaseRepository {


    // update collectible
    suspend fun updateCollectibleViews(
        userInfo: UserInfo, collectible: Collectible
    ): Boolean {
        return try {

            val index = userInfo.collectibles.indexOf(collectible)
            var views = userInfo.collectibles[index].timesViewed.toInt()
            views++
            userInfo.collectibles[index].timesViewed = views.toString()

            Firebase.firestore.collection("users").document("all_users")
                .collection(userInfo.uid).document("user_info")
                .update("collectibles", userInfo.collectibles).await()

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAppStartingData(): Pair<ArrayList<String>, ArrayList<String>>? {
        return try {
            val categories =
                Firebase.firestore.collection("app_data").document("collectible_categories")
                    .get().await().get("categories") as ArrayList<String>
            val conditions =
                Firebase.firestore.collection("app_data").document("collectible_conditions")
                    .get().await().get("conditions") as ArrayList<String>
            Pair(categories, conditions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getCollectibleConditions(): DocumentSnapshot? {
        return try {
            Firebase.firestore.collection("app_data").document("collectible_conditions")
                .get().await()
        } catch (e: Exception) {
            null
        }
    }

    // login
    suspend fun signInWithEmailAndPw(email: String, password: String): AuthResult? {
        return try {
            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .await()
        } catch (e: Exception) {
            null
        }
    }

    // registration
    suspend fun registerWithEmailAndPw(email: String, password: String): AuthResult? {
        return try {

            val authResult = FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .await()

            val data =
                Firebase.firestore.collection("users")
                    .document("all_users").get().await().data
            var users: ArrayList<String>
            if (data == null)
                users = ArrayList()
            else users = data["list"] as ArrayList<String>
            users.add(authResult.user!!.uid)
            val allUsers = AllUsers(users)

            Firebase.firestore.collection("users").document("all_users").set(allUsers).await()

            authResult
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // get user info
    suspend fun getUserInfo(uid: String): UserInfo? {
        return try {
            Firebase.firestore.collection("users").document("all_users")
                .collection(uid).document("user_info")
                .get().await().toObject(UserInfo::class.java)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // add user profile image
    suspend fun updateProfile(
        userInfo: UserInfo,
        filepath: Uri?,
    ): UserInfo? {
        return try {
            if (filepath != null) {
                val upload =
                    FirebaseStorage.getInstance().reference.child("users").child(userInfo.uid)
                        .child("profile_img")
                        .child("profile_img").putFile(filepath).await()
                userInfo.profileImgUrl = upload.storage.downloadUrl.await().toString()
            }
            Firebase.firestore.collection("users").document("all_users")
                .collection(userInfo.uid).document("user_info").set(userInfo).await()

            userInfo
        } catch (e: Exception) {
            null
        }
    }


    // add collectible to user
    suspend fun addCollectible(
        userInfo: UserInfo,
        filepath: Uri,
        collectible: Collectible
    ): Collectible? {
        return try {
            val metadata = storageMetadata {
                contentType = "image"
            }
            val upload = FirebaseStorage.getInstance().reference.child("users").child(userInfo.uid)
                .child("collectibles").child(collectible.uid)
                .putFile(filepath, metadata).await()
            collectible.imageUrl = upload.storage.downloadUrl.await().toString()
            userInfo.collectibles.add(collectible)
            Firebase.firestore.collection("users").document("all_users")
                .collection(userInfo.uid).document("user_info")
                .set(userInfo).await()
            collectible
        } catch (e: Exception) {
            null
        }
    }

    // delete collectible from user
    suspend fun deleteCollectible(userInfo: UserInfo, collectible: Collectible): Collectible? {
        return try {
            // check if any users have favorited that collectible. If so, remove it from their favorites
            val userList = Firebase.firestore.collection("users")
                .document("all_users").get().await().data!!.get("list") as ArrayList<String>
            for (user in userList) {
                val userInfoCheck = Firebase.firestore.collection("users").document("all_users")
                    .collection(user).document("user_info").get().await()
                    .toObject(UserInfo::class.java)
                if (userInfoCheck!!.favoriteCollectibles.contains(collectible)) {
                    userInfoCheck.favoriteCollectibles.remove(collectible)
                    updateProfile(userInfoCheck, null)
                }
            }

            // delete collectible image from firebase storage
            FirebaseStorage.getInstance().reference.child("users").child(userInfo.uid)
                .child("collectibles").child(collectible.uid).delete().await()
            userInfo.collectibles.remove(collectible)

            // delete collectible from user info
            Firebase.firestore.collection("users").document("all_users")
                .collection(userInfo.uid).document("user_info")
                .set(userInfo).await()

            collectible
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getMainFeed(
        userInfo: UserInfo,
        isUsingSearch: Boolean,
        searchValue: String?,
        tagsList: ArrayList<String>?,
        conditionsList: ArrayList<String>?
    ): Pair<ArrayList<Collectible>, ArrayList<Collectible>>? {
        return try {

            val collectiblesWithinDistance = ArrayList<Collectible>()
            val collectiblesOutOfDistance = ArrayList<Collectible>()
            val allUsers = Firebase.firestore.collection("users").document("all_users")
                .get().await().get("list") as ArrayList<String>
            for (userId in allUsers) {
                if (userId == userInfo.uid) continue

                val otherUserInfo =
                    Firebase.firestore.collection("users").document("all_users")
                        .collection(userId).document("user_info").get().await()
                        .toObject(UserInfo::class.java)

                if (userInfo.isLocationSet && otherUserInfo!!.isLocationSet) {
                    val distance = distance(
                        userInfo.latitude.toFloat(),
                        userInfo.longitude.toFloat(),
                        otherUserInfo!!.latitude.toFloat(),
                        otherUserInfo.longitude.toFloat(),
                    )

                    if (distance > userInfo.searchDistance) continue
                    if (isUsingSearch) {
                        for (collectible in otherUserInfo.collectibles) {
                            val nameAndDescription =
                                collectible.name + " " + collectible.description
                            if (searchValue != "") {
                                if (!nameAndDescription.contains(searchValue!!))
                                    continue
                            }
                            if (tagsList!!.isNotEmpty()) {
                                var correctTag = false
                                for (tag in collectible.tags) {
                                    if (tagsList!!.contains(tag)) {
                                        correctTag = true
                                    }
                                }
                                if (!correctTag) continue
                            }
                            if (conditionsList!!.isNotEmpty()) {
                                var correctCondition = false
                                for (condition in conditionsList) {
                                    if (condition == collectible.condition) {
                                        correctCondition = true
                                    }
                                }
                                if (!correctCondition) continue
                            }

                            collectiblesWithinDistance.add(collectible)
                        }
                    } else {
                        for (collectible in otherUserInfo.collectibles) {
                            for (tag in collectible.tags) {
                                if (userInfo.interests.contains(tag)) {
                                    collectiblesWithinDistance.add(collectible)
                                    break
                                }
                            }
                        }
                    }
                } else {
                    if (isUsingSearch) {
                        for (collectible in otherUserInfo!!.collectibles) {
                            val nameAndDescription =
                                collectible.name + " " + collectible.description
                            if (searchValue != "") {
                                if (!nameAndDescription.contains(searchValue!!))
                                    continue
                            }
                            if (tagsList!!.isNotEmpty()) {
                                var correctTag = false
                                for (tag in collectible.tags) {
                                    if (tagsList!!.contains(tag)) {
                                        correctTag = true
                                    }
                                }
                                if (!correctTag) continue
                            }
                            if (conditionsList!!.isNotEmpty()) {
                                var correctCondition = false
                                for (condition in conditionsList) {
                                    if (condition == collectible.condition) {
                                        correctCondition = true
                                    }
                                }
                                if (!correctCondition) continue
                            }

                            collectiblesOutOfDistance.add(collectible)
                        }
                    } else {
                        if (otherUserInfo!!.collectibles.size > 0) {
                            for (collectible in otherUserInfo.collectibles) {
                                for (tag in collectible.tags) {
                                    if (userInfo.interests.contains(tag)) {
                                        collectiblesOutOfDistance.add(collectible)
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Pair(collectiblesWithinDistance, collectiblesOutOfDistance)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun distance(lat_a: Float, lng_a: Float, lat_b: Float, lng_b: Float): Float {
        val earthRadius = 3958.75
        val latDiff = Math.toRadians((lat_b - lat_a).toDouble())
        val lngDiff = Math.toRadians((lng_b - lng_a).toDouble())
        val a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(lat_a.toDouble())) * Math.cos(Math.toRadians(lat_b.toDouble())) *
                Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distance = earthRadius * c

        return distance.toFloat()
    }

    // send textMessage to other user
    suspend fun sendTextMessageToUser(
        thisUserInfo: UserInfo,
        otherUserInfo: UserInfo,
        textMessage: TextMessage
    ): Boolean {
        return try {
            var conversation = Conversation(
                otherUserInfo.uid,
                otherUserInfo.screenName,
                otherUserInfo.profileImgUrl,
                textMessage.text,
                textMessage.time
            )

            Firebase.firestore.collection("users")
                .document("all_users").collection(thisUserInfo.uid).document("user_info")
                .collection("conversations").document(otherUserInfo.uid).set(conversation).await()

            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(thisUserInfo.uid).document("user_info")
                .collection("conversations").document(otherUserInfo.uid)
                .collection("messages").document(textMessage.messageId)
                .set(textMessage).await()

            conversation = Conversation(
                thisUserInfo.uid,
                thisUserInfo.screenName,
                thisUserInfo.profileImgUrl,
                textMessage.text,
                textMessage.time
            )

            Firebase.firestore.collection("users")
                .document("all_users").collection(otherUserInfo.uid).document("user_info")
                .collection("conversations").document(thisUserInfo.uid).set(conversation).await()

            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(otherUserInfo.uid).document("user_info")
                .collection("conversations").document(thisUserInfo.uid)
                .collection("messages").document(textMessage.messageId)
                .set(textMessage).await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }


    // send textMessage to other user
    suspend fun sendImageMessageToUser(
        thisUserInfo: UserInfo,
        otherUserInfo: UserInfo,
        imageMessage: ImageMessage,
        uri: Uri
    ): Boolean {
        return try {
            var conversation = Conversation(
                otherUserInfo.uid,
                otherUserInfo.screenName,
                otherUserInfo.profileImgUrl,
                "image",
                imageMessage.time
            )

            var metadata = storageMetadata {
                contentType = "image/jpg"
            }


            //update conversation item on firestore for user 1
            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(thisUserInfo.uid).document("user_info").collection("conversations")
                .document(otherUserInfo.uid).set(conversation).await()

            //update image messages on cloud storage for user 1
            FirebaseStorage.getInstance().reference.child("users").child(thisUserInfo.uid)
                .child("conversations").child(otherUserInfo.uid).child("image_messages")
                .child(imageMessage.messageId).putFile(uri, metadata).addOnSuccessListener {

                    FirebaseStorage.getInstance().reference.child("users").child(thisUserInfo.uid)
                        .child("conversations").child(otherUserInfo.uid).child("image_messages")
                        .child(imageMessage.messageId).downloadUrl.addOnSuccessListener {
                            imageMessage.imageUrl = it.toString()

                            // update messages on firestore for user 1
                            FirebaseFirestore.getInstance().collection("users")
                                .document("all_users")
                                .collection(thisUserInfo.uid).document("user_info")
                                .collection("conversations")
                                .document(otherUserInfo.uid).collection("messages")
                                .document(imageMessage.messageId)
                                .set(imageMessage)
                        }
                }


            conversation = Conversation(
                thisUserInfo.uid,
                thisUserInfo.screenName,
                thisUserInfo.profileImgUrl,
                "image",
                imageMessage.time
            )

            //update conversation item on firestore for user 2
            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(otherUserInfo.uid).document("user_info").collection("conversations")
                .document(thisUserInfo.uid).set(conversation).await()

            //update image messages on cloud storage for user 2
            FirebaseStorage.getInstance().reference.child("users").child(otherUserInfo.uid)
                .child("conversations").child(thisUserInfo.uid).child("image_messages")
                .child(imageMessage.messageId).putFile(uri, metadata).addOnSuccessListener {

                    FirebaseStorage.getInstance().reference.child("users").child(otherUserInfo.uid)
                        .child("conversations").child(thisUserInfo.uid).child("image_messages")
                        .child(imageMessage.messageId).downloadUrl.addOnSuccessListener {
                            imageMessage.imageUrl = it.toString()

                            // update messages on firestore for user 2
                            FirebaseFirestore.getInstance().collection("users")
                                .document("all_users")
                                .collection(otherUserInfo.uid).document("user_info")
                                .collection("conversations")
                                .document(thisUserInfo.uid).collection("messages")
                                .document(imageMessage.messageId)
                                .set(imageMessage)
                        }
                }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }


    suspend fun sendTradeOfferMessage(
        thisUserInfo: UserInfo,
        otherUserInfo: UserInfo,
        tradeMessage: TradeMessage
    ): Boolean {
        return try {

            // update conversation last message
            var conversation = Conversation(
                otherUserInfo.uid,
                otherUserInfo.screenName,
                otherUserInfo.profileImgUrl,
                "Trade Offer",
                tradeMessage.time
            )

            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(thisUserInfo.uid).document("user_info").collection("conversations")
                .document(otherUserInfo.uid).set(conversation).await()

            conversation = Conversation(
                thisUserInfo.uid,
                thisUserInfo.screenName,
                thisUserInfo.profileImgUrl,
                "Trade Offer",
                tradeMessage.time
            )

            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(otherUserInfo.uid).document("user_info").collection("conversations")
                .document(thisUserInfo.uid).set(conversation).await()


            // update messages
            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(thisUserInfo.uid).document("user_info").collection("conversations")
                .document(otherUserInfo.uid).collection("messages").document(tradeMessage.messageId)
                .set(tradeMessage).await()

            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(otherUserInfo.uid).document("user_info").collection("conversations")
                .document(thisUserInfo.uid).collection("messages").document(tradeMessage.messageId)
                .set(tradeMessage).await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun checkForAcceptedTrades(userId: String): TradeMessage? {
        val conversationDocs =
            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(userId).document("user_info").collection("conversations").get().await()

        for (doc in conversationDocs) {

            val conversation = doc.toObject(Conversation::class.java)
            val messagesDoc =
                FirebaseFirestore.getInstance().collection("users").document("all_users")
                    .collection(userId).document("user_info").collection("conversations")
                    .document(conversation.otherUserId).collection("messages").get().await()
            for (item in messagesDoc) {
                if (item["type"] == Constants.MESSAGE_TYPE_TRADE) {
                    val tradeMessage = item.toObject(TradeMessage::class.java)
                    if (tradeMessage.tradeStatus == Constants.TRADE_STATUS_ACCEPTED && !tradeMessage.tradeAcceptanceReceived) {
                        return tradeMessage
                    }
                }
            }
        }

        return null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun listenForTextMessagesFromOtherUser(
        userId: String,
        otherUserId: String
    ): Flow<ArrayList<Message>?> = callbackFlow {

        val eventDocument =
            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(userId).document("user_info").collection("conversations")
                .document(otherUserId).collection("messages").orderBy("time")

        // Generate a subscription that is going to let us listen for changes with
        // .addSnapshotListener and then offer those values to the channel that will be collected in viewmodel
        val subscription = eventDocument.addSnapshotListener { snapshot, _ ->
            if (!snapshot!!.isEmpty && snapshot.documents.isNotEmpty()) {
                val messages = ArrayList<Message>()
                for (doc in snapshot.documents) {
                    val type = doc!!.get("type").toString()
                    if (type == Constants.MESSAGE_TYPE_TEXT) {
                        val textMessage = doc.toObject(TextMessage::class.java)!!
                        if (!messages.contains(textMessage)) {
                            messages.add(textMessage)
                        }
                    } else if (type == Constants.MESSAGE_TYPE_IMAGE) {
                        // messages of type IMAGE retrieved from firestore
                        // need to get the actual image from firebase storage
                        val imageMessage = doc.toObject(ImageMessage::class.java)!!
                        if (!messages.contains(imageMessage)) {
                            messages.add(imageMessage)
                        }
                    } else if (type == Constants.MESSAGE_TYPE_TRADE) {
                        //message of type TRADE retrieved from firestore.
                        //need to get images from firebase storage
                        // this is handled in the message adapter class
                        val tradeMessage = doc.toObject(TradeMessage::class.java) as TradeMessage
                        if (!messages.contains(tradeMessage)) {
                            messages.add(tradeMessage)
                        }
                    }
                }
                messages.sortWith(compareBy {
                    SimpleDateFormat("dd-MM-yy HH:mm").parse(it.time)
                })
                offer(messages)
            } else
                offer(null)
        }

        //Finally if collect is not in use or collecting any data, cancel this channel to prevent
        // any leak and remove the subscription listener to the database
        awaitClose { subscription.remove() }
    }


    suspend fun getConversationsForUser(userId: String): QuerySnapshot? {
        return try {
            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(userId).document("user_info").collection("conversations").get().await()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // update trade status
    suspend fun updateTradeStatus(
        userInfo: UserInfo,
        otherUserInfo: UserInfo,
        tradeMessage: TradeMessage
    ): TradeMessage? {
        return try {

            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(otherUserInfo.uid).document("user_info").collection("conversations")
                .document(userInfo.uid).collection("messages").document(tradeMessage.messageId)
                .set(tradeMessage).await()

            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(userInfo.uid).document("user_info").collection("conversations")
                .document(otherUserInfo.uid).collection("messages").document(tradeMessage.messageId)
                .set(tradeMessage).await()

            tradeMessage
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // update trade status
    suspend fun updateTradeAcceptanceReceived(
        userId: String,
        otherUserId: String,
        tradeMessage: TradeMessage,
        deleteCollectibles: Boolean
    ): UserInfo? {
        return try {

            FirebaseFirestore.getInstance().collection("users").document("all_users")
                .collection(userId).document("user_info").collection("conversations")
                .document(otherUserId).collection("messages").document(tradeMessage.messageId)
                .set(tradeMessage).await()

            val otherUserInfo = getUserInfo(otherUserId)
            if (deleteCollectibles) {
                val currentUserInfo = getUserInfo(userId)
                println("current user = ${currentUserInfo!!.screenName}")
                for (collectible in tradeMessage.trade?.senderCollectibles!!) {
                    if (currentUserInfo.collectibles.contains(collectible))
                        deleteCollectible(currentUserInfo, collectible)
                }
                println("other user = ${otherUserInfo!!.screenName}")
                for (collectible in otherUserInfo.collectibles) {
                    if (otherUserInfo.collectibles.contains(collectible))
                        deleteCollectible(otherUserInfo, collectible)
                }
            }
            otherUserInfo

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}