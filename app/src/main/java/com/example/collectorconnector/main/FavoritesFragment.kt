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
import com.example.collectorconnector.databinding.FragmentFavoritesBinding
import com.example.collectorconnector.models.Collectible

class FavoritesFragment : Fragment(), CollectibleAdapter.OnItemClickListener,
    CollectibleAdapter.OnFavoriteClickListener {

    private lateinit var binding: FragmentFavoritesBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_favorites, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        val activity = (requireActivity() as MainActivity)
        viewModel.userInfo = activity.userInfo
        val collectiblesAdapter =
            CollectibleAdapter(arrayListOf(), viewModel.userInfo, this, this, false, true)
        binding.collectiblesRecycler.adapter = collectiblesAdapter

        return binding.root
    }

    override fun onItemClick(position: Int, collectible: Collectible, isChecked: Boolean) {
        findNavController().navigate(
            FavoritesFragmentDirections.actionFavoritesFragmentToCollectibleDetailsFragment(
                collectible
            )
        )
    }

    override fun onFavoriteClick(position: Int, collectible: Collectible) {
        val activity = (requireActivity() as MainActivity)
        val adapter = binding.collectiblesRecycler.adapter as CollectibleAdapter
        activity.userInfo.favoriteCollectibles.remove(collectible)
        adapter.notifyItemRemoved(position)
        viewModel.updateProfile(activity.userInfo, null)
    }
}