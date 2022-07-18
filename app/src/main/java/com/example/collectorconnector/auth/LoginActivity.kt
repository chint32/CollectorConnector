package com.example.collectorconnector.auth

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.collectorconnector.databinding.ActivityLoginBinding
import com.example.collectorconnector.main.MainActivity
import com.example.collectorconnector.models.UserInfo
import com.google.firebase.auth.FirebaseAuth


class LoginActivity : AppCompatActivity() {

    val viewModel: AuthViewModel by viewModels()
    val currUser = FirebaseAuth.getInstance().currentUser
    private lateinit var userInfo: UserInfo
    lateinit var tagsArray: Array<String?>
    var cityStatesList = ArrayList<Pair<String, String>>()
    var statesList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tagsArray = intent.extras!!.get("categories") as Array<String?>
        statesList = intent.extras!!.get("states") as ArrayList<String>
        cityStatesList = intent.extras!!.get("cities_states") as ArrayList<Pair<String, String>>

        supportActionBar!!.setBackgroundDrawable(ColorDrawable(Color.parseColor("#FF007F")))
        viewModel.userInfoLiveData.observe(this, Observer {
            if(!it.exists()) return@Observer

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
        intent.putExtra("states", statesList)
        intent.putExtra("cities_states", cityStatesList)
        intent.putExtra("categories",tagsArray)
        startActivity(intent)
    }
}