package com.example.collectorconnector.main

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.example.collectorconnector.R
import com.example.collectorconnector.databinding.FragmentCollectibleDetailsBinding
import com.example.collectorconnector.models.Collectible


class CollectibleDetailsFragment : Fragment() {

    private lateinit var binding: FragmentCollectibleDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_collectible_details, container, false)

        val collectible = requireArguments().get("collectible") as Collectible
        loadDataIntoViews(collectible)

        binding.btnMessageOwner.setOnClickListener{
            (requireActivity() as MainActivity).binding.bottomNavigationView.selectedItemId
           findNavController().navigate(CollectibleDetailsFragmentDirections.actionCollectibleDetailsFragmentToMessageFragment(collectible.ownerId))
        }

        binding.btnOwnerProfile.setOnClickListener {
            findNavController().navigate(CollectibleDetailsFragmentDirections.actionCollectibleDetailsFragmentToProfileFragment(collectible.ownerId))
        }

        return binding.root
    }

    private fun loadDataIntoViews(collectible: Collectible){
        binding.collectibleImg.setImageBitmap(BitmapFactory.decodeByteArray(collectible.imageByteArray, 0, collectible.imageByteArray!!.size))
        binding.tvCollectibleName.text = collectible.name
        binding.tvCollectibleDesc.text = collectible.description
        binding.tvCollectibleCond.text = collectible.condition
        binding.tvCollectibleTags.text = collectible.tags.toString()
    }
}