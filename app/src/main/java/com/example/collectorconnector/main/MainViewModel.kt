package com.example.collectorconnector.main

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.collectorconnector.models.*
import com.example.collectorconnector.repository.FirebaseRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository: FirebaseRepository = FirebaseRepository

    var userInfo = UserInfo()
    val collectibleDeletedLiveData = MutableLiveData<Boolean>()
    var collectible = Collectible()

    private val _mainFeedCollectiblesNearLiveData = MutableLiveData<ArrayList<Collectible>?>()
    val mainFeedCollectiblesNearLiveData: LiveData<ArrayList<Collectible>?>
        get() = _mainFeedCollectiblesNearLiveData

    private val _mainFeedCollectiblesFarLiveData = MutableLiveData<ArrayList<Collectible>?>()
    val mainFeedCollectiblesFarLiveData: LiveData<ArrayList<Collectible>?>
        get() = _mainFeedCollectiblesFarLiveData

    val showDistanceLabel = MutableLiveData<Pair<Boolean, Boolean>>()

    fun getMainFeedCollectibles(userInfo: UserInfo,
                                isUsingSearch: Boolean,
                                searchValue: String?,
                                tagsList: ArrayList<String>?,
                                conditionsList: ArrayList<String>?
                                ){
        viewModelScope.launch {
            val result =  repository.getMainFeed(userInfo, isUsingSearch, searchValue, tagsList, conditionsList)!!
            showDistanceLabel.value = Pair(result.first.isNotEmpty(), result.second.isNotEmpty())
            _mainFeedCollectiblesNearLiveData.value = result.first
            _mainFeedCollectiblesFarLiveData.value = result.second
        }
    }

    private val _isTextMessageSentLiveData = MutableLiveData<Boolean>()
    val isTextMessageSentLiveData: LiveData<Boolean>
        get() = _isTextMessageSentLiveData

    fun sendTextMessage(
        thisUserInfo: UserInfo,
        otherUserInfo: UserInfo,
        textMessage: TextMessage
    ) {
        viewModelScope.launch {
            _isTextMessageSentLiveData.value =
                repository.sendTextMessageToUser(thisUserInfo, otherUserInfo, textMessage)
        }
    }

    private val _isTradeMessageSentLiveData = MutableLiveData<Boolean>()
    val isTradeMessageSentLiveData: LiveData<Boolean>
        get() = _isTradeMessageSentLiveData

    fun sendTradeMessage(thisUserInfo: UserInfo, otherUserInfo: UserInfo, tradeMessage: TradeMessage) {
        viewModelScope.launch {
            _isTradeMessageSentLiveData.value =
                repository.sendTradeOfferMessage(thisUserInfo, otherUserInfo, tradeMessage)
        }
    }

    private val _messagesLiveData = MutableLiveData<ArrayList<Message>?>()
    val messagesLiveData: LiveData<ArrayList<Message>?>
        get() = _messagesLiveData

    fun listenForMessagesFromOtherUser(userId: String, otherUserId: String) {
        viewModelScope.launch {
            repository.listenForTextMessagesFromOtherUser(userId, otherUserId).collect {
                _messagesLiveData.value = it
            }
        }
    }

    private val _conversationsLiveData = MutableLiveData<QuerySnapshot?>()
    val conversationsLiveData: LiveData<QuerySnapshot?>
        get() = _conversationsLiveData

    fun getConversationsForUser(userId: String) {
        viewModelScope.launch {
            _conversationsLiveData.value = repository.getConversationsForUser(userId)
        }
    }

    private val _isTradeStatusUpdatedLiveData = MutableLiveData<TradeMessage?>()
    val isTradeStatusUpdatedLiveData: LiveData<TradeMessage?>
        get() = _isTradeStatusUpdatedLiveData

    fun updateTradeStatus(userInfo: UserInfo, otherUserInfo: UserInfo, tradeMessage: TradeMessage) {
        viewModelScope.launch {
            _isTradeStatusUpdatedLiveData.value =
                repository.updateTradeStatus(userInfo, otherUserInfo, tradeMessage)
        }
    }



    private val _isUserInfoUpdatedLiveData = MutableLiveData<UserInfo?>()
    val isUserInfoUpdatedLiveData: LiveData<UserInfo?>
        get() = _isUserInfoUpdatedLiveData

    fun updateProfile(userInfo: UserInfo, filePath: Uri?) {
        viewModelScope.launch {
            _isUserInfoUpdatedLiveData.value = repository.updateProfile(userInfo, filePath)
        }
    }

    private val _userInfoLiveData = MutableLiveData<UserInfo?>()
    val userInfoLiveData: LiveData<UserInfo?>
        get() = _userInfoLiveData

    fun getUserInfo(uid: String) {
        viewModelScope.launch {
            _userInfoLiveData.value = repository.getUserInfo(uid)
        }
    }

    private val _isCollectibleDeletedLiveData = MutableLiveData<Collectible?>()
    val isCollectibleDeletedLiveData: LiveData<Collectible?>
        get() = _isCollectibleDeletedLiveData

    fun deleteCollectible(userInfo: UserInfo, collectible: Collectible) {
        viewModelScope.launch {
            _isCollectibleDeletedLiveData.value =
                repository.deleteCollectible(userInfo, collectible)
        }
    }

    private val _checkForAcceptedTradesLiveData = MutableLiveData<TradeMessage?>()
    val checkForAcceptedTradeLiveData: LiveData<TradeMessage?>
        get() = _checkForAcceptedTradesLiveData

    fun checkForAcceptedTrades(userId: String){
        viewModelScope.launch {
            _checkForAcceptedTradesLiveData.value = repository.checkForAcceptedTrades(userId)
        }
    }

    private val _isTradeAcceptanceReceivedUpdatedLiveData = MutableLiveData<UserInfo?>()
    val isTradeStatusAcceptanceReceivedUpdatedLiveData: LiveData<UserInfo?>
        get() = _isTradeAcceptanceReceivedUpdatedLiveData

    fun updateTradeAcceptanceReceived(
        userId: String,
        otherUserId: String,
        tradeMessage: TradeMessage,
        deleteCollectibles: Boolean
    ) {
        viewModelScope.launch {
            _isTradeAcceptanceReceivedUpdatedLiveData.value =
                repository.updateTradeAcceptanceReceived(userId, otherUserId, tradeMessage, deleteCollectibles)
        }
    }

    private val _isUserRatedSuccessfully = MutableLiveData<UserInfo?>()
    val isUserRatedSuccessfully: LiveData<UserInfo?>
        get() = _isUserRatedSuccessfully

    fun rateUser(otherUserInfo: UserInfo){
        viewModelScope.launch {
            _isUserRatedSuccessfully.value = repository.updateProfile(otherUserInfo, null)
        }
    }

    private val _isImageMessageSentLiveData = MutableLiveData<Boolean>()
    val isImageMessageSentLiveData: LiveData<Boolean>
        get() = _isImageMessageSentLiveData

    fun sendImageMessage(
        thisUserInfo: UserInfo,
        otherUserInfo: UserInfo,
        imageMessage: ImageMessage,
        filePath: Uri
    ) {
        viewModelScope.launch {
            _isImageMessageSentLiveData.value =
                repository.sendImageMessageToUser(thisUserInfo, otherUserInfo, imageMessage, filePath)
        }
    }

    private val _isCollectibleUpdatedLiveData = MutableLiveData<Boolean>()
    val isCollectibleUpdatedLiveData: LiveData<Boolean>
        get() = _isCollectibleUpdatedLiveData


    fun updateCollectibleViews(userInfo: UserInfo, collectible: Collectible) {
        viewModelScope.launch {
            _isCollectibleUpdatedLiveData.value = repository.updateCollectibleViews(userInfo, collectible)
        }

    }
}