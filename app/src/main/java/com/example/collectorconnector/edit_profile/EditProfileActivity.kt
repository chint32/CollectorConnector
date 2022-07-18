package com.example.collectorconnector.edit_profile

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.collectorconnector.databinding.ActivityEditProfileBinding
import com.example.collectorconnector.databinding.ActivityMainBinding
import com.example.collectorconnector.models.Collectible
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.util.Constants.ONE_HUNDRED_MEGABYTE
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUser = mAuth.currentUser
    lateinit var userInfo: UserInfo
    val viewModel: EditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(Color.parseColor("#FF007F")))

        //get user info passed from MainActivity
        userInfo = intent.extras!!.get("user_info") as UserInfo

        //binding.progressBar.visibility = View.VISIBLE
    }
}