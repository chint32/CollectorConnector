package com.example.collectorconnector.auth

import android.app.Application
import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.*
import com.example.collectorconnector.R
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.repository.FirebaseRepository
import com.google.firebase.auth.AuthResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.auth.User
import kotlinx.coroutines.launch


class AuthViewModel(application: Application) : AndroidViewModel(application) {
    val app = application
    private val repository: FirebaseRepository = FirebaseRepository
    var showProgressBar = MutableLiveData<Boolean>()

    var email = ""
    var pw = ""

    var userInfo = UserInfo()
    var filePath = Uri.EMPTY

    private val _appStartingDataLiveData = MutableLiveData<Pair<ArrayList<String>, ArrayList<String>>?>()
    val appStartingDataLiveData: LiveData<Pair<ArrayList<String>, ArrayList<String>>?>
        get() = _appStartingDataLiveData

    fun getAppStartingData(){
        viewModelScope.launch {
            _appStartingDataLiveData.value = repository.getAppStartingData()
        }
    }

    private val _authenticatedUserLiveData = MutableLiveData<AuthResult>()
    val authenticatedUserLiveData: LiveData<AuthResult>
        get() = _authenticatedUserLiveData

    fun signInWithEmailAndPw(email: String, pw: String) {
        if(email.length < 6 || email.length > 30) {
            Toast.makeText(app, app.getString(R.string.email_req_toast), Toast.LENGTH_SHORT).show()
            return
        }
        if(pw.length < 4 || pw.length > 20) {
            Toast.makeText(app, app.getString(R.string.pw_req_toast), Toast.LENGTH_SHORT).show()
            return
        }
        showProgressBar.value = true

        viewModelScope.launch {
            _authenticatedUserLiveData.value = repository.signInWithEmailAndPw(email, pw)
        }
    }

    fun registerWithEmailAndPw(email: String, pw: String) {

        if(userInfo.screenName.isEmpty()) {
            Toast.makeText(
                app,
                app.getString(R.string.empty_screen_name_toast),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if(filePath == Uri.EMPTY) {
            Toast.makeText(
                app,
                app.getString(R.string.empty_profile_image_toast),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if(userInfo.interests.isNullOrEmpty()){
            Toast.makeText(
                app,
                app.getString(R.string.empty_interests_toast),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if(!userInfo.isLocationSet){
            Toast.makeText(
                app,
                app.getString(R.string.location_not_set_toast),
                Toast.LENGTH_LONG
            ).show()
        }
        else {
            if (userInfo.searchDistance == 0) {
                Toast.makeText(
                    app,
                    app.getString(R.string.empty_search_distance_toast),
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }
        showProgressBar.value = true

        viewModelScope.launch {
            _authenticatedUserLiveData.value = repository.registerWithEmailAndPw(email, pw)
        }
    }

    private val _isUserInfoUpdatedLiveData = MutableLiveData<UserInfo?>()
    val isUserInfoUpdatedLiveData: LiveData<UserInfo?>
        get() = _isUserInfoUpdatedLiveData

    fun updateProfile(userInfo: UserInfo, filePath: Uri?){
        viewModelScope.launch {
            _isUserInfoUpdatedLiveData.value = repository.updateProfile(userInfo, filePath)
        }
    }

    private val _userInfoLiveData = MutableLiveData<UserInfo>()
    val userInfoLiveData: LiveData<UserInfo>
        get() = _userInfoLiveData

    fun getUserInfo(uid: String){
        viewModelScope.launch {
            _userInfoLiveData.value = repository.getUserInfo(uid)
        }
    }
}