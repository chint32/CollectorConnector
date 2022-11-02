package com.example.collectorconnector.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.collectorconnector.R
import com.example.collectorconnector.models.Conversation
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.storage.StorageReference

class ConversationsAdapter(private var dataSet: ArrayList<DocumentSnapshot>,
                           private val listener: OnItemClickListener
) :
    RecyclerView.Adapter<ConversationsAdapter.ViewHolder>() {


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val profileImg: ImageView = view.findViewById(R.id.profileImg)
        val lastMessageTV: TextView = view.findViewById(R.id.last_message)
        val timeTV: TextView = view.findViewById(R.id.tv_time)
        val screenNameTV: TextView = view.findViewById(R.id.tv_screen_name)

        init{
            view.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val conversation = Conversation(
                dataSet[adapterPosition].id,
                dataSet[adapterPosition].get("otherUserScreenName").toString(),
                dataSet[adapterPosition].get("otherUserProfileImgUrl").toString(),
                dataSet[adapterPosition].get("lastMessage").toString(),
                dataSet[adapterPosition].get("time").toString()
            )
            if(adapterPosition != RecyclerView.NO_POSITION) listener.onItemClick(conversation)
        }
    }

    interface OnItemClickListener{
        fun onItemClick(conversation: Conversation)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.conversations_list_item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val conversation = Conversation(
            dataSet[position].id,
            dataSet[position].get("otherUserScreenName").toString(),
            dataSet[position].get("otherUserProfileImgUrl").toString(),
            dataSet[position].get("lastMessage").toString(),
            dataSet[position].get("time").toString()
        )

        viewHolder.screenNameTV.text = conversation.otherUserScreenName
        Glide.with(viewHolder.itemView.context)
            .asBitmap()
            .load(conversation.otherUserProfileImgUrl)
            .circleCrop()
            .into(viewHolder.profileImg)

        viewHolder.lastMessageTV.text = conversation.lastMessage
        viewHolder.timeTV.text = conversation.time

    }

    override fun getItemCount() = dataSet.size

    fun submitList(list: ArrayList<DocumentSnapshot>) {
        dataSet = list
        notifyDataSetChanged()
    }
}