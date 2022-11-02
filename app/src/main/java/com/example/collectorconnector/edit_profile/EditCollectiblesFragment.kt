package com.example.collectorconnector.edit_profile

import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.collectorconnector.R
import com.example.collectorconnector.adapters.CollectibleAdapter
import com.example.collectorconnector.databinding.FragmentEditCollectiblesBinding
import com.example.collectorconnector.models.Collectible

class EditCollectiblesFragment : Fragment(), CollectibleAdapter.OnItemClickListener,
    CollectibleAdapter.OnFavoriteClickListener {

    private lateinit var binding: FragmentEditCollectiblesBinding
    private val checkedItems = ArrayList<Int>()
    private val collectiblesToDelete = ArrayList<Collectible>()
    private val viewModel: EditViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_edit_collectibles,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        // reference to activity where some needed references (viewModel, myCOllectibles)
        // are instantiated
        val activity = (requireActivity() as EditProfileActivity)
        viewModel.userInfo = activity.userInfo

        val adapter =
            CollectibleAdapter(activity.userInfo.collectibles, activity.userInfo, this, this, true, false)
        binding.collectiblesRecyclerNear.adapter = adapter

        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setCancelable(false)
        progressDialog.setTitle(getString(R.string.deleting_collectible))

        // observe whether or not collectible deletion was successful or not
        viewModel.isCollectibleDeletedLiveData.observe(viewLifecycleOwner) {
            progressDialog.dismiss()
            if (it == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_deleting_collectible_toast),
                    Toast.LENGTH_SHORT
                )
                    .show()
                return@observe
            }

            activity.userInfo.collectibles.remove(it)
            adapter.submitList(activity.userInfo.collectibles)
        }

        binding.btnDelete.setOnClickListener {
            //make call to delete collectibles from firebase cloud storage
            progressDialog.show()
            for (collectible in collectiblesToDelete) {
                viewModel.deleteCollectible(activity.userInfo, collectible)
            }
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(
                EditCollectiblesFragmentDirections.actionDisplayedCollectiblesFragmentToAddCollectibleFragment()
            )
        }

        // after adding a collectible, the user is directed
        // to this fragment (edit collectibles fragment).
        // Instead of back navigation going back to add
        // collectible fragment, user should be directed
        // to edit profile fragment
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(EditCollectiblesFragmentDirections.actionDisplayedCollectiblesFragmentToEditProfileFragment())
                }
            }
        )

        return binding.root
    }

    // allow user to mark collectibles for deletion.
    // btn_delete will delete all marked collectibles
    override fun onItemClick(position: Int, collectible: Collectible, isChecked: Boolean) {
        if (checkedItems.contains(position)) {
            checkedItems.remove(position)
            collectiblesToDelete.remove(collectible)
        } else {
            checkedItems.add(position)
            collectiblesToDelete.add(collectible)
        }

        if (checkedItems.isNotEmpty()) {
            binding.btnDelete.isEnabled = true
            binding.btnDelete.alpha = 1f
        } else {
            binding.btnDelete.isEnabled = false
            binding.btnDelete.alpha = .5f
        }
    }

    override fun onFavoriteClick(position: Int, collectible: Collectible) {
        // nothing to do, you cant favorite your own collectibles
        // its only implemented so I can make use of collectible adapter
        // which requires implementing CollectibleAdapter.onFavoriteClick
    }
}