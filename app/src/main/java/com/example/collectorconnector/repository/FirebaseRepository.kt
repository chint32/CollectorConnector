package com.example.collectorconnector.repository

import android.net.Uri
import com.example.collectorconnector.models.*
import com.example.collectorconnector.util.Constants.ONE_HUNDRED_MEGABYTE
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storageMetadata
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.collections.ArrayList


object FirebaseRepository {

    suspend fun getCollectibleCategories(): DocumentSnapshot? {
        return try {
            Firebase.firestore.collection("app_data").document("collectible_categories")
                .get().await()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStatesAndCities(): DocumentSnapshot? {
        return try {
            Firebase.firestore.collection("app_data").document("locations")
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

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .await()


        } catch (e: Exception) {
            null
        }
    }

    // update user info
    suspend fun updateUserInfo(userInfo: UserInfo): Boolean {
        return try {
            Firebase.firestore.collection("users").document(userInfo.uid)
                .collection("user_info").document("user_info_doc")
                .set(userInfo).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // get user info
    suspend fun getUserInfo(uid: String): DocumentSnapshot? {
        return try {
            Firebase.firestore.collection("users").document(uid)
                .collection("user_info").document("user_info_doc")
                .get().await()
        } catch (e: Exception) {
            null
        }
    }

    // get all users with collectibles
    suspend fun getAllUsersWithCollectibles(): ListResult? {
        return try {
            FirebaseStorage.getInstance().reference.child("users")
                .listAll().await()
        } catch (e: Exception) {
            return null
        }
    }

    // get user collectibles based on uid
    suspend fun getCollectiblesByUserId(uid: String): ListResult? {
        return try {
            FirebaseStorage.getInstance().reference.child("users")
                .child(uid).child("collectibles").listAll().await()
        } catch (e: Exception) {
            null
        }
    }

//    suspend fun getProfileImgUrl(userId: String): String?{
//        return try {
//            FirebaseStorage.getInstance().reference.child("users").child(userId)
//                .child("profile_img").child("profile_img").downloadUrl.await().toString()
//        } catch (e: Exception) {
//            null
//        }
//    }

    // add user profile image
    suspend fun addProfileImg(
        userId: String,
        filepath: Uri,
    ): Uri? {
        return try {
            FirebaseStorage.getInstance().reference.child("users").child(userId)
                .child("profile_img")
                .child("profile_img").putFile(filepath).await()
            FirebaseStorage.getInstance().reference.child("users").child(userId)
            .child("profile_img").child("profile_img").downloadUrl.await()
        } catch (e: Exception) {
           null
        }
    }


    // add collectible to user
    suspend fun addCollectible(
        userId: String,
        collectibleId: String,
        filepath: Uri,
        metadata: StorageMetadata
    ): Boolean {
        return try {
            FirebaseStorage.getInstance().reference.child("users").child(userId)
                .child("collectibles").child(collectibleId)
                .putFile(filepath, metadata).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // delete collectible from user
    suspend fun deleteCollectible(userId: String, collectibleId: String): Boolean {
        return try {
            FirebaseStorage.getInstance().reference.child("users").child(userId)
                .child("collectibles").child(collectibleId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // send textMessage to other user
    suspend fun sendTextMessageToUser(
        userId: String,
        otherUserId: String,
        textMessage: TextMessage
    ): Boolean {
        return try {
            val conversation = Conversation(
                otherUserId, null, null,textMessage.text, textMessage.time
            )

            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("conversations").document(otherUserId).set(conversation)

            FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                .collection("conversations").document(userId)
                .collection("messages").document(textMessage.messageId)
                .set(textMessage).await()

            FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                .collection("conversations").document(userId).set(conversation)

            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("conversations").document(otherUserId)
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
        userId: String,
        otherUserId: String,
        imageMessage: ImageMessage,
        uri: Uri
    ): Boolean {
        return try {
            val conversation = Conversation(
                otherUserId, null, null, "image", imageMessage.time
            )

            var metadata = storageMetadata {
                contentType = "image/jpg"
                setCustomMetadata("senderId", userId)
                setCustomMetadata("receiverId", otherUserId)
                setCustomMetadata("time", imageMessage.time)
            }



            //update conversation item on firestore for user 1
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("conversations").document(otherUserId).set(conversation)

            //update image messages on cloud storage for user 1
            FirebaseStorage.getInstance().reference.child("users").child(userId)
                .child("conversations").child(otherUserId).child("image_messages")
                .child(imageMessage.messageId).putFile(uri, metadata).await()



            //update conversation doc for user 2
            FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                .collection("conversations").document(userId).set(conversation)

            //update image messages on cloud storage for user 2
            FirebaseStorage.getInstance().reference.child("users").child(otherUserId)
                .child("conversations").child(userId).child("image_messages")
                .child(imageMessage.messageId).putFile(uri, metadata).await()

            imageMessage.image = null

            // update messages on firestore for user 1
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("conversations").document(otherUserId)
                .collection("messages").document(imageMessage.messageId)
                .set(imageMessage)

            // update messages on firestore for user 2
            FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                .collection("conversations").document(userId)
                .collection("messages").document(imageMessage.messageId)
                .set(imageMessage)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun sendTradeOfferMessage(
        userId: String,
        otherUserId: String,
        tradeMessage: TradeMessage
    ): Boolean {
        return try {


            // sender
            for(i in tradeMessage.trade!!.senderCollectibles.indices){

                val metadata = storageMetadata {
                    setCustomMetadata("collectible_id", tradeMessage.trade.senderCollectibles[i].uid)
                }
                // update cloud storage where trade collectible images are stored
                FirebaseStorage.getInstance().reference.child("users").child(userId)
                    .child("conversations").child(otherUserId).child("trade_offers")
                    .child(tradeMessage.messageId).child("sender").child(UUID.randomUUID().toString())
                    .putBytes(tradeMessage.trade.senderCollectibles[i].imageByteArray!!, metadata)
                    .await()

                // update cloud storage where trade collectible images are stored
                FirebaseStorage.getInstance().reference.child("users").child(userId)
                    .child("conversations").child(otherUserId).child("trade_offers")
                    .child(tradeMessage.messageId).child("receiver").child(UUID.randomUUID().toString())
                    .putBytes(tradeMessage.trade.receiverCollectibles[i].imageByteArray!!, metadata)
                    .await()


            }

            // receiver
            for(i in tradeMessage.trade.receiverCollectibles.indices){

                val metadata = storageMetadata {
                    setCustomMetadata("collectible_id", tradeMessage.trade.receiverCollectibles[i].uid)
                }
                // update cloud storage where trade collectible images are stored
                FirebaseStorage.getInstance().reference.child("users").child(otherUserId)
                    .child("conversations").child(userId).child("trade_offers")
                    .child(tradeMessage.messageId).child("receiver").child(UUID.randomUUID().toString())
                    .putBytes(tradeMessage.trade.receiverCollectibles[i].imageByteArray!!, metadata)
                    .await()

                // update cloud storage where trade collectible images are stored
                FirebaseStorage.getInstance().reference.child("users").child(otherUserId)
                    .child("conversations").child(userId).child("trade_offers")
                    .child(tradeMessage.messageId).child("sender").child(UUID.randomUUID().toString())
                    .putBytes(tradeMessage.trade.senderCollectibles[i].imageByteArray!!, metadata)
                    .await()

            }

            for(collectible in tradeMessage.trade.senderCollectibles)
                collectible.imageByteArray = null
            for(collectible in tradeMessage.trade.receiverCollectibles)
                collectible.imageByteArray = null


            // update last conversation last message
            val conversation = Conversation(
                otherUserId, null, null,"Trade Offer", tradeMessage.time
            )

            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("conversations").document(otherUserId).set(conversation)

            FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                .collection("conversations").document(userId).set(conversation)


            // update messages
            FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                .collection("conversations").document(userId)
                .collection("messages").document(tradeMessage.messageId)
                .set(tradeMessage).await()

            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("conversations").document(otherUserId)
                .collection("messages").document(tradeMessage.messageId)
                .set(tradeMessage).await()
            true
        } catch (e: Exception){
            e.printStackTrace()
            return false
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun listenForTextMessagesFromOtherUser(
        userId: String,
        otherUserId: String
    ): Flow<MutableList<DocumentSnapshot?>?> = callbackFlow {

        val eventDocument = FirebaseFirestore.getInstance().collection("users")
            .document(userId).collection("conversations")
            .document(otherUserId).collection("messages")

        // Generate a subscription that is going to let us listen for changes with
        // .addSnapshotListener and then offer those values to the channel that will be collected in viewmodel
        val subscription = eventDocument.addSnapshotListener { snapshot, _ ->
            if (!snapshot!!.isEmpty && snapshot.documents.isNotEmpty())
                offer(snapshot.documents)
            else
                offer(null)
        }

        //Finally if collect is not in use or collecting any data we cancel this channel to prevent any leak and remove the subscription listener to the database
        awaitClose { subscription.remove() }
    }

    suspend fun getTradeMessagesForUser(userId: String): QuerySnapshot? {
        return try {
            Firebase.firestore.collection("users").document(userId)
                .collection("conversations").get().await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    }

    // get image messages for user
    suspend fun getImageFromImageMessage(uid: String, otherUserId: String, imageMessage: ImageMessage): ByteArray? {
        return try {
            //update image messages on cloud storage for user 1
            FirebaseStorage.getInstance().reference.child("users").child(uid)
                .child("conversations").child(otherUserId).child("image_messages")
                .child(imageMessage.messageId).getBytes(ONE_HUNDRED_MEGABYTE).await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // get images for trade for user
    suspend fun getImagesForTradeReceiver(userId: String, otherUserId: String, tradeId: String): ListResult? {
        return try {
            //update images for trade
            FirebaseStorage.getInstance().reference.child("users").child(userId)
                .child("conversations").child(otherUserId).child("trade_offers")
                .child(tradeId).child("receiver")
                .listAll().await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // get images for trade for user
    suspend fun getImagesForTradeSender(userId: String, otherUserId: String, tradeId: String): ListResult? {
        return try {
            //update images for trade
            FirebaseStorage.getInstance().reference.child("users").child(userId)
                .child("conversations").child(otherUserId).child("trade_offers")
                .child(tradeId).child("sender")
                .listAll().await()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getConversationsForUser(userId: String): QuerySnapshot? {
        return try {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("conversations").get().await()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // update trade status
    suspend fun updateTradeStatus(userId: String, otherUserId: String, tradeMessage: TradeMessage): TradeMessage? {
        return try {

            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("conversations").document(otherUserId)
                .collection("messages").document(tradeMessage.messageId)
                .set(tradeMessage).await()

            FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                .collection("conversations").document(userId)
                .collection("messages").document(tradeMessage.messageId)
                .set(tradeMessage).await()
            tradeMessage
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // update trade status
    suspend fun updateTradeAcceptanceReceived(userId: String, otherUserId: String, tradeMessage: TradeMessage): TradeMessage? {
        return try {

            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("conversations").document(otherUserId)
                .collection("messages").document(tradeMessage.messageId)
                .set(tradeMessage).await()

            tradeMessage
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}