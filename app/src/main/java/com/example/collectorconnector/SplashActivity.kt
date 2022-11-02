package com.example.collectorconnector

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.collectorconnector.auth.AuthViewModel
import com.example.collectorconnector.auth.LoginActivity
import com.example.collectorconnector.databinding.ActivitySplashBinding


class SplashActivity: AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val viewModel: AuthViewModel by viewModels()
    private var isAnimOver = false
    private var isStartingDataDownloaded = false
    private lateinit var tagsArray: Array<String?>
    private lateinit var conditions: Array<String?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.hide()

        viewModel.appStartingDataLiveData.observe(this){
            if(it == null)  return@observe
            tagsArray = arrayOfNulls<String>(it.first.size)
            for(i in it.first.indices)
                tagsArray[i] = it.first[i]
            conditions = arrayOfNulls<String>(it.second.size)
            for(i in it.second.indices)
                conditions[i] = it.second[i]

            isStartingDataDownloaded = true
            if(isAnimOver) startLoginActivity()
        }
        viewModel.getAppStartingData()



        val splashAnim = findViewById<LottieAnimationView>(R.id.splash_anim)
        splashAnim.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
                Log.e("Animation:", "start")
            }

            override fun onAnimationEnd(animation: Animator?) {
                Log.e("Animation:", "end")
                isAnimOver = true
                if(isStartingDataDownloaded) startLoginActivity()
            }

            override fun onAnimationCancel(animation: Animator?) {
                Log.e("Animation:", "cancel")
            }

            override fun onAnimationRepeat(animation: Animator?) {
                Log.e("Animation:", "repeat")
            }
        })

    }

    private fun startLoginActivity(){


        val intent = Intent(this@SplashActivity, LoginActivity::class.java)
        intent.putExtra("categories", tagsArray)
        intent.putExtra("conditions", conditions)
        startActivity(intent)
    }
}