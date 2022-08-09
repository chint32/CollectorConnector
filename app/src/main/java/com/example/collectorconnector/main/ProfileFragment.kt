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
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.collectorconnector.BlurTransformation
import com.example.collectorconnector.R
import com.example.collectorconnector.adapters.CollectibleAdapter
import com.example.collectorconnector.databinding.FragmentProfileBinding
import com.example.collectorconnector.models.Collectible
import com.example.collectorconnector.models.FavoriteCollectible
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.util.Constants.ONE_HUNDRED_MEGABYTE

class ProfileFragment : Fragment(), CollectibleAdapter.OnItemClickListener,
    CollectibleAdapter.OnFavoriteClickListener {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var activity: MainActivity
    private val viewModel: MainViewModel by viewModels()
    private val profileCollectibles = ArrayList<Collectible>()
    private var isProfileOwner = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        activity = requireActivity() as MainActivity
        activity.binding.toolbar.navigationIcon = null
        // if passed a userId, load userInfo for that user
        // otherwise load the userInfo for the logged in user
        if (requireArguments().get("userId") != null) {
            isProfileOwner = false
            viewModel.getUserInfo(requireArguments().get("userId").toString())
            viewModel.getThisUsersCollectibles(requireArguments().get("userId").toString())
        } else {
            isProfileOwner = true
            viewModel.getUserInfo(activity.currentUser!!.uid)
            viewModel.getThisUsersCollectibles(activity.currentUser!!.uid)
        }
        binding.progressBar.visibility = View.VISIBLE
        val profileCollectiblesAdapter =
            CollectibleAdapter(profileCollectibles, activity.userInfo, this, this, false, isProfileOwner)
        binding.collectiblesRecycler.adapter = profileCollectiblesAdapter
        binding.collectiblesRecycler.layoutManager = GridLayoutManager(requireContext(), 2)

        viewModel.userInfoLiveData.observe(viewLifecycleOwner) {
            if (it == null) {
                Toast.makeText(requireContext(), "Error retrieving profile pic", Toast.LENGTH_SHORT)
                    .show()
                return@observe
            }
            val userInfo = it.toObject(UserInfo::class.java)
            if(userInfo == null){
                binding.tvScreenName.text = "Error"
                binding.rating.rating = 0f
                binding.tvTotalRates.text = "(0)"

                // profile image
                Glide.with(requireContext())
                    .asBitmap()
                    .load(R.drawable.ic_baseline_cloud_off_24)
                    .circleCrop()
                    .into(binding.profilePic)

                // blurred profile image behind
                Glide.with(requireContext())
                    .asBitmap()
                    .load(R.drawable.ic_baseline_cloud_off_24)
                    .transform(BlurTransformation(requireContext()))
                    .into(binding.ivBlurProfile)
            } else {
                binding.tvScreenName.text = userInfo.screenName
                binding.rating.rating = userInfo.rating
                binding.tvTotalRates.text = "(" + userInfo.totalRates + ")"

                // profile image
                Glide.with(requireContext())
                    .asBitmap()
                    .load(userInfo.profileImgUrl)
                    .circleCrop()
                    .into(binding.profilePic)

                // blurred profile image behind
                Glide.with(requireContext())
                    .asBitmap()
                    .load(userInfo.profileImgUrl)
                    .transform(BlurTransformation(requireContext()))
                    .into(binding.ivBlurProfile)

                if(!isProfileOwner)
                    binding.btnFavorites.visibility = View.GONE
                else binding.btnFavorites.visibility = View.VISIBLE
            }
        }

        if (!viewModel.thisUsersCollectiblesLiveData.hasObservers()) {
            viewModel.thisUsersCollectiblesLiveData.observe(activity) {
                if (it == null) {
                    Toast.makeText(
                        requireContext(),
                        "Error retrieving user collectibles",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    binding.progressBar.visibility = View.GONE
                    return@observe
                }
                if (it.items.isEmpty()) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvNoResults.visibility = View.VISIBLE
                    return@observe
                }


                // cycle through collectibles for all users
                for (item in it.items) {
                    item.metadata.addOnSuccessListener { metadata ->
                        item.getBytes(ONE_HUNDRED_MEGABYTE)
                            .addOnSuccessListener { byteArray ->
                                binding.progressBar.visibility = View.GONE

                                val tags = ArrayList<String>()
                                val tagsArr = metadata.getCustomMetadata("tags")
                                    .toString()
                                    .split(",")
                                for (i in tagsArr.indices)
                                    tags.add(tagsArr[i])
                                // construct model
                                val collectible = Collectible(
                                    item.name,
                                    metadata.getCustomMetadata("name").toString(),
                                    metadata.getCustomMetadata("desc").toString(),
                                    metadata.getCustomMetadata("cond").toString(),
                                    byteArray,
                                    metadata.getCustomMetadata("views").toString(),
                                    tags,
                                    metadata.getCustomMetadata("ownerId").toString()
                                )
                                if (!profileCollectibles.contains(collectible)) {
                                    profileCollectibles.add(collectible)
                                    profileCollectiblesAdapter.notifyItemInserted(
                                        profileCollectibles.indexOf(collectible)
                                    )
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
            }
        }

        binding.btnFavorites.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToFavoritesFragment())
        }

        return binding.root
    }

    override fun onItemClick(position: Int) {
        val collectible = profileCollectibles[position]
        findNavController().navigate(
            ProfileFragmentDirections.actionSearchFragmentToCollectibleDetailsFragment(
                collectible
            )
        )
    }

    override fun onFavoriteClick(position: Int) {
        val favoriteCollectible = FavoriteCollectible(
            profileCollectibles[position].uid, profileCollectibles[position].ownerId
        )
        if(!activity.userInfo.favoriteCollectibles.contains(favoriteCollectible)) {
            activity.userInfo.favoriteCollectibles.add(favoriteCollectible)
            activity.viewModel.updateUserInfo(activity.userInfo)
        }

    }
}