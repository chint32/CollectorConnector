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
import com.example.collectorconnector.models.UserInfo

class ConversationsFragment : Fragment(), ConversationsAdapter.OnItemClickListener {

    private lateinit var binding: FragmentConversationsBinding
    val conversations = ArrayList<Conversation>()
    val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_conversations, container, false)
        val activity = (requireActivity() as MainActivity)
        val conversationsAdapter = ConversationsAdapter(conversations, this)
        binding.conversationsRecycler.adapter = conversationsAdapter

        // observe conversations and load into recyclerview
        viewModel.conversationsLiveData.observe(viewLifecycleOwner) {
            if (it == null) {
                Toast.makeText(requireContext(), "Error getting conversations", Toast.LENGTH_SHORT)
                    .show()
                return@observe
            }


            for (doc in it.documents) {
                viewModel.userInfoLiveData.removeObservers(viewLifecycleOwner)
                viewModel.userInfoLiveData.observe(viewLifecycleOwner) { value ->
                    if (value == null) {
                        Toast.makeText(
                            requireContext(),
                            "Error getting other user's info",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@observe
                    }
                    val otherUserInfo = value.toObject(UserInfo::class.java)
                    val conversation = Conversation(
                        doc.id,
                        otherUserInfo!!.screenName,
                        otherUserInfo.profileImgUrl,
                        doc.get("lastMessage").toString(),
                        doc.get("time").toString()
                    )

                    conversations.add(conversation)
                    conversationsAdapter.notifyItemInserted(
                        conversations.indexOf(
                            conversation
                        )
                    )
                }
                viewModel.getUserInfo(doc.id)
            }
        }

        // call to retrieve conversations for current user
        viewModel.getConversationsForUser(activity.currentUser!!.uid)

        return binding.root
    }

    override fun onItemClick(position: Int) {
        val conversation = conversations[position]
        findNavController().navigate(
            ConversationsFragmentDirections.actionConversationsFragmentToMessageFragment(
                conversation.otherUserId
            )
        )
    }

}