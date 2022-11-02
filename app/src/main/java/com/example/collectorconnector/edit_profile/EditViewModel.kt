package com.example.collectorconnector.edit_profile

import android.app.Application
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.lifecycle.*
import com.example.collectorconnector.R
import com.example.collectorconnector.models.Collectible
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageMetadata

import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class EditViewModel(application: Application) : AndroidViewModel(application) {

    val app = application
    private val repository: FirebaseRepository = FirebaseRepository

    var showProgressBar = MutableLiveData<Boolean>()
    val collectible = Collectible()
    var userInfo = UserInfo()


    private val _isUserInfoUpdatedLiveData = MutableLiveData<UserInfo?>()
    val isUserInfoUpdatedLiveData: LiveData<UserInfo?>
        get() = _isUserInfoUpdatedLiveData

    fun updateProfile(userInfo: UserInfo, filepath: Uri?){
        viewModelScope.launch {


            if (userInfo.screenName.isEmpty()) {
                Toast.makeText(app, app.getString(R.string.empty_screen_name_toast), Toast.LENGTH_SHORT)
                    .show()
                return@launch
            } else if (userInfo.interests.isEmpty()) {
                Toast.makeText(app, app.getString(R.string.empty_interests_toast), Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }
            showProgressBar.value = true
            _isUserInfoUpdatedLiveData.value = repository.updateProfile(userInfo, filepath)
        }
    }

    private val _isCollectibleDeletedLiveData = MutableLiveData<Collectible>()
    val isCollectibleDeletedLiveData: LiveData<Collectible>
        get() = _isCollectibleDeletedLiveData

    fun deleteCollectible(userInfo: UserInfo, collectible:Collectible){
        viewModelScope.launch {
            _isCollectibleDeletedLiveData.value = repository.deleteCollectible(userInfo, collectible)
        }
    }

    private val _isCollectibleAddedLiveData = MutableLiveData<Collectible?>()
    val isCollectibleAddedLiveData: LiveData<Collectible?>
        get() = _isCollectibleAddedLiveData

    fun addCollectible(name: String, description: String, condition: String, filepath: Uri?){
        viewModelScope.launch {

            if (filepath == null) {
                Toast.makeText(app, app.getString(R.string.collectible_must_have_image), Toast.LENGTH_SHORT).show()
                return@launch
            } else if (name.length < 4  || name.length > 40) {
                Toast.makeText(app, app.getString(R.string.screen_name_req_toast), Toast.LENGTH_SHORT).show()
                return@launch
            } else if (description.length < 4 || description.length > 300) {
                Toast.makeText(app, app.getString(R.string.description_req_toast), Toast.LENGTH_SHORT)
                    .show()
                return@launch
            } else if (condition.isEmpty()) {
                Toast.makeText(app, app.getString(R.string.collectible_must_have_condition), Toast.LENGTH_SHORT)
                    .show()
                return@launch
            } else if (collectible.tags.isEmpty()) {
                Toast.makeText(app, app.getString(R.string.collectible_must_have_tag), Toast.LENGTH_SHORT).show()
                return@launch
            }

            showProgressBar.value = true

            collectible.uid = UUID.randomUUID().toString()
            collectible.name = name
            collectible.description = description
            collectible.condition = condition
            collectible.ownerId = userInfo.uid

            _isCollectibleAddedLiveData.value = repository.addCollectible(userInfo, filepath, collectible)
        }
    }
}