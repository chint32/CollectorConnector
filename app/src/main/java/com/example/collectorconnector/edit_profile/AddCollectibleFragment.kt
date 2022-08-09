package com.example.collectorconnector.edit_profile

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.collectorconnector.R
import com.example.collectorconnector.databinding.FragmentAddCollectibleBinding
import com.example.collectorconnector.util.Constants.PICK_IMAGE_REQUEST
import com.google.firebase.storage.ktx.storageMetadata
import java.io.IOException
import java.util.*


class AddCollectibleFragment : Fragment() {

    private lateinit var binding: FragmentAddCollectibleBinding

    // Uri indicates, where the image will be picked from
    private var filePath: Uri? = null

    // references for tags associated with the collectible to be added
    var tagsList: ArrayList<Int> = ArrayList()
    private lateinit var selectedTags:BooleanArray

    // reference to parent activity
    private lateinit var activity: EditProfileActivity


    val viewModel: EditViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_add_collectible, container, false
        )
        activity = (requireActivity() as EditProfileActivity)

        // progress dialog for uploading image
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setTitle("Uploading...")

        selectedTags = BooleanArray(activity.tagsArray.size)
        // initially, no tags are associated with the collectible to be added
        for (i in selectedTags.indices) selectedTags[i] = false

        binding.imageView2.setOnClickListener {
            selectImage()
        }

        binding.addCollectibleBtn.setOnClickListener {
            uploadImage(progressDialog)
        }

        // check whether collectible was added successfully or not via observer
        // if added successfully, navigate user back to their displayed collectibles
        viewModel.isCollectibleAddedLiveData.observe(viewLifecycleOwner, Observer {
            progressDialog.dismiss()

            if (!it) {
                Toast.makeText(
                    requireContext(),
                    "Collectible failed to be added",
                    Toast.LENGTH_SHORT
                ).show()
                return@Observer
            }


            Toast.makeText(requireContext(), "Collectible added", Toast.LENGTH_SHORT).show()
            findNavController().navigate(AddCollectibleFragmentDirections.actionAddCollectibleFragmentToDisplayedCollectiblesFragment())

        })

        binding.etSelectedTags.setOnClickListener(View.OnClickListener {
            buildSelectTagsDialog()
        })

        return binding.root
    }

    private fun buildSelectTagsDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Categories")
        builder.setCancelable(false)

        builder.setMultiChoiceItems(activity.tagsArray, selectedTags,
            DialogInterface.OnMultiChoiceClickListener { dialogInterface, i, b ->

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

                for (j in 0 until tagsList.size) {
                    stringBuilder.append(activity.tagsArray.get(tagsList.get(j)))
                    if (j != tagsList.size - 1) {
                        // When j value  not equal
                        // to lang list size - 1
                        // add comma
                        stringBuilder.append(", ")
                    }
                }

                binding.etSelectedTags.hint = stringBuilder.toString()
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
                    binding.etSelectedTags.hint = ""
                }
            })

        // show dialog
        builder.show()
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
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {

            // Get the Uri of data
            filePath = data.data
            try {

                // Setting image on image view using Bitmap
                val bitmap = MediaStore.Images.Media
                    .getBitmap(
                        requireActivity().contentResolver,
                        filePath
                    )
                binding.imageView2.setImageBitmap(bitmap)
            } catch (e: IOException) {
                // Log the exception
                e.printStackTrace()
            }
        }
    }

    // UploadImage method
    private fun uploadImage(progressDialog: ProgressDialog) {

        if (filePath == null) {
            Toast.makeText(requireContext(), "Image must not be blank", Toast.LENGTH_SHORT).show()
            return
        } else if (binding.etName.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Name must not be blank", Toast.LENGTH_SHORT).show()
            return
        } else if (binding.etDescription.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Description must not be blank", Toast.LENGTH_SHORT)
                .show()
            return
        } else if (binding.etCondition.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Condition must not be blank", Toast.LENGTH_SHORT)
                .show()
            return
        } else if (binding.etSelectedTags.hint.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Tags must not be blank", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()

        // Create file metadata including the content type
        var metadata = storageMetadata {
            contentType = "image/jpg"
            setCustomMetadata("name", binding.etName.text.toString())
            setCustomMetadata("desc", binding.etDescription.text.toString())
            setCustomMetadata("cond", binding.etCondition.text.toString())
            setCustomMetadata("tags", binding.etSelectedTags.hint.toString())
            setCustomMetadata("views", "0")
            setCustomMetadata("ownerId", activity.currentUser!!.uid)
        }

        // call to actually add collectible image and metadata to firebase cloud storage
        viewModel.addCollectible(
            activity.userInfo.uid, UUID.randomUUID().toString(),
            filePath!!, metadata
        )
    }
}