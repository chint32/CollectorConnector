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
import com.example.collectorconnector.adapters.ConversationsAdapter
import com.example.collectorconnector.databinding.FragmentConversationsBinding
import com.example.collectorconnector.models.Conversation

class ConversationsFragment : Fragment(), ConversationsAdapter.OnItemClickListener {

    private lateinit var binding: FragmentConversationsBinding

    val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_conversations, container, false)
        val activity = (requireActivity() as MainActivity)
        activity.binding.toolbar.navigationIcon = null
        binding.conversationsRecycler.adapter = activity.conversationsAdapter
        if (activity.conversations.isEmpty()){
            binding.tvNoResults.visibility = View.VISIBLE
        }


        return binding.root
    }

    override fun onItemClick(position: Int) {


    }

}