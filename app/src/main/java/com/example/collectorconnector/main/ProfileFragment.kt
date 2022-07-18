package com.example.collectorconnector.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.collectorconnector.BlurTransformation
import com.example.collectorconnector.R
import com.example.collectorconnector.adapters.CollectibleAdapter
import com.example.collectorconnector.databinding.FragmentProfileBinding
import com.example.collectorconnector.models.Collectible
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.util.Constants.ONE_HUNDRED_MEGABYTE

class ProfileFragment : Fragment(), CollectibleAdapter.OnItemClickListener {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var activity: MainActivity
    private val viewModel: MainViewModel by viewModels()

    private val profileCollectibles = ArrayList<Collectible>()
    private val profileCollectiblesAdapter = CollectibleAdapter(profileCollectibles, this, false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        activity = requireActivity() as MainActivity
        binding.collectiblesRecycler.adapter = profileCollectiblesAdapter
        binding.collectiblesRecycler.layoutManager = GridLayoutManager(requireContext(), 2)

        viewModel.userInfoLiveData.observe(viewLifecycleOwner) {
            if (it == null) {
                Toast.makeText(requireContext(), "Error retrieving profile pic", Toast.LENGTH_SHORT)
                    .show()
                return@observe
            }
            val userInfo = it.toObject(UserInfo::class.java)

            println("Image url = ${userInfo!!.profileImgUrl}")

            Glide.with(requireContext())
                .asBitmap()
                .load(userInfo!!.profileImgUrl)
                .circleCrop()
                .into(binding.profilePic)

            Glide.with(requireContext())
                .asBitmap()
                .load(userInfo.profileImgUrl) // or url
                .transform(BlurTransformation(requireContext()))
                .into(binding.ivBlurProfile)

        }

        viewModel.thisUsersCollectiblesLiveData.observe(viewLifecycleOwner) {
            if (it == null) {
                Toast.makeText(requireContext(), "Error retrieving user collectibles", Toast.LENGTH_SHORT)
                    .show()
                return@observe
            }

            // cycle through collectibles for all users
            for (item in it.items) {
                item.metadata.addOnSuccessListener { metadata ->
                    item.getBytes(ONE_HUNDRED_MEGABYTE)
                        .addOnSuccessListener { byteArray ->

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
                                tags,
                                metadata.getCustomMetadata("ownerId").toString()
                            )
                            if (!profileCollectibles.contains(collectible)) {
                                profileCollectibles.add(collectible)
                                profileCollectiblesAdapter.notifyItemInserted(profileCollectibles.indexOf(collectible))
                            }
                        }.addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "Error: " + it.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
        }

        if (activity.userInfo.rating > 4 && activity.userInfo.totalRates > 30) {
            binding.imageView3.setBackgroundResource(R.drawable.gold_badge)
            binding.tvRank.text = "Your Rank: Gold"
            binding.tvPointsNextRank.text = "Super Collector"
            binding.imageView3.visibility = View.VISIBLE
        } else if (activity.userInfo.rating > 3.5 && activity.userInfo.totalRates > 10) {
            binding.imageView3.setBackgroundResource(R.drawable.silver_badge)
            binding.tvRank.text = "Your Rank: Silver"
            binding.tvPointsNextRank.text = "Collector Master"
            binding.imageView3.visibility = View.VISIBLE
        } else if (activity.userInfo.rating > 3 && activity.userInfo.totalRates > 2) {
            binding.imageView3.setBackgroundResource(R.drawable.bronze_badge)
            binding.tvRank.text = "Your Rank: Bronze"
            binding.tvPointsNextRank.text = "Collector in Training"
            binding.imageView3.visibility = View.VISIBLE
        } else {
            binding.tvRank.text = "Your Rank: None"
            binding.tvPointsNextRank.text = "New Collector"
            binding.imageView3.visibility = View.GONE
        }

        if(requireArguments().get("userId") != null) {
            viewModel.getUserInfo(requireArguments().get("userId").toString())
            viewModel.getThisUsersCollectibles(requireArguments().get("userId").toString())
        } else {
            viewModel.getUserInfo(activity.currentUser!!.uid)
            viewModel.getThisUsersCollectibles(activity.currentUser!!.uid)
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
}