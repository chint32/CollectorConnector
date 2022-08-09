package com.example.collectorconnector.auth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.DialogInterface.OnMultiChoiceClickListener
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.collectorconnector.R
import com.example.collectorconnector.databinding.FragmentQuestionnaireBinding
import com.example.collectorconnector.main.MainActivity
import com.example.collectorconnector.models.Address
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.util.Constants.PICK_IMAGE_REQUEST
import com.example.collectorconnector.util.Constants.REQUEST_LOCATION_PERMISSION
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException
import java.util.*


class QuestionnaireFragment : Fragment() {

    private var latitude = 0.0
    private var longitude = 0.0
    private lateinit var mfusedLocationClient: FusedLocationProviderClient
    private var searchDistance = 0

    private lateinit var binding: FragmentQuestionnaireBinding
    var tagsList: ArrayList<Int> = ArrayList()


    // Uri indicates, where the image will be picked from
    private var filePath: Uri? = null

    private lateinit var userInfo: UserInfo
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var email: String
    private lateinit var pw: String
    private val finalTags = ArrayList<String>()


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


        mfusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        binding.btnSetLocation.setOnClickListener {
            if (isPermissionGranted())
                getLocation()
            else checkLocationPermissions()

        }

        viewModel.isUserInfoUpdatedLiveData.observe(viewLifecycleOwner, Observer {
            binding.progressBar.visibility = View.GONE
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

        viewModel.authenticatedUserLiveData.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                Toast.makeText(requireContext(), "Error, that email already exists", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                return@Observer
            }


            loadUserInfoIntoModelAndSendUpdate(viewModel)
        })

        binding.selectTagsTv.setOnClickListener { // Initialize alert dialog
            val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
            buildSelectTagsDialog(builder, selectedTags)
            builder.show()
        }

        binding.searchTitle.setOnClickListener {
            if (binding.formLayout.visibility == View.GONE)
                binding.formLayout.visibility = View.VISIBLE
            else {
                binding.formLayout.visibility = View.GONE
                if(binding.addressLine1.text.toString() != "")
                    binding.searchTitle.text = binding.addressLine1.text
            }
        }

        binding.btnSubmit.setOnClickListener {

            for (i in (requireActivity() as LoginActivity).tagsArray.indices) {
                if (selectedTags[i]) finalTags.add((requireActivity() as LoginActivity).tagsArray[i]!!)
            }

            if (binding.etScreenName.text.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Screen Name must not be empty", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            } else if (filePath == null) {
                Toast.makeText(requireContext(), "Profile image must not be empty", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            } else if (finalTags.isEmpty()) {
                Toast.makeText(requireContext(), "Interests must not be empty", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            } else if(latitude == 0.0 && longitude == 0.0){
                Toast.makeText(requireContext(), "Please set your location for distance filter to work", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE

            viewModel.registerWithEmailAndPw(email, pw)
        }

        binding.cardView.setOnClickListener {
            selectImage()
        }

        binding.sliderSearchDistance.addOnChangeListener { slider, value, fromUser ->
            searchDistance = value.toInt()
            binding.tvSearchDistance.text = "Search Distance: $searchDistance mi"
        }

        return binding.root
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            }
        }
    }

    private fun checkLocationPermissions(): Boolean {
        return if (isPermissionGranted()) {
            true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
            false
        }
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) === PackageManager.PERMISSION_GRANTED
    }


    @SuppressLint("MissingPermission")
    private fun getLocation() {
        if (isPermissionGranted()) {
            if (checkLocationPermissions()) {
                mfusedLocationClient.lastLocation.addOnCompleteListener(
                    OnCompleteListener<Location?> { task ->
                        val location = task.result!!
                        latitude = location.latitude
                        longitude = location.longitude

                        val address = geoCodeLocation(location)
                        binding.addressLine1.setText(
                            address.line2 + " " + address.line1,
                            TextView.BufferType.EDITABLE
                        )
                        binding.city.setText(address.city, TextView.BufferType.EDITABLE)
                        binding.state.text = address.state
                        binding.zip.setText(address.zip, TextView.BufferType.EDITABLE)
                    }
                )
            } else {
                Toast.makeText(requireContext(), "Turn on location", Toast.LENGTH_LONG).show()

                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun geoCodeLocation(location: Location): Address {
        val geocoder = Geocoder(context, Locale.getDefault())
        return geocoder.getFromLocation(location.latitude, location.longitude, 1)
            .map { address ->
                Address(
                    address.thoroughfare,
                    address.subThoroughfare,
                    address.locality,
                    address.adminArea,
                    address.postalCode
                )
            }
            .first()
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

    private fun loadUserInfoIntoModelAndSendUpdate(
        viewModel: AuthViewModel
    ) {

        userInfo = UserInfo(
            FirebaseAuth.getInstance().currentUser!!.uid,
            binding.etScreenName.text.toString(),
            latitude,
            longitude,
            binding.addressLine1.text.toString(),
            binding.addressLine2.text.toString(),
            binding.city.text.toString(),
            binding.state.text.toString(),
            binding.zip.text.toString(),
            searchDistance,
            finalTags
        )
        println(userInfo.interests)

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
                    stringBuilder.append(
                        (requireActivity() as LoginActivity).tagsArray.get(
                            tagsList.get(
                                j
                            )
                        )
                    )
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


    private fun startMainActivity(userInfo: UserInfo) {

        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra("user_info", userInfo)
        intent.putExtra("categories", (requireActivity() as LoginActivity).tagsArray)
        startActivity(intent)
    }
}