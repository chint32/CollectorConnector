package com.example.collectorconnector.auth

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.collectorconnector.databinding.ActivityLoginBinding
import com.example.collectorconnector.main.MainActivity
import com.example.collectorconnector.models.UserInfo
import com.google.firebase.auth.FirebaseAuth


class LoginActivity : AppCompatActivity() {

    val viewModel: AuthViewModel by viewModels()
    val currUser = FirebaseAuth.getInstance().currentUser
    private lateinit var userInfo: UserInfo
    lateinit var tagsArray: Array<String?>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tagsArray = intent.extras!!.get("categories") as Array<String?>

        supportActionBar!!.hide()
        viewModel.userInfoLiveData.observe(this, Observer {
            if(!it.exists()) {
                Toast.makeText(this, "Error loggin in", Toast.LENGTH_SHORT).show()
                return@Observer
            }

            userInfo = it.toObject(UserInfo::class.java)!!
            Toast.makeText(this, "${userInfo.screenName} logged in", Toast.LENGTH_SHORT).show()
            startMainActivity(userInfo)
        })
    }

    override fun onStart() {
        super.onStart()
        if(currUser != null)
            viewModel.getUserInfo(currUser.uid)
    }

    private fun startMainActivity( userInfo: UserInfo){


        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user_info", userInfo)
        intent.putExtra("categories",tagsArray)
        startActivity(intent)
    }
}