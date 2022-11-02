package com.example.collectorconnector.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.collectorconnector.R
import com.example.collectorconnector.databinding.FragmentCollectibleDetailsBinding
import com.example.collectorconnector.models.Collectible


class CollectibleDetailsFragment : Fragment() {

    private lateinit var binding: FragmentCollectibleDetailsBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_collectible_details,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        val activity = (requireActivity() as MainActivity)

        activity.binding.toolbar.navigationIcon =
            resources.getDrawable(R.drawable.ic_baseline_arrow_back_24)
        activity.binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val collectible = requireArguments().get("collectible") as Collectible
        viewModel.collectible = collectible
        binding.viewModel = viewModel

        if (activity.currentUser!!.uid != collectible.ownerId) {
            binding.ivFavorite.visibility = View.VISIBLE
            if(activity.userInfo.favoriteCollectibles.contains(collectible)) {
                binding.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_24)
                binding.ivFavorite.tag = "fav"
            }
            else {
                binding.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_border_24)
                binding.ivFavorite.tag = "not fav"
            }

            viewModel.getUserInfo(collectible.ownerId)
            viewModel.userInfoLiveData.observe(viewLifecycleOwner){
                if(it == null){
                    Toast.makeText(requireContext(), getString(R.string.error_getting_user_info), Toast.LENGTH_SHORT).show()
                    return@observe
                }
                viewModel.updateCollectibleViews(it, collectible)
            }
        }
        else {
            binding.ivFavorite.visibility = View.GONE
        }
        if(activity.currentUser.uid == collectible.ownerId){
            binding.btnMessageOwner.visibility = View.GONE
        } else {
            binding.btnMessageOwner.visibility = View.VISIBLE

        }

        viewModel.collectibleDeletedLiveData.observe(viewLifecycleOwner){
            if(it) {
                binding.ivFavorite.visibility = View.GONE
                binding.tvNumViews.visibility = View.GONE
                binding.collectibleImg.setBackgroundResource(R.drawable.content_deleted)
            }


        }


        binding.btnMessageOwner.setOnClickListener {
            (requireActivity() as MainActivity).binding.bottomNavigationView.selectedItemId
            findNavController().navigate(
                CollectibleDetailsFragmentDirections.actionCollectibleDetailsFragmentToMessageFragment(
                    collectible.ownerId
                )
            )
        }

        binding.btnOwnerProfile.setOnClickListener {
            findNavController().navigate(
                CollectibleDetailsFragmentDirections.actionCollectibleDetailsFragmentToProfileFragment(
                    collectible.ownerId
                )
            )
        }
        binding.ivFavorite.setOnClickListener {

            if(binding.ivFavorite.tag == "fav"){
                activity.userInfo.favoriteCollectibles.remove(collectible)
                binding.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_border_24)
                binding.ivFavorite.tag = "not fav"
            } else {
                activity.userInfo.favoriteCollectibles.add(collectible)
                binding.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_24)
                binding.ivFavorite.tag = "fav"
            }

            viewModel.updateProfile(activity.userInfo, null)
        }

        return binding.root
    }
}