package com.example.collectorconnector.auth

import android.app.Activity
import android.content.DialogInterface
import android.content.DialogInterface.OnMultiChoiceClickListener
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.collectorconnector.R
import com.example.collectorconnector.databinding.FragmentQuestionnaireBinding
import com.example.collectorconnector.main.MainActivity
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.util.Constants.PICK_IMAGE_REQUEST
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException


class QuestionnaireFragment : Fragment() {

    private lateinit var binding: FragmentQuestionnaireBinding
    private val citiesList = ArrayList<String>()
    var tagsList: ArrayList<Int> = ArrayList()

    private var selectedState = ""
    private var selectedCity = ""

    // Uri indicates, where the image will be picked from
    private var filePath: Uri? = null

    private lateinit var userInfo: UserInfo
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var email: String
    private lateinit var pw: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_questionnaire, container, false)

        val activity = (requireActivity() as LoginActivity)
        email = requireArguments().get("email").toString()
        pw = requireArguments().get("password").toString()
        var selectedTags = BooleanArray(activity.tagsArray.size)

        // initially all tags are unchecked in select tags dialog
        for (i in selectedTags.indices) selectedTags[i] = false

        println(activity.statesList)
        val statesAdapter =
            ArrayAdapter(requireContext(), R.layout.spinner_item, activity.statesList)
        binding.stateSpinner.adapter = statesAdapter

        setSpinnerListeners()

        viewModel.isUserInfoUpdatedLiveData.observe(viewLifecycleOwner, Observer {
            if (!it) {
                Toast.makeText(requireContext(), "Error updating user info", Toast.LENGTH_SHORT)
                    .show()
                return@Observer
            }

            Toast.makeText(
                requireContext(),
                "${userInfo.screenName} registered successfully",
                Toast.LENGTH_SHORT
            ).show()
            startMainActivity(userInfo)
        })

        viewModel.isProfileImgUploadedLiveData.observe(viewLifecycleOwner, Observer {
            binding.progressBar.visibility = View.GONE
            if (it == null) {
                Toast.makeText(
                    requireContext(),
                    "Error uploading profile image",
                    Toast.LENGTH_SHORT
                ).show()
                return@Observer
            }
            userInfo.profileImgUrl = it

            viewModel.updateUserInfo(userInfo)
        })

        viewModel.authenticatedUserLiveData.observe(viewLifecycleOwner, Observer{
            if (it.user == null){
                Toast.makeText(requireContext(), "User is null", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                return@Observer
            }

            binding.progressBar.visibility = View.GONE

            loadUserInfoIntoModelAndSendUpdate(viewModel, selectedTags)
        })

        binding.selectTagsTv.setOnClickListener { // Initialize alert dialog
            val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
            buildSelectTagsDialog(builder, selectedTags)
            builder.show()
        }

        binding.button.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            viewModel.registerWithEmailAndPw(email, pw)
        }

        binding.cardView.setOnClickListener {
            selectImage()
        }

        return binding.root
    }

    // Override onActivityResult method
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // checking request code and result code
        // if request code is PICK_IMAGE_REQUEST and
        // resultCode is RESULT_OK
        // then set image in the image view
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {

            // Get the Uri of data
            filePath = data.data
            try {

                // Setting image on image view using Bitmap
                val bitmap = MediaStore.Images.Media
                    .getBitmap(
                        requireActivity().contentResolver,
                        filePath
                    )
                Glide.with(requireContext())
                    .load(bitmap)
                    .fitCenter()
                    .circleCrop()
                    .into(binding.profilePic)
            } catch (e: IOException) {
                // Log the exception
                e.printStackTrace()
            }
        }
    }

    // Select Image method
    private fun selectImage() {

        // Defining Implicit Intent to mobile gallery
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Image from here..."),
            PICK_IMAGE_REQUEST
        )
    }

    private fun loadUserInfoIntoModelAndSendUpdate(viewModel: AuthViewModel, selectedTags: BooleanArray) {




        val finalTags = ArrayList<String>()
        for (i in (requireActivity() as LoginActivity).tagsArray.indices) {
            if (selectedTags[i]) finalTags.add((requireActivity() as LoginActivity).tagsArray[i]!!)
        }

        if (binding.etScreenName.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Screen Name must not be empty", Toast.LENGTH_SHORT)
                .show()
            return
        } else if (filePath == null) {
            Toast.makeText(requireContext(), "Profile image must not be empty", Toast.LENGTH_SHORT)
                .show()
            return
        } else if (finalTags.isEmpty()) {
            Toast.makeText(requireContext(), "Interests must not be empty", Toast.LENGTH_SHORT)
                .show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        userInfo = UserInfo(
            FirebaseAuth.getInstance().currentUser!!.uid,
            binding.etScreenName.text.toString(),
            selectedCity,
            selectedState,
            finalTags
        )

        viewModel.uploadProfileImg(userInfo.uid, filePath!!)
    }

    private fun buildSelectTagsDialog(builder: AlertDialog.Builder, selectedTags: BooleanArray) {

        // set title
        builder.setTitle("Select Categories")

        // set dialog non cancelable
        builder.setCancelable(false)
        println((requireActivity() as LoginActivity).tagsArray)

        builder.setMultiChoiceItems((requireActivity() as LoginActivity).tagsArray, selectedTags,
            OnMultiChoiceClickListener { dialogInterface, i, b ->
                // check condition
                if (b) {
                    // when checkbox selected
                    // Add position  in lang list
                    tagsList.add(i)
                    // Sort array list
                    tagsList.sort()
                } else {
                    // when checkbox unselected
                    // Remove position from langList
                    tagsList.remove(Integer.valueOf(i))
                }
            })

        builder.setPositiveButton("OK",
            DialogInterface.OnClickListener { dialogInterface, i -> // Initialize string builder
                val stringBuilder = StringBuilder()

                // use for loop
                for (j in 0 until tagsList.size) {
                    // concat array value
                    stringBuilder.append((requireActivity() as LoginActivity).tagsArray.get(tagsList.get(j)))
                    // check condition
                    if (j != tagsList.size - 1) {
                        // When j value  not equal
                        // to lang list size - 1
                        // add comma
                        stringBuilder.append(", ")
                    }
                }

                // set text on textView
                binding.selectTagsTv.text = stringBuilder.toString()
            })

        builder.setNegativeButton("Cancel",
            DialogInterface.OnClickListener { dialogInterface, i -> // dismiss dialog
                dialogInterface.dismiss()
            })

        builder.setNeutralButton("Clear All",
            DialogInterface.OnClickListener { dialogInterface, i ->

                // use for loop
                for (j in 0 until selectedTags.size) {
                    // remove all selection
                    selectedTags[j] = false
                    // clear language list
                    tagsList.clear()
                    // clear text view value
                    binding.selectTagsTv.text = ""
                }
            })

    }

    private fun loadCitySpinnerBasedOnState(position: Int) {
        citiesList.clear()
        for (cityState in (requireActivity() as LoginActivity).cityStatesList) {
            if ((requireActivity() as LoginActivity).statesList[position] == cityState.first)
                citiesList.add(cityState.second)
        }
    }

    private fun setSpinnerListeners() {

        binding.stateSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View, position: Int, id: Long
            ) {
                selectedState = (requireActivity() as LoginActivity).statesList[position]
                loadCitySpinnerBasedOnState(position)

                val citiesAdapter =
                    ArrayAdapter(requireContext(), R.layout.spinner_item, citiesList)
                binding.citySpinner.adapter = citiesAdapter
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        binding.citySpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View, position: Int, id: Long
            ) {
                selectedCity = citiesList[position]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
    }

    private fun startMainActivity( userInfo: UserInfo){

        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra("user_info", userInfo)
        intent.putExtra("states", (requireActivity() as LoginActivity).statesList)
        intent.putExtra("cities_states", (requireActivity() as LoginActivity).cityStatesList)
        intent.putExtra("categories",(requireActivity() as LoginActivity).tagsArray)
        startActivity(intent)
    }
}