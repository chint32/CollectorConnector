package com.example.collectorconnector.adapters

import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable.ConstantState
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.collectorconnector.R
import com.example.collectorconnector.models.Collectible
import com.example.collectorconnector.models.FavoriteCollectible
import com.example.collectorconnector.models.UserInfo


class CollectibleAdapter (private val dataSet: ArrayList<Collectible>,
                          private val userInfo: UserInfo?,
                          private val listener: OnItemClickListener,
                          private val favoriteListener: OnFavoriteClickListener,
                          private var showCheckboxes: Boolean,
                          private var showFavorite: Boolean
) : RecyclerView.Adapter<CollectibleAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.collectibles_list_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.collIV.setImageBitmap(BitmapFactory.decodeByteArray(dataSet[position].imageByteArray, 0, dataSet[position].imageByteArray!!.size))

        if(userInfo != null) {
            val favCollectible = FavoriteCollectible(dataSet[position].uid, dataSet[position].ownerId)
            if (userInfo.favoriteCollectibles.contains(favCollectible)) {
                viewHolder.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_24)
                viewHolder.ivFavorite.tag = "fav"
            }
            else {
                viewHolder.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_border_24)
                viewHolder.ivFavorite.tag = "not fav"
            }
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        val collIV: ImageView = view.findViewById(R.id.collIV)
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)
        val ivFavorite: ImageView = view.findViewById(R.id.iv_favorite)

        init{

            if(showCheckboxes) checkbox.visibility = View.VISIBLE
            else checkbox.visibility = View.GONE

            if(showFavorite) ivFavorite.visibility = View.VISIBLE
            else ivFavorite.visibility = View.GONE

            collIV.setOnClickListener(this)
            ivFavorite.setOnClickListener(this)
            checkbox.setOnClickListener(this)

        }

        override fun onClick(v: View?) {
            if(v!!.id == R.id.iv_favorite){
                favoriteListener.onFavoriteClick(adapterPosition)
                if(ivFavorite.tag == "not fav") {
                    ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_24)
                    ivFavorite.tag = "fav"
                }
                else  {
                    ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_border_24)
                    ivFavorite.tag = "not fav"
                }
            } else if(v.id == R.id.collIV){
                checkbox.performClick()
            }
            else if(v.id == R.id.checkbox){
                if (adapterPosition != RecyclerView.NO_POSITION) listener.onItemClick(
                    adapterPosition
                )
            }
        }

    }

    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }

    interface OnFavoriteClickListener{
        fun onFavoriteClick(position: Int)
    }

    override fun getItemCount() = dataSet.size
}