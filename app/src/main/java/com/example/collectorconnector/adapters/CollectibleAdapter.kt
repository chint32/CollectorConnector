package com.example.collectorconnector.adapters

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.collectorconnector.R
import com.example.collectorconnector.databinding.CollectiblesListItemBinding
import com.example.collectorconnector.models.Collectible
import com.example.collectorconnector.models.UserInfo


class CollectibleAdapter(
    private var dataSet: ArrayList<Collectible>,
    private val userInfo: UserInfo,
    private val listener: OnItemClickListener,
    private val favoriteListener: OnFavoriteClickListener,
    private var showCheckboxes: Boolean,
    private var showFavorite: Boolean
) : RecyclerView.Adapter<CollectibleAdapter.ViewHolder>() {


    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        val binding = DataBindingUtil.inflate<CollectiblesListItemBinding>(
            layoutInflater,
            R.layout.collectibles_list_item,
            viewGroup,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {


        if (userInfo.favoriteCollectibles.contains(dataSet[position])) {
            viewHolder.binding.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_24)
            viewHolder.binding.ivFavorite.tag = "fav"
        } else {
            viewHolder.binding.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_border_24)
            viewHolder.binding.ivFavorite.tag = "not fav"
        }


        val circularProgressDrawable = CircularProgressDrawable(viewHolder.itemView.context)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.start()

        Glide.with(viewHolder.binding.collIV)
            .asBitmap()
            .load(dataSet[position].imageUrl)
            .placeholder(circularProgressDrawable)
            .listener(object : RequestListener<Bitmap?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    val errorCode =
                        e!!.rootCauses.get(0).message!!.substringAfter(
                            viewHolder.itemView.context.getString(
                                R.string.status_code
                            )
                        )
                    if (errorCode == viewHolder.itemView.context.getString(R.string.http404)) {
                        ("- Item Deleted -\nName: ${dataSet[position].name}\n\n" +
                                "You can still click here to see it's details, but the image is gone."
                                ).also { viewHolder.binding.tvItemDeleted.text = it }
                        viewHolder.binding.tvItemDeleted.visibility = View.VISIBLE
                        viewHolder.binding.collIV.visibility = View.GONE
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .fitCenter()
            .into(viewHolder.binding.collIV)


    }

    inner class ViewHolder(val binding: CollectiblesListItemBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            if (showCheckboxes)
                binding.checkbox.visibility = View.VISIBLE
            else binding.checkbox.visibility = View.GONE

            if (showFavorite) binding.ivFavorite.visibility = View.VISIBLE
            else binding.ivFavorite.visibility = View.GONE

            binding.collIV.setOnClickListener(this)
            binding.ivFavorite.setOnClickListener(this)
            binding.checkbox.setOnClickListener(this)
            binding.tvItemDeleted.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            if (v!!.id == R.id.iv_favorite) {
                favoriteListener.onFavoriteClick(adapterPosition, dataSet[adapterPosition])
                if (binding.ivFavorite.tag == "not fav") {
                    binding.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_24)
                    binding.ivFavorite.tag = "fav"
                } else {
                    binding.ivFavorite.setImageResource(R.drawable.ic_baseline_favorite_border_24)
                    binding.ivFavorite.tag = "not fav"
                }
            } else if (v.id == R.id.collIV || v.id == R.id.tv_item_deleted) {
                binding.checkbox.performClick()
            } else if (v.id == R.id.checkbox) {
                if (adapterPosition != RecyclerView.NO_POSITION) listener.onItemClick(
                    adapterPosition, dataSet[adapterPosition], binding.checkbox.isChecked
                )
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int, collectible: Collectible, isChecked: Boolean)
    }

    interface OnFavoriteClickListener {
        fun onFavoriteClick(position: Int, collectible: Collectible)
    }

    override fun getItemCount() = dataSet.size

    fun submitList(list: ArrayList<Collectible>) {
        dataSet = list
        notifyDataSetChanged()
    }
}