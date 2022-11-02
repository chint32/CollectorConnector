package com.example.collectorconnector.edit_profile

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
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
import java.io.IOException
import java.util.*


class AddCollectibleFragment : Fragment() {

    private lateinit var binding: FragmentAddCollectibleBinding
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
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        val activity = (requireActivity() as EditProfileActivity)
        viewModel.userInfo = activity.userInfo

        // progress dialog for uploading image
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setTitle(getString(R.string.uploading))
        progressDialog.setCancelable(false)

        viewModel.showProgressBar.observe(viewLifecycleOwner){
            if(it){
                progressDialog.show()
            }
        }

        // check whether collectible was added successfully or not via observer
        // if added successfully, navigate user back to their displayed collectibles
        viewModel.isCollectibleAddedLiveData.observe(viewLifecycleOwner, Observer {
            progressDialog.dismiss()

            if (it == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.collectible_add_failed),
                    Toast.LENGTH_SHORT
                ).show()
                return@Observer
            }
            Toast.makeText(requireContext(), getString(R.string.collectible_added_toast), Toast.LENGTH_SHORT).show()
            findNavController().navigate(AddCollectibleFragmentDirections.actionAddCollectibleFragmentToDisplayedCollectiblesFragment())
        })

        binding.imageView2.setOnClickListener {
            selectImage()
        }

        binding.etCondition.setOnClickListener {
            buildConditionDialog(activity.conditions)
        }

        binding.etSelectedTags.setOnClickListener {
            buildSelectTagsDialog()
        }

        return binding.root
    }

    private fun buildConditionDialog(conditions: Array<String?>) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.select_condition))
        builder.setCancelable(false)


        var checkedItem = arrayOf(-1)

        var selectedCondition = ""

        builder.setSingleChoiceItems(conditions, checkedItem[0],
            DialogInterface.OnClickListener { dialogInterface, i ->

                checkedItem[0] = i
                selectedCondition = conditions[checkedItem[0]].toString()
            })

        builder.setPositiveButton(requireContext().getString(R.string.ok),
            DialogInterface.OnClickListener { dialogInterface, i -> // Initialize string builder

                binding.etCondition.text = selectedCondition
            })

        builder.setNegativeButton(requireContext().getString(R.string.cancel),
            DialogInterface.OnClickListener { dialogInterface, i -> // dismiss dialog
                dialogInterface.dismiss()
            })

        builder.setNeutralButton(requireContext().getString(R.string.clear_all),
            DialogInterface.OnClickListener { dialogInterface, i ->
                checkedItem[0] = -1
                selectedCondition = ""
                binding.etCondition.text = requireContext().getString(R.string.select_condition)

            })

        // show dialog
        builder.show()
    }

    private fun buildSelectTagsDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.select_categories))
        builder.setCancelable(false)
        val activity = (requireActivity() as EditProfileActivity)

        // references for tags associated with the collectible to be added
        val tagsList: ArrayList<Int> = ArrayList()
        var selectedTags: BooleanArray = BooleanArray(activity.tagsArray.size)
        // initially, no tags are associated with the collectible to be added
        for (i in selectedTags.indices) selectedTags[i] = false

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

        builder.setPositiveButton(requireContext().getString(R.string.ok),
            DialogInterface.OnClickListener { dialogInterface, i -> // Initialize string builder
                val stringBuilder = StringBuilder()

                for (j in 0 until tagsList.size) {
                    stringBuilder.append(activity.tagsArray.get(tagsList.get(j)))
                    viewModel.collectible.tags.add(activity.tagsArray[tagsList[j]]!!)
                    if (j != tagsList.size - 1) {
                        // When j value  not equal
                        // to lang list size - 1
                        // add comma
                        stringBuilder.append(", ")
                    }
                }

                binding.etSelectedTags.hint = stringBuilder.toString()
            })

        builder.setNegativeButton(requireContext().getString(R.string.cancel),
            DialogInterface.OnClickListener { dialogInterface, i -> // dismiss dialog
                dialogInterface.dismiss()
            })

        builder.setNeutralButton(requireContext().getString(R.string.clear_all),
            DialogInterface.OnClickListener { dialogInterface, i ->
                // use for loop

                for (j in 0 until selectedTags.size) {
                    // remove all selection
                    selectedTags[j] = false
                    // clear language list
                    tagsList.clear()
                    viewModel.collectible.tags.clear()
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
                getString(R.string.select_image_from_here)
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
            binding.filepath = data.data!!
            try {

                // Setting image on image view using Bitmap
                val bitmap = MediaStore.Images.Media
                    .getBitmap(
                        requireActivity().contentResolver,
                        binding.filepath
                    )
                binding.imageView2.setImageBitmap(bitmap)
            } catch (e: IOException) {
                // Log the exception
                e.printStackTrace()
            }
        }
    }
}