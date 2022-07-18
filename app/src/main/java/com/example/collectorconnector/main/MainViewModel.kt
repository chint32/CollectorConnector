package com.example.collectorconnector.main

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.collectorconnector.models.ImageMessage
import com.example.collectorconnector.models.TextMessage
import com.example.collectorconnector.models.TradeMessage
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.repository.FirebaseRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.ListResult
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository: FirebaseRepository = FirebaseRepository

    private val _thisUsersCollectiblesLiveData = MutableLiveData<ListResult?>()
    val thisUsersCollectiblesLiveData: LiveData<ListResult?>
        get() = _thisUsersCollectiblesLiveData


    fun getThisUsersCollectibles(userId: String) {
        viewModelScope.launch {
            _thisUsersCollectiblesLiveData.value = repository.getCollectiblesByUserId(userId)
        }
    }

    private val _otherUsersCollectiblesLiveData = MutableLiveData<ListResult?>()
    val otherUsersCollectiblesLiveData: LiveData<ListResult?>
        get() = _otherUsersCollectiblesLiveData

    fun getOtherUsersCollectibles(userId: String) {
        viewModelScope.launch {
            _otherUsersCollectiblesLiveData.value = repository.getCollectiblesByUserId(userId)
        }
    }

    private val _usersWithCollectiblesLiveData = MutableLiveData<ListResult?>()
    val usersWithCollectiblesLiveData: LiveData<ListResult?>
        get() = _usersWithCollectiblesLiveData

    fun getAllUsersWithCollectibles() {
        viewModelScope.launch {
            _usersWithCollectiblesLiveData.value = repository.getAllUsersWithCollectibles()
        }
    }

    private var _searchFeedCollectiblesLiveData = MutableLiveData<ListResult?>()
    val searchFeedCollectiblesLiveData: LiveData<ListResult?>
        get() = _searchFeedCollectiblesLiveData

    fun getSearchFeedCollectiblesByUid(uid: String) {
        viewModelScope.launch {
            _searchFeedCollectiblesLiveData.value = repository.getCollectiblesByUserId(uid)
        }
    }

    private val _mainFeedCollectiblesLiveData = MutableLiveData<ListResult?>()
    val mainFeedCollectiblesLiveData: LiveData<ListResult?>
        get() = _mainFeedCollectiblesLiveData

    fun getMainFeedCollectiblesByUid(uid: String) {
        viewModelScope.launch {
            _mainFeedCollectiblesLiveData.value = repository.getCollectiblesByUserId(uid)
        }
    }

    private val _isTextMessageSentLiveData = MutableLiveData<Boolean>()
    val isTextMessageSentLiveData: LiveData<Boolean>
        get() = _isTextMessageSentLiveData

    fun sendTextMessage(userId: String, otherUserId: String, textMessage: TextMessage) {
        viewModelScope.launch {
            _isTextMessageSentLiveData.value =
                repository.sendTextMessageToUser(userId, otherUserId, textMessage)
        }
    }

    private val _isTradeMessageSentLiveData = MutableLiveData<Boolean>()
    val isTradeMessageSentLiveData: LiveData<Boolean>
        get() = _isTradeMessageSentLiveData

    fun sendTradeMessage(userId: String, otherUserId: String, tradeMessage: TradeMessage) {
        viewModelScope.launch {
            _isTradeMessageSentLiveData.value =
                repository.sendTradeOfferMessage(userId, otherUserId, tradeMessage)
        }
    }

    private val _tradeImagesSenderLiveData = MutableLiveData<ListResult?>()
    val tradeImagesSenderLiveData: LiveData<ListResult?>
        get() = _tradeImagesSenderLiveData

    fun getImagesForTradeSender(userId: String, otherUserId: String, tradeId: String) {
        viewModelScope.launch {
            _tradeImagesSenderLiveData.value =
                repository.getImagesForTradeSender(userId, otherUserId, tradeId)
        }
    }

    private val _tradeImagesReceiverLiveData = MutableLiveData<ListResult?>()
    val tradeImagesReceiverLiveData: LiveData<ListResult?>
        get() = _tradeImagesReceiverLiveData

    fun getImagesForTradeReceiver(userId: String, otherUserId: String, tradeId: String) {
        viewModelScope.launch {
            _tradeImagesReceiverLiveData.value =
                repository.getImagesForTradeReceiver(userId, otherUserId, tradeId)
        }
    }

    private val _messagesLiveData = MutableLiveData<MutableList<DocumentSnapshot?>?>()
    val messagesLiveData: LiveData<MutableList<DocumentSnapshot?>?>
        get() = _messagesLiveData

    fun listenForMessagesFromOtherUser(userId: String, otherUserId: String) {
        viewModelScope.launch {
            repository.listenForTextMessagesFromOtherUser(userId, otherUserId).collect {
                _messagesLiveData.value = it
            }
        }
    }

    private val _imageMessagesLiveData = MutableLiveData<ByteArray?>()
    val imageMessagesLiveData: LiveData<ByteArray?>
        get() = _imageMessagesLiveData

    fun getImageFromImageMessage(userId: String, otherUserId: String, imageMessage: ImageMessage) {
        viewModelScope.launch {
            _imageMessagesLiveData.value =
                repository.getImageFromImageMessage(userId, otherUserId, imageMessage)
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

    fun updateTradeStatus(userId: String, otherUserId: String, tradeMessage: TradeMessage) {
        viewModelScope.launch {
            _isTradeStatusUpdatedLiveData.value =
                repository.updateTradeStatus(userId, otherUserId, tradeMessage)
        }
    }

    private val _tradeMessagesForUser = MutableLiveData<QuerySnapshot?>()
    val tradeMessagesForUser: LiveData<QuerySnapshot?>
        get() = _tradeMessagesForUser

    fun getTradeMessagesForUser(userId: String) {
        viewModelScope.launch {
            _tradeMessagesForUser.value = repository.getTradeMessagesForUser(userId)
        }
    }

    private val _isUserInfoUpdatedLiveData = MutableLiveData<Boolean>()
    val isUserInfoUpdatedLiveData: LiveData<Boolean>
        get() = _isUserInfoUpdatedLiveData

    fun updateUserInfo(userInfo: UserInfo) {
        viewModelScope.launch {
            _isUserInfoUpdatedLiveData.value = repository.updateUserInfo(userInfo)
        }
    }

    private val _userInfoLiveData = MutableLiveData<DocumentSnapshot?>()
    val userInfoLiveData: LiveData<DocumentSnapshot?>
        get() = _userInfoLiveData

    fun getUserInfo(uid: String) {
        viewModelScope.launch {
            _userInfoLiveData.value = repository.getUserInfo(uid)
        }
    }

    private val _isCollectibleDeletedLiveData = MutableLiveData<Boolean>()
    val isCollectibleDeletedLiveData: LiveData<Boolean>
        get() = _isCollectibleDeletedLiveData

    fun deleteCollectible(userId: String, collectibleId: String) {
        viewModelScope.launch {
            _isCollectibleDeletedLiveData.value =
                repository.deleteCollectible(userId, collectibleId)
        }
    }

    private val _isTradeAcceptanceReceivedUpdatedLiveData = MutableLiveData<TradeMessage?>()
    val isTradeStatusAcceptanceReceivedUpdatedLiveData: LiveData<TradeMessage?>
        get() = _isTradeAcceptanceReceivedUpdatedLiveData

    fun updateTradeAcceptanceReceived(
        userId: String,
        otherUserId: String,
        tradeMessage: TradeMessage
    ) {
        viewModelScope.launch {
            _isTradeAcceptanceReceivedUpdatedLiveData.value =
                repository.updateTradeAcceptanceReceived(userId, otherUserId, tradeMessage)
        }
    }

    private val _isImageMessageSentLiveData = MutableLiveData<Boolean>()
    val isImageMessageSentLiveData: LiveData<Boolean>
        get() = _isImageMessageSentLiveData

    fun sendImageMessage(
        userId: String,
        otherUserId: String,
        imageMessage: ImageMessage,
        filePath: Uri
    ) {
        viewModelScope.launch {
            _isImageMessageSentLiveData.value =
                repository.sendImageMessageToUser(userId, otherUserId, imageMessage, filePath)
        }
    }

    // because viewmodel survives fragment, it still holds data from looking at another convo.
    // So, need to clear that out everytime this fragment is opened
    fun clearMessagesWhenSwitchingConversations() {
        if (_messagesLiveData.value != null)
            _messagesLiveData.value!!.clear()
    }
}