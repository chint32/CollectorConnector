package com.example.collectorconnector.auth

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.repository.FirebaseRepository
import com.google.firebase.auth.AuthResult
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.launch


class AuthViewModel : ViewModel() {
    private val repository: FirebaseRepository = FirebaseRepository

    private val _statesCitiesLiveData = MutableLiveData<DocumentSnapshot?>()
    val statesCitiesLiveData: LiveData<DocumentSnapshot?>
        get() = _statesCitiesLiveData

    fun getCitiesAndStates(){
        viewModelScope.launch {
            _statesCitiesLiveData.value = repository.getStatesAndCities()
        }
    }

    private val _colelctibleCategoriesLiveData = MutableLiveData<DocumentSnapshot?>()
    val collectibleCategoriesLiveData: LiveData<DocumentSnapshot?>
        get() = _colelctibleCategoriesLiveData

    fun getCollectibleCategories(){
        viewModelScope.launch {
            _colelctibleCategoriesLiveData.value = repository.getCollectibleCategories()
        }
    }

    private val _authenticatedUserLiveData = MutableLiveData<AuthResult>()
    val authenticatedUserLiveData: LiveData<AuthResult>
        get() = _authenticatedUserLiveData

    fun signInWithEmailAndPw(email: String, pw: String) {
        viewModelScope.launch {
            _authenticatedUserLiveData.value = repository.signInWithEmailAndPw(email, pw)
        }
    }

    fun registerWithEmailAndPw(email: String, pw: String) {
        viewModelScope.launch {
            _authenticatedUserLiveData.value = repository.registerWithEmailAndPw(email, pw)

        }
    }

    private val _isUserInfoUpdatedLiveData = MutableLiveData<Boolean>()
    val isUserInfoUpdatedLiveData: LiveData<Boolean>
        get() = _isUserInfoUpdatedLiveData

    fun updateUserInfo(userInfo: UserInfo){
        viewModelScope.launch {
            _isUserInfoUpdatedLiveData.value = repository.updateUserInfo(userInfo)
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

    private val _isProfileImgUploadedLiveData = MutableLiveData<String?>()
    val isProfileImgUploadedLiveData: LiveData<String?>
        get() = _isProfileImgUploadedLiveData


    fun uploadProfileImg(userId: String, filePath: Uri){
        viewModelScope.launch {
            _isProfileImgUploadedLiveData.value = repository.addProfileImg(userId, filePath).toString()
        }
    }
}