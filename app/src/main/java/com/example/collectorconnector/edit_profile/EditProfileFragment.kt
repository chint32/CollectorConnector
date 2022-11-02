package com.example.collectorconnector.edit_profile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private lateinit var mfusedLocationClient: FusedLocationProviderClient
    val viewModel: EditViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_edit_profile, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        val activity = (requireActivity() as EditProfileActivity)
        val tagsArray = activity.intent.extras!!.get("categories") as Array<String?>

        viewModel.userInfo = activity.intent.extras!!.get("user_info") as UserInfo

        val selectedTags = BooleanArray(tagsArray.size)
        // initially check the tags the user is interested in
        for (i in selectedTags.indices) {
            selectedTags[i] = false
            for(interest in viewModel.userInfo.interests) {
                if(interest == tagsArray[i]) selectedTags[i] = true
            }
        }

        //back to MainActivity should refresh it. So, create new MainActivity here and open it.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    startMainActivity(viewModel.userInfo, tagsArray)
                }
            }
        )

        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setTitle(getString(R.string.uploading_profile_info))
        progressDialog.setCancelable(false)

        viewModel.showProgressBar.observe(viewLifecycleOwner) {
            if (it) progressDialog.show()
        }

        // observe whether or not user info is successfully updated
        viewModel.isUserInfoUpdatedLiveData.observe(viewLifecycleOwner, Observer {
            binding.progressBar.visibility = View.GONE
            if (it == null) {
                Toast.makeText(requireContext(), getString(R.string.error_updating_user_info), Toast.LENGTH_SHORT)
                    .show()
                return@Observer
            }
            progressDialog.dismiss()
            startMainActivity(viewModel.userInfo, tagsArray)
        })

        binding.selectTagsTv.setOnClickListener(View.OnClickListener { // Initialize alert dialog
            buildSelectTagsDialog(tagsArray, selectedTags, tagsArray)
        })

        binding.searchTitle.setOnClickListener {
            if (binding.formLayout.visibility == View.GONE) {
                binding.formLayout.visibility = View.VISIBLE
                if(viewModel.userInfo.isLocationSet)
                    binding.searchTitle.text = "${viewModel.userInfo.city}, ${viewModel.userInfo.state}"
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

        binding.editCollectiblesBtn.setOnClickListener {
            findNavController().navigate(EditProfileFragmentDirections.actionEditProfileFragmentToDisplayedCollectiblesFragment())
        }

        binding.sliderSearchDistance.addOnChangeListener { slider, value, fromUser ->
            viewModel.userInfo.searchDistance = value.toInt()
            "Search Distance: ${value.toInt()} mi".also { binding.tvSearchDistance.text = it }
        }

        mfusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        binding.btnSetLocation.setOnClickListener {
            if (isPermissionGranted())
                getLocation()
            else checkLocationPermissions()
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
            binding.filepath = data.data
            try {

                // Setting image on image view using Bitmap
                val bitmap = MediaStore.Images.Media
                    .getBitmap(
                        requireActivity().contentResolver,
                        binding.filepath
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
                getString(R.string.select_image_from_here)
            ),
            PICK_IMAGE_REQUEST
        )
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
                        val address = geoCodeLocation(location)

                        viewModel.userInfo.city = address.city
                        viewModel.userInfo.state = address.state
                        binding.searchTitle.text = "${viewModel.userInfo.city}, ${viewModel.userInfo.state}"

                        viewModel.userInfo.isLocationSet = true
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

    private fun buildSelectTagsDialog(tags: Array<String?>, selectedTags:BooleanArray, tagsArray: Array<String?>, ) {

        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.select_categories))
        builder.setCancelable(false)
        val tagsList = ArrayList<Int>()

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

        builder.setPositiveButton(getString(R.string.ok),
            DialogInterface.OnClickListener { dialogInterface, i -> // Initialize string builder
                val stringBuilder = StringBuilder()
                var finalString = ""
                viewModel.userInfo.interests.clear()

                if(tagsList.size > 0) {
                    // use for loop
                    for (j in 0 until tagsList.size) {
                        // concat array value
                            viewModel.userInfo.interests.add(tags.get(tagsList.get(j))!!)
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

        builder.setNegativeButton(getString(R.string.cancel),
            DialogInterface.OnClickListener { dialogInterface, i -> // dismiss dialog
                dialogInterface.dismiss()
            })

        builder.setNeutralButton(getString(R.string.clear_all),
            DialogInterface.OnClickListener { dialogInterface, i ->
                viewModel.userInfo.interests.clear()

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

    private fun startMainActivity( userInfo: UserInfo, tagsArray: Array<String?>){
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra("user_info", userInfo)
        intent.putExtra("categories",tagsArray)
        intent.putExtra("conditions", (requireActivity() as EditProfileActivity).conditions)
        startActivity(intent)
    }
}