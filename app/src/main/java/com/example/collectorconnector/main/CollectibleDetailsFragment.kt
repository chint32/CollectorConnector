package com.example.collectorconnector.main

import android.graphics.BitmapFactory
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
import com.example.collectorconnector.models.FavoriteCollectible


class CollectibleDetailsFragment : Fragment() {

    private lateinit var binding: FragmentCollectibleDetailsBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_collectible_details,
            container,
            false
        )
        val activity = (requireActivity() as MainActivity)

        activity.binding.toolbar.navigationIcon =
            resources.getDrawable(R.drawable.ic_baseline_arrow_back_24)
        activity.binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val collectible = requireArguments().get("collectible") as Collectible


        loadDataIntoViews(collectible)
        if ((requireActivity() as MainActivity).currentUser!!.uid != collectible.ownerId) {
            binding.ivFavorite.visibility = View.VISIBLE
            binding.btnMessageOwner.visibility = View.VISIBLE
            var views = collectible.timesViewed.toInt()
            views++
            collectible.timesViewed = views.toString()

            viewModel.isCollectibleUpdatedLiveData.observe(viewLifecycleOwner) {
                if (!it)
                    Toast.makeText(
                        requireContext(),
                        "Error updating collectible views",
                        Toast.LENGTH_SHORT
                    ).show()
                loadDataIntoViews(collectible)
            }

            viewModel.updateCollectible(collectible)
        } else {
            binding.ivFavorite.visibility = View.GONE
            binding.btnMessageOwner.visibility = View.GONE
        }

        binding.btnMessageOwner.setOnClickListener {
            (requireActivity() as MainActivity).binding.bottomNavigationView.selectedItemId
            findNavController().navigate(
                CollectibleDetailsFragmentDirections
                    .actionCollectibleDetailsFragmentToMessageFragment(
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

            val favCollectible = FavoriteCollectible(
                collectible.uid,
                collectible.ownerId
            )
            if(binding.ivFavorite.tag == "fav"){
                activity.userInfo.favoriteCollectibles.remove(favCollectible)
                binding.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_border_24)
                binding.ivFavorite.tag = "not fav"
            } else {
                activity.userInfo.favoriteCollectibles.add(favCollectible)
                binding.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_24)
                binding.ivFavorite.tag = "fav"
            }

            viewModel.updateUserInfo(activity.userInfo)
        }

        return binding.root
    }

    private fun loadDataIntoViews(collectible: Collectible) {
        binding.collectibleImg.setImageBitmap(
            BitmapFactory.decodeByteArray(
                collectible.imageByteArray,
                0,
                collectible.imageByteArray!!.size
            )
        )
        binding.tvNumViews.text = collectible.timesViewed
        binding.tvCollectibleName.text = collectible.name
        binding.tvCollectibleDesc.text = collectible.description
        binding.tvCollectibleCond.text = collectible.condition
        binding.tvCollectibleTags.text = collectible.tags.toString()

        val favCollectible = FavoriteCollectible(
            collectible.uid, collectible.ownerId
        )

        if((requireActivity() as MainActivity).userInfo.favoriteCollectibles.contains(favCollectible)){
            binding.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_24)
            binding.ivFavorite.tag = "fav"
        } else {
            binding.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_border_24)
            binding.ivFavorite.tag = "not fav"
        }
    }
}