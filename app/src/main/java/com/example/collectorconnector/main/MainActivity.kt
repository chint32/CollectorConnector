package com.example.collectorconnector.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.collectorconnector.R
import com.example.collectorconnector.adapters.CollectibleAdapter
import com.example.collectorconnector.auth.LoginActivity
import com.example.collectorconnector.databinding.ActivityMainBinding
import com.example.collectorconnector.edit_profile.EditProfileActivity
import com.example.collectorconnector.models.Collectible
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.util.Constants.ONE_HUNDRED_MEGABYTE
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity(), CollectibleAdapter.OnItemClickListener {

    lateinit var binding: ActivityMainBinding
    lateinit var navController: NavController
    val viewModel: MainViewModel by viewModels()
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUser = mAuth.currentUser
    lateinit var userInfo: UserInfo
    lateinit var tagsArray: Array<String?>
    var cityStatesList = ArrayList<Pair<String, String>>()
    var statesList = ArrayList<String>()
    private var animDirectionFlag = false
    val mainFeedCollectibles = ArrayList<Collectible>()
    val feedAdapter = CollectibleAdapter(mainFeedCollectibles, this, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bottomNavigationView.setBackgroundColor(
            ContextCompat.getColor(
                this,
                android.R.color.transparent
            )
        )
        binding.bottomNavigationView.itemIconTintList = null

        userInfo = intent.extras!!.get("user_info") as UserInfo
        tagsArray = intent.extras!!.get("categories") as Array<String?>
        statesList = intent.extras!!.get("states") as ArrayList<String>
        cityStatesList = intent.extras!!.get("cities_states") as ArrayList<Pair<String, String>>

        println(statesList)


        navController = Navigation.findNavController(this, R.id.main_nav_host_fragment)
        NavigationUI.setupWithNavController(binding.bottomNavigationView, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->

            if (!animDirectionFlag) {
                binding.blueAcct1.animate().apply {
                    duration = 1000
                    translationX(-400F)
                }.start()
                binding.blueAcct2.animate().apply {
                    duration = 1000
                    translationX(400F)
                }.start()
                binding.blueAcct3.animate().apply {
                    duration = 1000
                    translationX(-200F)
                }.start()

            } else {
                binding.blueAcct1.animate().apply {
                    duration = 1000
                    translationX(0f)
                }.start()
                binding.blueAcct2.animate().apply {
                    duration = 1000
                    translationX(0F)
                }.start()
                binding.blueAcct3.animate().apply {
                    duration = 1000
                    translationX(0F)
                }.start()
            }
            animDirectionFlag = !animDirectionFlag

        }

        // main feed collectibles - observer 1
        viewModel.usersWithCollectiblesLiveData.observe(this, Observer {
            if(it == null){
                Toast.makeText(this, "Error getting other users", Toast.LENGTH_SHORT).show()
                return@Observer
            }
            for (item in it.prefixes) {
                if (item.name != currentUser!!.uid) {
                    //get collectible
                    viewModel.getMainFeedCollectiblesByUid(item.name)

                }
            }
        })

        // main feed collectibles - observer 2
        viewModel.mainFeedCollectiblesLiveData.observe(this, Observer {
            if (it == null) {
                Toast.makeText(this, "Error getting collectible for home feed", Toast.LENGTH_SHORT).show()
                return@Observer
            }

            // cycle through collectibles for all users
            for (collectibleData in it.items) {
                // make sure current users own collectibles are not returned in the search
                if (collectibleData.name != currentUser!!.uid) {
                    collectibleData.metadata.addOnSuccessListener { metadata ->

                        // if not same location and tags, skip
                        if (metadata.getCustomMetadata("state") == userInfo.state &&
                            metadata.getCustomMetadata("city") == userInfo.city &&
                            userInfo.tags.toString()
                                .contains(metadata.getCustomMetadata("tags")!!)
                        ) {
                            collectibleData.getBytes(ONE_HUNDRED_MEGABYTE)
                                .addOnSuccessListener { byteArray ->

                                    val tags = ArrayList<String>()
                                    val tagsArr = metadata.getCustomMetadata("tags")
                                        .toString()
                                        .split(",")
                                    for (i in tagsArr.indices)
                                        tags.add(tagsArr[i])
                                    // construct model
                                    val collectible = Collectible(
                                        collectibleData.name,
                                        metadata.getCustomMetadata("name").toString(),
                                        metadata.getCustomMetadata("desc").toString(),
                                        metadata.getCustomMetadata("cond").toString(),
                                        byteArray,
                                        tags,
                                        metadata.getCustomMetadata("ownerId").toString()
                                    )
                                    if (!mainFeedCollectibles.contains(collectible)) {

                                        //add to recycler list
                                        mainFeedCollectibles.add(collectible)
                                        feedAdapter.notifyItemInserted(
                                            mainFeedCollectibles.indexOf(
                                                collectible
                                            )
                                        )
                                    }

                                }.addOnFailureListener {
                                    Toast.makeText(
                                        this,
                                        "Error: " + it.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }
            }
            findViewById<ProgressBar>(R.id.myProgressBar).visibility = View.GONE
        })

        viewModel.getAllUsersWithCollectibles()
    }



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        //TODO - fix
        if (currentUser == null) startActivity(Intent(this, LoginActivity::class.java))


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.logout -> {
                mAuth.signOut()
                startLoginActivity()
                true
            }
            R.id.edit_profile -> {
                //navigate to edit profile screen

                startEditProfileActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startLoginActivity(){


        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("states", statesList)
        intent.putExtra("cities_states", cityStatesList)
        intent.putExtra("categories", tagsArray)
        startActivity(intent)
    }

    private fun startEditProfileActivity(){


        val intent = Intent(this, EditProfileActivity::class.java)
        intent.putExtra("user_info", userInfo)
        intent.putExtra("states", statesList)
        intent.putExtra("cities_states", cityStatesList)
        intent.putExtra("categories",tagsArray)
        startActivity(intent)
    }

    fun getVisibleFragment(): Fragment? {
        val fragmentManager: FragmentManager = this@MainActivity.supportFragmentManager
        val fragments: List<Fragment> = fragmentManager.fragments
        for (fragment in fragments) {
            if (fragment.isVisible) return fragment
        }
        return null
    }

    override fun onItemClick(position: Int) {

        val currFragment = getVisibleFragment()
        println(currFragment!!.findNavController().currentDestination!!.id)

        if (currFragment.findNavController().currentDestination!!.id == R.id.homeFragment) {
            val collectible = mainFeedCollectibles[position]
            navController.navigate(
                HomeFragmentDirections.actionHomeFragmentToCollectibleDetailsFragment(collectible)
            )
        }
    }
}