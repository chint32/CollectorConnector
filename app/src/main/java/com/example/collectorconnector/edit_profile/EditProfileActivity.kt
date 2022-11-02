package com.example.collectorconnector.edit_profile

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.collectorconnector.R
import com.example.collectorconnector.auth.LoginActivity
import com.example.collectorconnector.databinding.ActivityEditProfileBinding
import com.example.collectorconnector.databinding.ActivityMainBinding
import com.example.collectorconnector.main.MainActivity
import com.example.collectorconnector.models.Collectible
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.util.Constants.ONE_HUNDRED_MEGABYTE
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var navController:NavController
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUser = mAuth.currentUser
    lateinit var userInfo: UserInfo
    val viewModel: EditViewModel by viewModels()
    lateinit var tagsArray: Array<String?>
    lateinit var conditions: Array<String?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.hide()

        navController = Navigation.findNavController(this, R.id.edit_nav_host_fragment)

        binding.toolbar.navigationIcon =
            resources.getDrawable(R.drawable.ic_baseline_arrow_back_24)
        supportActionBar!!.hide()
        binding.toolbar.inflateMenu(R.menu.edit_menu)
        binding.toolbar.setNavigationOnClickListener {
            if(navController.currentDestination!!.id == R.id.editProfileFragment)
                startMainActivity(userInfo)
            else if(navController.currentDestination!!.id == R.id.displayedCollectiblesFragment)
                navController.navigate(EditCollectiblesFragmentDirections.actionDisplayedCollectiblesFragmentToEditProfileFragment())
            else navController.navigateUp()
        }

        binding.toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
            if (item.itemId == R.id.logout) {
                mAuth.signOut()
                startLoginActivity()
            }
            false
        })

        //get user info passed from MainActivity
        userInfo = intent.extras!!.get("user_info") as UserInfo
        tagsArray = intent.extras!!.get("categories") as Array<String?>
        conditions = intent.extras!!.get("conditions") as Array<String?>


        //binding.progressBar.visibility = View.VISIBLE
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("categories", tagsArray)
        intent.putExtra("conditions", conditions)
        startActivity(intent)
    }

    private fun startMainActivity( userInfo: UserInfo){


        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user_info", userInfo)
        intent.putExtra("categories",tagsArray)
        intent.putExtra("conditions", conditions)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_menu, menu)
        return true
    }
}