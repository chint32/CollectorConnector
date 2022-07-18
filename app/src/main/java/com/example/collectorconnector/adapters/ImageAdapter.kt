package com.example.collectorconnector.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.collectorconnector.R

class ImageAdapter(private val imageList: ArrayList<ByteArray>, private val showCheckBoxes: Boolean) :
    RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.collectibles_list_item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.collIV.setImageBitmap(BitmapFactory.decodeByteArray(imageList[position], 0, imageList[position].size))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view){

        val collIV: ImageView = view.findViewById(R.id.collIV)
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)

        init{
            if(showCheckBoxes) checkbox.visibility = View.VISIBLE
            else checkbox.visibility = View.GONE
        }

    }

    override fun getItemCount() = imageList.size
}