package com.example.collectorconnector.edit_profile

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.collectorconnector.R
import com.example.collectorconnector.databinding.FragmentEditProfileBinding
import com.example.collectorconnector.main.MainActivity
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.util.Constants.PICK_IMAGE_REQUEST
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException

class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private var cityStatesList = ArrayList<Pair<String, String>>()
    private var statesList = ArrayList<String>()
    private val citiesList = ArrayList<String>()
    lateinit var tagsArray: Array<String?>
    var tagsList: ArrayList<Int> = ArrayList()
    private var myTags = ArrayList<String>()
    private var selectedState = ""
    private var selectedCity = ""

    // Uri indicates, where the image will be picked from
    private var filePath: Uri? = null

    private lateinit var userInfo: UserInfo
    val viewModel: EditViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_edit_profile, container, false)

        val activity = (requireActivity() as EditProfileActivity)

        statesList = activity.intent.extras!!.get("states") as ArrayList<String>
        cityStatesList =
            activity.intent.extras!!.get("cities_states") as ArrayList<Pair<String, String>>
        tagsArray = activity.intent.extras!!.get("categories") as Array<String?>

        val statesAdapter =
            ArrayAdapter(requireContext(), R.layout.spinner_item, statesList)
        binding.stateSpinner.adapter = statesAdapter

        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())

        userInfo = activity.intent.extras!!.get("user_info") as UserInfo
        binding.etScreenName.setText(userInfo.screenName)
        binding.stateSpinner.setSelection(statesList.indexOf(userInfo.state))
        binding.citySpinner.setSelection(citiesList.indexOf(userInfo.city))
        binding.selectTagsTv.text = userInfo.tags.toString()


        Glide.with(requireContext())
            .asBitmap()
            .load(userInfo.profileImgUrl)
            .circleCrop()
            .into(binding.profilePic)

        setSpinnerListeners()

        //back to MainActivity should refresh it. So, create new MainActivity here and open it.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    startMainActivity(userInfo)
                }
            }
        )

        // observe whether or not user info is successfully updated
        viewModel.isUserInfoUpdatedLiveData.observe(viewLifecycleOwner, Observer {
            binding.progressBar.visibility = View.GONE
            if (!it) {
                Toast.makeText(requireContext(), "Error updating user info", Toast.LENGTH_SHORT)
                    .show()
                return@Observer
            }
            startMainActivity(userInfo)
        })

        // observe whether or not user profile image is successfully uploaded or not
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

        binding.selectTagsTv.setOnClickListener(View.OnClickListener { // Initialize alert dialog
            buildSelectTagsDialog(builder)
        })

        binding.btnSubmitProfile.setOnClickListener {
            loadUserInfoIntoModelAndSendUpdate(viewModel)
        }

        binding.cardView.setOnClickListener {
            selectImage()
        }

        binding.editCollectiblesBtn.setOnClickListener {
            findNavController().navigate(EditProfileFragmentDirections.actionEditProfileFragmentToDisplayedCollectiblesFragment())
        }

        return binding.root
    }

    // Override onActivityResult method
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

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
            Intent.createChooser(
                intent,
                "Select Image from here..."
            ),
            PICK_IMAGE_REQUEST
        )
    }

    private fun loadUserInfoIntoModelAndSendUpdate(viewModel: EditViewModel) {

        var selectedTags = BooleanArray(myTags.size)
        // initially all tags are unchecked in select tags dialog
        for (i in selectedTags.indices) selectedTags[i] = false

        val finalTags = ArrayList<String>()
        for (i in myTags.indices) {
            if (selectedTags[i]) finalTags.add(myTags[i])
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

    private fun buildSelectTagsDialog(builder: AlertDialog.Builder) {

        builder.setTitle("Select Categories")
        builder.setCancelable(false)

        val tags =
            (requireActivity() as EditProfileActivity).intent.extras!!.get("categories") as Array<String?>

        val selectedTags = BooleanArray(tags.size)
        // initially all tags are unchecked
        for (i in selectedTags.indices) selectedTags[i] = false

        builder.setMultiChoiceItems(tags, selectedTags,
            DialogInterface.OnMultiChoiceClickListener { dialogInterface, i, b ->
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
                    stringBuilder.append(tags.get(tagsList.get(j)))
                    // check condition
                    if (j != tagsList.size - 1) {
                        // When j value  not equal
                        // to lang list size - 1
                        // add comma
                        stringBuilder.append(", ")
                    }
                }

                dialogInterface.dismiss()
                // set text on textView
                myTags.add(stringBuilder.toString())
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
        val dialog = builder.create()
        if (!dialog.isShowing) dialog.show()

    }

    private fun loadCitySpinnerBasedOnState(position: Int) {
        citiesList.clear()
        for (cityState in cityStatesList) {
            if (statesList[position] == cityState.first)
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
                selectedState = statesList[position]
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
        intent.putExtra("states", statesList)
        intent.putExtra("cities_states", cityStatesList)
        intent.putExtra("categories",tagsArray)
        startActivity(intent)
    }
}