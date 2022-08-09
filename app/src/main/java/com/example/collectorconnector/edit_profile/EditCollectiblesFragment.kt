package com.example.collectorconnector.edit_profile

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.collectorconnector.R
import com.example.collectorconnector.adapters.CollectibleAdapter
import com.example.collectorconnector.databinding.FragmentEditCollectiblesBinding
import com.example.collectorconnector.models.Collectible
import com.example.collectorconnector.util.Constants


class EditCollectiblesFragment : Fragment(), CollectibleAdapter.OnItemClickListener, CollectibleAdapter.OnFavoriteClickListener {

    private lateinit var binding: FragmentEditCollectiblesBinding
    private var showCheckBoxes = true
    private val checkedItems = ArrayList<Int>()
    val myCollectibles = ArrayList<Collectible>()
    private val collectiblesDeleted = ArrayList<Collectible>()
    private val viewModel:EditViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_edit_collectibles,
            container,
            false
        )
        // reference to activity where some needed references (viewModel, myCOllectibles)
        // are instantiated
        val activity = (requireActivity() as EditProfileActivity)

        val adapter = CollectibleAdapter(myCollectibles, activity.userInfo, this, this, showCheckBoxes, false)
        binding.collectiblesRecycler.adapter = adapter
        binding.collectiblesRecycler.layoutManager = GridLayoutManager(requireContext(), 2)

        viewModel.clearCollectiblesLiveData()
        
        // observe this users collectibles
        viewModel.collectiblesLiveData.observe(viewLifecycleOwner, Observer {
            if(it == null) {
                binding.tvNoResults.text = "Error retrieving your collectibles. Please try again"
                binding.tvNoResults.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
                return@Observer
            }
            if(it.items.isEmpty()) {
                binding.tvNoResults.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
                return@Observer
            }

            // cycle through through this users collectibles
            for (item in it.items) {

                //get collectible metadata and image byte array for each user
                item.metadata.addOnSuccessListener { metadata ->
                    item.getBytes(Constants.ONE_HUNDRED_MEGABYTE)
                        .addOnSuccessListener { byteArray ->
                            binding.progressBar.visibility = View.GONE

                            val tags = ArrayList<String>()
                            val tagsArr =
                                metadata.getCustomMetadata("tags").toString()
                                    .split(",")
                            for (i in tagsArr.indices)
                                tags.add(tagsArr[i])

                            val collectible = Collectible(
                                item.name,
                                metadata.getCustomMetadata("name").toString(),
                                metadata.getCustomMetadata("desc").toString(),
                                metadata.getCustomMetadata("cond").toString(),
                                byteArray,
                                metadata.getCustomMetadata("views").toString(),
                                tags,
                                activity.currentUser!!.uid
                            )
                            if(!myCollectibles.contains(collectible)){
                                myCollectibles.add(collectible)
                                adapter.notifyItemInserted(myCollectibles.indexOf(collectible))
                            }
                        }.addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "Error: " + it.message,
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.progressBar.visibility = View.GONE
                        }
                }
            }
        })

        viewModel.getCollectiblesByUid(activity.currentUser!!.uid)
        binding.progressBar.visibility = View.VISIBLE

        // observe whether or not collectible deletion was successful or not
        viewModel.isCollectibleDeletedLiveData.observe(
            viewLifecycleOwner,
            Observer {
                if (!it) {
                    Toast.makeText(
                        requireContext(),
                        "Error deleting collectible",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    return@Observer
                }
                if(collectiblesDeleted.isEmpty()) return@Observer

                Toast.makeText(
                    requireContext(),
                    "Collectible deleted",
                    Toast.LENGTH_SHORT
                ).show()
                val deleteCollectible = collectiblesDeleted[collectiblesDeleted.lastIndex]
                val index = myCollectibles.indexOf(deleteCollectible)
                myCollectibles.removeAt(index)
                adapter.notifyItemRemoved(index)
                collectiblesDeleted.remove(deleteCollectible)

                binding.progressBar.visibility = View.GONE
            })

        binding.btnDelete.setOnClickListener{

            for(index in checkedItems){
                collectiblesDeleted.add(myCollectibles[index])
            }
            binding.progressBar.visibility = View.VISIBLE

            //make call to delete collectibles from firebase cloud storage
            for(collectible in collectiblesDeleted){
                viewModel.deleteCollectible(activity.currentUser.uid, collectible.uid)
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
    override fun onItemClick(position: Int) {
        if(checkedItems.contains(position))
            checkedItems.remove(position)
        else
            checkedItems.add(position)

        if(checkedItems.isNotEmpty()) {
            binding.btnDelete.isEnabled = true
            binding.btnDelete.alpha = 1f
        } else {
            binding.btnDelete.isEnabled = false
            binding.btnDelete.alpha = .5f
        }

    }

    override fun onFavoriteClick(position: Int) {
        // nothing to do, you cant favorite your own collectibles
    }
}