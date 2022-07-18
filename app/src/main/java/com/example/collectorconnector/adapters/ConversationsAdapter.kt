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

class ConversationsAdapter(private val dataSet: ArrayList<Conversation>,
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
            if(adapterPosition != RecyclerView.NO_POSITION) listener.onItemClick(adapterPosition)
        }

    }

    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.conversations_list_item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.screenNameTV.text = dataSet[position].otherUserScreenName
        Glide.with(viewHolder.itemView.context)
            .asBitmap()
            .load(dataSet[position].otherUserProfileImgUrl)
            .circleCrop()
            .into(viewHolder.profileImg)

        viewHolder.lastMessageTV.text = dataSet[position].lastMessage
        viewHolder.timeTV.text = dataSet[position].time.toString()

    }

    override fun getItemCount() = dataSet.size
}