package com.example.collectorconnector.auth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.DialogInterface.OnMultiChoiceClickListener
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import java.io.IOException
import java.util.*


class QuestionnaireFragment : Fragment() {

    private lateinit var binding: FragmentQuestionnaireBinding
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var mfusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_questionnaire, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val activity = (requireActivity() as LoginActivity)
        viewModel.email = requireArguments().getString("email")!!
        viewModel.pw = requireArguments().getString("pw")!!
        val selectedTags = BooleanArray(activity.tagsArray.size)

        // initially all tags are unchecked in select tags dialog
        for (i in selectedTags.indices) selectedTags[i] = false

        mfusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        binding.btnSetLocation.setOnClickListener {
            if (isPermissionGranted())
                getLocation()
            else checkLocationPermissions()
        }

        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setTitle(getString(R.string.creating_profile))
        progressDialog.setCancelable(false)

        viewModel.showProgressBar.observe(viewLifecycleOwner) {
            if (it) progressDialog.show()
        }

        viewModel.authenticatedUserLiveData.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_email_exists_toast),
                    Toast.LENGTH_SHORT
                ).show()
                progressDialog.dismiss()
                return@Observer
            }
            viewModel.userInfo.uid = it.user!!.uid
            viewModel.updateProfile(viewModel.userInfo, viewModel.filePath)
        })


        viewModel.isUserInfoUpdatedLiveData.observe(viewLifecycleOwner, Observer {
            progressDialog.dismiss()
            if (it == null) {
                Toast.makeText(requireContext(), getString(R.string.error_updating_user_info), Toast.LENGTH_SHORT)
                    .show()
                return@Observer
            }
            progressDialog.dismiss()
            Toast.makeText(
                requireContext(),
                "${viewModel.userInfo.screenName} registered successfully",
                Toast.LENGTH_SHORT
            ).show()
            startMainActivity(viewModel.userInfo)
        })

        binding.selectTagsTv.setOnClickListener { // Initialize alert dialog
            val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
            buildSelectTagsDialog(builder, selectedTags)
            builder.show()
        }

        binding.searchTitle.setOnClickListener {
            if (binding.formLayout.visibility == View.GONE) {
                binding.formLayout.visibility = View.VISIBLE
                if(viewModel.userInfo.isLocationSet)
                    "${viewModel.userInfo.city}, ${viewModel.userInfo.state}".also { binding.searchTitle.text = it }
                else
                    binding.searchTitle.text = getString(R.string.set_your_location)
            }
            else {
                binding.formLayout.visibility = View.GONE
            }
        }

        binding.cardView.setOnClickListener {
            selectImage()
        }

        binding.sliderSearchDistance.addOnChangeListener { slider, value, fromUser ->
            viewModel.userInfo.searchDistance = value.toInt()
            "Search Distance: ${viewModel.userInfo.searchDistance} mi".also { binding.tvSearchDistance.text = it }
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
                        viewModel.userInfo.latitude = location.latitude
                        viewModel.userInfo.longitude = location.longitude
                        viewModel.userInfo.isLocationSet = true

                        val address = geoCodeLocation(location)

                        viewModel.userInfo.city = address.city
                        viewModel.userInfo.state = address.state
                        binding.searchTitle.text = "${viewModel.userInfo.city}, ${viewModel.userInfo.state}"
                    }
                )
            } else {
                Toast.makeText(requireContext(), getString(R.string.turn_on_location), Toast.LENGTH_LONG).show()

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

    @RequiresApi(Build.VERSION_CODES.DONUT)
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
            viewModel.filePath = data.data
            try {

                // Setting image on image view using Bitmap
                val bitmap = MediaStore.Images.Media
                    .getBitmap(
                        requireActivity().contentResolver,
                        viewModel.filePath
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
            Intent.createChooser(intent, getString(R.string.select_image_from_here)),
            PICK_IMAGE_REQUEST
        )
    }


    private fun buildSelectTagsDialog(builder: AlertDialog.Builder, selectedTags: BooleanArray) {

        var tagsList: ArrayList<Int> = ArrayList()

        // set title
        builder.setTitle(getString(R.string.select_categories))

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

        builder.setPositiveButton(getString(R.string.ok),
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

                for (i in (requireActivity() as LoginActivity).tagsArray.indices) {
                    if (selectedTags[i]) viewModel.userInfo.interests.add((requireActivity() as LoginActivity).tagsArray[i]!!)
                }

                // set text on textView
                binding.selectTagsTv.text = stringBuilder.toString()
            })

        builder.setNegativeButton(requireContext().getString(R.string.cancel),
            DialogInterface.OnClickListener { dialogInterface, i -> // dismiss dialog
                dialogInterface.dismiss()
            })

        builder.setNeutralButton(getString(R.string.clear_all),
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
                viewModel.userInfo.interests.clear()
            })

    }


    private fun startMainActivity(userInfo: UserInfo) {

        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra("user_info", userInfo)
        intent.putExtra("categories", (requireActivity() as LoginActivity).tagsArray)
        intent.putExtra("conditions", (requireActivity() as LoginActivity).conditions)
        startActivity(intent)
    }
}