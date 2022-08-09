package com.example.collectorconnector.edit_profile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
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
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.collectorconnector.R
import com.example.collectorconnector.databinding.FragmentEditProfileBinding
import com.example.collectorconnector.main.MainActivity
import com.example.collectorconnector.models.Address
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.util.Constants.PICK_IMAGE_REQUEST
import com.example.collectorconnector.util.Constants.REQUEST_LOCATION_PERMISSION
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private lateinit var mfusedLocationClient: FusedLocationProviderClient
    private var latitude = 0.0
    private var longitude = 0.0
    private var searchDistance = 0

    private lateinit var tagsArray: Array<String?>
    private var tagsList: ArrayList<Int> = ArrayList()
    private val finalTags = ArrayList<String>()


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
        tagsArray = activity.intent.extras!!.get("categories") as Array<String?>

        userInfo = activity.intent.extras!!.get("user_info") as UserInfo
        val selectedTags = BooleanArray(tagsArray.size)
        // initially check the tags the user is interested in
        for (i in selectedTags.indices) {
            selectedTags[i] = false
            for(interest in userInfo.interests) {
                if(interest == tagsArray[i]) selectedTags[i] = true
            }
        }
        binding.etScreenName.setText(userInfo.screenName)
        binding.tvSearchDistance.text = "Search Distance: ${userInfo.searchDistance} mi"
        binding.selectTagsTv.text = userInfo.interests.toString()
        binding.addressLine1.text = userInfo.addressLine1
        binding.searchTitle.text = userInfo.addressLine1
        binding.addressLine2.text = userInfo.addressLine2
        binding.city.text = userInfo.city
        binding.state.text = userInfo.state
        binding.zip.text = userInfo.zip
        binding.sliderSearchDistance.value = userInfo.searchDistance.toFloat()
        latitude = userInfo.latitude
        longitude = userInfo.longitude
        searchDistance = userInfo.searchDistance


        Glide.with(requireContext())
            .asBitmap()
            .load(userInfo.profileImgUrl)
            .circleCrop()
            .into(binding.profilePic)


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

        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        binding.selectTagsTv.setOnClickListener(View.OnClickListener { // Initialize alert dialog
            buildSelectTagsDialog(builder, tagsArray, selectedTags)
        })

        binding.searchTitle.setOnClickListener {
            if (binding.formLayout.visibility == View.GONE) {
                binding.formLayout.visibility = View.VISIBLE
                binding.cardView.visibility = View.GONE
                binding.searchTitle.text = binding.addressLine1.text
            }
            else {
                binding.formLayout.visibility = View.GONE
                binding.cardView.visibility = View.VISIBLE
            }
        }

        binding.btnSubmitProfile.setOnClickListener {
            loadUserInfoIntoModelAndSendUpdate(viewModel, selectedTags)
        }

        binding.cardView.setOnClickListener {
            selectImage()
        }

        binding.editCollectiblesBtn.setOnClickListener {
            findNavController().navigate(EditProfileFragmentDirections.actionEditProfileFragmentToDisplayedCollectiblesFragment())
        }

        binding.sliderSearchDistance.addOnChangeListener { slider, value, fromUser ->
            searchDistance = value.toInt()
            binding.tvSearchDistance.text = "Search Distance: $searchDistance mi"
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

    private fun loadUserInfoIntoModelAndSendUpdate(viewModel: EditViewModel, selectedTags: BooleanArray) {

        for(i in selectedTags.indices)
            if(selectedTags[i]) finalTags.add(tagsArray[i]!!)

        for(value in finalTags)println(value)

        if (binding.etScreenName.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Screen Name must not be empty", Toast.LENGTH_SHORT)
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
            latitude,
            longitude,
            binding.addressLine1.text.toString(),
            binding.addressLine2.text.toString(),
            binding.city.text.toString(),
            binding.state.text.toString(),
            binding.zip.text.toString(),
            searchDistance,
            finalTags,
            userInfo.totalRatingStars,
            userInfo.totalRates,
            userInfo.rating,
            userInfo.profileImgUrl,
            userInfo.favoriteCollectibles
        )



        if(filePath == null)
            viewModel.updateUserInfo(userInfo)
        else
            viewModel.uploadProfileImg(userInfo.uid, filePath!!)




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
                        binding.addressLine1.text =
                            address.line2 + " " + address.line1
                        binding.city.text = address.city
                        binding.zip.text = address.zip
                        binding.state.text = address.state

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

    private fun buildSelectTagsDialog(builder: AlertDialog.Builder, tags: Array<String?>, selectedTags:BooleanArray) {

        builder.setTitle("Select Categories")
        builder.setCancelable(false)

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
                var finalString = ""

                if(tagsList.size > 0) {
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
                    finalString = stringBuilder.toString()
                } else {
                    for(k in selectedTags.indices)
                        if(selectedTags[k]) stringBuilder.append(tagsArray[k]!! + ", ")
                            finalString = stringBuilder.toString().substringBeforeLast(",")
                }

                dialogInterface.dismiss()
                // set text on textView
                binding.selectTagsTv.text = finalString
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

    private fun startMainActivity( userInfo: UserInfo){


        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra("user_info", userInfo)
        intent.putExtra("categories",tagsArray)
        startActivity(intent)
    }
}