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
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        val activity = (requireActivity() as MainActivity)
        activity.binding.toolbar.navigationIcon = null
        val conversationsAdapter = ConversationsAdapter(arrayListOf(), this)
        binding.conversationsRecycler.adapter = conversationsAdapter
        viewModel.getConversationsForUser(activity.currentUser!!.uid)
        viewModel.conversationsLiveData.observe(viewLifecycleOwner){
            if(it == null){
                binding.tvNoResults.text = getString(R.string.error_retriveing_conversation)
                binding.tvNoResults.visibility = View.VISIBLE
                return@observe
            }
            else
                binding.tvNoResults.visibility = View.GONE

            if(it.documents.isEmpty()){
                binding.tvNoResults.text = getString(R.string.have_started_conversations)
                binding.tvNoResults.visibility = View.VISIBLE
                return@observe
            }
            else
                binding.tvNoResults.visibility = View.GONE
        }
        return binding.root
    }

    override fun onItemClick(conversation: Conversation) {

        findNavController().navigate(
            ConversationsFragmentDirections.actionConversationsFragmentToMessageFragment(
                conversation.otherUserId
            )
        )
    }
}