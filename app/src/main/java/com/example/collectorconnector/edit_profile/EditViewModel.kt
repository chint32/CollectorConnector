package com.example.collectorconnector.edit_profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.repository.FirebaseRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageMetadata

import kotlinx.coroutines.launch

class EditViewModel: ViewModel() {
    private val repository: FirebaseRepository = FirebaseRepository

    private val _isUserInfoUpdatedLiveData = MutableLiveData<Boolean>()
    val isUserInfoUpdatedLiveData: LiveData<Boolean>
        get() = _isUserInfoUpdatedLiveData

    fun updateUserInfo(userInfo: UserInfo){
        viewModelScope.launch {
            _isUserInfoUpdatedLiveData.value = repository.updateUserInfo(userInfo)
        }
    }

    private val _isCollectibleDeletedLiveData = MutableLiveData<Boolean>()
    val isCollectibleDeletedLiveData: LiveData<Boolean>
        get() = _isCollectibleDeletedLiveData

    fun deleteCollectible(userId: String, collectibleId: String){
        viewModelScope.launch {
            _isCollectibleDeletedLiveData.value = repository.deleteCollectible(userId, collectibleId)
        }
    }

    private val _isCollectibleAddedLiveData = MutableLiveData<Boolean>()
    val isCollectibleAddedLiveData: LiveData<Boolean>
        get() = _isCollectibleAddedLiveData

    fun addCollectible(userId: String, collectibleId: String, filePath: Uri, metadata: StorageMetadata){
        viewModelScope.launch {
            _isCollectibleAddedLiveData.value = repository.addCollectible(userId, collectibleId, filePath, metadata)
        }
    }

    private val _collectiblesLiveData = MutableLiveData<ListResult?>()
    val collectiblesLiveData: LiveData<ListResult?>
        get() = _collectiblesLiveData

    fun getCollectiblesByUid(uid: String){
        viewModelScope.launch {
            _collectiblesLiveData.value = repository.getCollectiblesByUserId(uid)
        }
    }
    fun clearCollectiblesLiveData(){
        if(_collectiblesLiveData.value != null){
            _collectiblesLiveData.value = null
        }
    }

    private val _isProfileImgUploadedLiveData = MutableLiveData<String?>()
    val isProfileImgUploadedLiveData: LiveData<String?>
        get() = _isProfileImgUploadedLiveData

    fun uploadProfileImg(userId: String, filePath: Uri){
        viewModelScope.launch {
            _isProfileImgUploadedLiveData.value = repository.addProfileImg(userId, filePath).toString()
        }
    }


    private val _userInfoLiveData = MutableLiveData<DocumentSnapshot>()
    val userInfoLiveData: LiveData<DocumentSnapshot>
        get() = _userInfoLiveData

    fun getUserInfo(uid: String){
        viewModelScope.launch {
            _userInfoLiveData.value = repository.getUserInfo(uid)
        }
    }
}