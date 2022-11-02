package com.example.collectorconnector.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.collectorconnector.R
import com.example.collectorconnector.adapters.CollectibleAdapter
import com.example.collectorconnector.databinding.FragmentProfileBinding
import com.example.collectorconnector.models.Collectible

class ProfileFragment : Fragment(), CollectibleAdapter.OnItemClickListener,
    CollectibleAdapter.OnFavoriteClickListener {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        val activity = (requireActivity() as MainActivity)
        activity.binding.bottomNavigationView.menu.getItem(2).isChecked = true
        activity.binding.toolbar.navigationIcon = null
        val isProfileOwner: Boolean
        // if passed a userId, load userInfo for that user
        // otherwise load the userInfo for the logged in user
        if (requireArguments().get("userId") != null) {
            isProfileOwner = true
            viewModel.getUserInfo(requireArguments().get("userId").toString())
            binding.btnFavorites.visibility = View.GONE
        } else {
            isProfileOwner = false
            viewModel.getUserInfo(activity.currentUser!!.uid)
            binding.btnFavorites.visibility = View.VISIBLE
        }
        viewModel.userInfoLiveData.observe(viewLifecycleOwner){
            if(it == null){
                binding.tvNoResults.text = getString(R.string.user_info_network_error)
                binding.tvNoResults.visibility = View.VISIBLE
                return@observe
            }
            else {
                binding.tvNoResults.visibility = View.GONE
            }
            if(it.collectibles.isEmpty()) {
                "${it.screenName} has no collectibles".also { binding.tvNoResults.text = it }
                binding.tvNoResults.visibility = View.VISIBLE
            }
            else {
                binding.tvNoResults.visibility = View.GONE

                val profileCollectiblesAdapter =
                    CollectibleAdapter(
                        it.collectibles,
                        viewModel.userInfo,
                        this,
                        this,
                        false,
                        isProfileOwner
                    )
                binding.collectiblesRecyclerNear.adapter = profileCollectiblesAdapter
            }
        }


        binding.btnFavorites.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToFavoritesFragment())
        }

        return binding.root
    }

    override fun onItemClick(position: Int, collectible: Collectible, isChecked: Boolean) {
        findNavController().navigate(
            ProfileFragmentDirections.actionSearchFragmentToCollectibleDetailsFragment(
                collectible
            )
        )
    }

    override fun onFavoriteClick(position: Int, collectible: Collectible) {
        val activity = requireActivity() as MainActivity

        if(!activity.userInfo.favoriteCollectibles.contains(collectible)) {
            activity.userInfo.favoriteCollectibles.add(collectible)
            activity.viewModel.updateProfile(activity.userInfo, null)
        }
    }
}