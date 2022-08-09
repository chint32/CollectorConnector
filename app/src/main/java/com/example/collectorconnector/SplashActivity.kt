package com.example.collectorconnector

import android.animation.Animator
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.collectorconnector.auth.AuthViewModel
import com.example.collectorconnector.auth.LoginActivity
import com.example.collectorconnector.databinding.ActivitySplashBinding


class SplashActivity: AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var tagsArray: Array<String?>
    private var isAnimOver = false
    private var isTagsDownloaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.hide()

        viewModel.collectibleCategoriesLiveData.observe(this){
            if(it == null)  return@observe
            if(it.data.isNullOrEmpty()) return@observe
            val tagsArrayList = (it.get("categories") as ArrayList<String>)
            tagsArray = arrayOfNulls<String>(tagsArrayList.size)
            for(i in tagsArrayList.indices)
                tagsArray[i] = tagsArrayList[i]
            isTagsDownloaded = true
            if(isAnimOver) startLoginActivity()
        }
        viewModel.getCollectibleCategories()

        val splashAnim = findViewById<LottieAnimationView>(R.id.splash_anim)
        splashAnim.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
                Log.e("Animation:", "start")
            }

            override fun onAnimationEnd(animation: Animator?) {
                Log.e("Animation:", "end")
                isAnimOver = true
                if(isTagsDownloaded) startLoginActivity()
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
        startActivity(intent)
    }
}