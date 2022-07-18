package com.example.collectorconnector.adapters

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collectorconnector.R
import com.example.collectorconnector.models.Collectible
import com.google.android.material.button.MaterialButton

class CollectibleAdapter (private val dataSet: ArrayList<Collectible>,
                          private val listener: OnItemClickListener,
                          private var showCheckboxes: Boolean,
) : RecyclerView.Adapter<CollectibleAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.collectibles_list_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.collIV.setImageBitmap(BitmapFactory.decodeByteArray(dataSet[position].imageByteArray, 0, dataSet[position].imageByteArray!!.size))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        val collIV: ImageView = view.findViewById(R.id.collIV)
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)

        init{
            view.setOnClickListener(this)
            if(showCheckboxes) checkbox.visibility = View.VISIBLE
            else checkbox.visibility = View.GONE
        }

        override fun onClick(v: View?) {
            if(adapterPosition != RecyclerView.NO_POSITION) listener.onItemClick(adapterPosition)
            checkbox.performClick()
        }
    }

    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }

    override fun getItemCount() = dataSet.size
}