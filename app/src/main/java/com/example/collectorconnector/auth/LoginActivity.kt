package com.example.collectorconnector.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.collectorconnector.databinding.ActivityLoginBinding


class LoginActivity : AppCompatActivity() {

    lateinit var tagsArray: Array<String?>
    lateinit var conditions: Array<String?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tagsArray = intent.extras!!.get("categories") as Array<String?>
        conditions = intent.extras!!.get("conditions") as Array<String?>

        supportActionBar!!.hide()
    }
}