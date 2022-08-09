package com.example.collectorconnector.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.collectorconnector.R
import com.example.collectorconnector.adapters.CollectibleAdapter
import com.example.collectorconnector.adapters.ConversationsAdapter
import com.example.collectorconnector.auth.LoginActivity
import com.example.collectorconnector.databinding.ActivityMainBinding
import com.example.collectorconnector.edit_profile.EditProfileActivity
import com.example.collectorconnector.models.Conversation
import com.example.collectorconnector.models.UserInfo
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity(), CollectibleAdapter.OnItemClickListener,
    ConversationsAdapter.OnItemClickListener {

    lateinit var binding: ActivityMainBinding
    lateinit var navController: NavController
    val viewModel: MainViewModel by viewModels()
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUser = mAuth.currentUser
    lateinit var userInfo: UserInfo
    lateinit var tagsArray: Array<String?>
    val conversations = ArrayList<Conversation>()
    var conversationsAdapter = ConversationsAdapter(conversations, this)

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
        supportActionBar!!.hide()
        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
            if (item.itemId == R.id.logout) {
                mAuth.signOut()
                startLoginActivity()
            } else if (item.itemId == R.id.edit_profile) {
                startEditProfileActivity()
            }
            false
        })

        userInfo = intent.extras!!.get("user_info") as UserInfo
        tagsArray = intent.extras!!.get("categories") as Array<String?>

        navController = Navigation.findNavController(this, R.id.main_nav_host_fragment)
        NavigationUI.setupWithNavController(binding.bottomNavigationView, navController)

        viewModel.conversationsLiveData.observe(this) {
            if (it == null) {
                Toast.makeText(this, "Error getting conversations", Toast.LENGTH_SHORT)
                    .show()
                return@observe
            }

            for (doc in it.documents) {

                val conversation = Conversation(
                    doc.id,
                    doc.get("otherUserScreenName").toString(),
                    doc.get("otherUserProfileImgUrl").toString(),
                    doc.get("lastMessage").toString(),
                    doc.get("time").toString()
                )

                conversations.add(conversation)
            }
            conversations.sortWith(compareBy { it.time })
            conversationsAdapter = ConversationsAdapter(conversations, this)

        }
        viewModel.getConversationsForUser(currentUser!!.uid)
    }




    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        //TODO - fix
        if (currentUser == null) startActivity(Intent(this, LoginActivity::class.java))


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("categories", tagsArray)
        startActivity(intent)
    }

    private fun startEditProfileActivity() {

        val intent = Intent(this, EditProfileActivity::class.java)
        intent.putExtra("user_info", userInfo)
        intent.putExtra("categories", tagsArray)
        startActivity(intent)
    }

    fun getVisibleFragment(): Fragment? {
        val fragments: List<Fragment> = supportFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment.isVisible)
                return fragment
        }
        return null
    }

    override fun onItemClick(position: Int) {

        val currFragment = getVisibleFragment()

        if (currFragment!!.findNavController().currentDestination!!.id == R.id.conversationsFragment) {
            val conversation = conversations[position]
            navController.navigate(
                ConversationsFragmentDirections.actionConversationsFragmentToMessageFragment(
                    conversation.otherUserId
                )
            )
        }
    }
}