package com.example.collectorconnector

import android.animation.Animator
import android.app.PendingIntent.getActivity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
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
    private val cityStatesList = ArrayList<Pair<String, String>>()
    private val statesList = ArrayList<String>()
    private var isAnimOver = false
    private var isTagsDownloaded = false
    private var isLocationsDownloaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.collectibleCategoriesLiveData.observe(this){
            if(it == null)  return@observe
            if(it.data.isNullOrEmpty()) return@observe
            val tagsArrayList = (it.get("categories") as ArrayList<String>)
            tagsArray = arrayOfNulls<String>(tagsArrayList.size)
            for(i in tagsArrayList.indices)
                tagsArray[i] = tagsArrayList[i]
            isTagsDownloaded = true
            if(isLocationsDownloaded && isAnimOver) startLoginActivity()
        }
        viewModel.getCollectibleCategories()

        viewModel.statesCitiesLiveData.observe(this){
            if(!it!!.exists()) return@observe

            for (entry in it.data!!.entries) {
                statesList.add(entry.key)
                val pairList = entry.toPair() as Pair<String, ArrayList<String>>
                for(city in pairList.second) {
                    val pair = Pair(pairList.first, city)
                    cityStatesList.add(pair)
                }
            }
            statesList.sort()
            cityStatesList.sortWith(compareBy { pair ->
                pair.first
            })
            isLocationsDownloaded = true
            if(isTagsDownloaded && isAnimOver) startLoginActivity()
        }
        viewModel.getCitiesAndStates()

        val splashAnim = findViewById<LottieAnimationView>(R.id.splash_anim)
        splashAnim.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
                Log.e("Animation:", "start")
            }

            override fun onAnimationEnd(animation: Animator?) {
                Log.e("Animation:", "end")
                isAnimOver = true
                if(isTagsDownloaded && isLocationsDownloaded) startLoginActivity()
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
        intent.putExtra("states", statesList)
        intent.putExtra("cities_states", cityStatesList)
        intent.putExtra("categories", tagsArray)
        startActivity(intent)
    }
}