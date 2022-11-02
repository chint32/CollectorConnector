package com.example.collectorconnector.adapters

import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.collectorconnector.BlurTransformation
import com.example.collectorconnector.R
import com.example.collectorconnector.main.MainViewModel
import com.example.collectorconnector.models.Collectible
import com.example.collectorconnector.models.Message
import com.example.collectorconnector.models.UserInfo
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

@BindingAdapter(value = ["submitCollectibles"])
fun submitCollectibles(recyclerView: RecyclerView, list: ArrayList<Collectible>?) {
    if(list == null) {
        return
    }

    if(recyclerView.adapter == null) return
    val myAdapter = recyclerView.adapter as CollectibleAdapter
    myAdapter.submitList(list)
}


@BindingAdapter(value = ["submitMessageList"])
fun submitMessageList(recyclerView: RecyclerView, list: ArrayList<Message>?) {
    if(list == null) {
        return
    }
    if(recyclerView.adapter == null) return
    val myAdapter = recyclerView.adapter as MessageAdapter
    myAdapter.submitList(list)
}


@BindingAdapter(value = ["loadScreenName"])
fun loadScreenName(tvScreenName: TextView, userInfo: UserInfo?){
    if(userInfo == null) tvScreenName.text = tvScreenName.context.getString(R.string.error)
    else tvScreenName.text = userInfo.screenName
}

@BindingAdapter(value = ["loadProfileImage"])
fun loadProfileImage( imageView: ImageView, userInfo: UserInfo?){
    if(userInfo == null) {
        // profile image
        Glide.with(imageView)
            .asBitmap()
            .load(R.drawable.ic_baseline_cloud_off_24)
            .circleCrop()
            .into(imageView)
    }
    else {
        // profile image
        Glide.with(imageView)
            .asBitmap()
            .load(userInfo.profileImgUrl)
            .circleCrop()
            .into(imageView)
    }
}

@BindingAdapter(value = ["blurProfileImage"])
fun blurProfileImage(imageViewBlur: ImageView, userInfo: UserInfo?){

    if(userInfo == null) {
        // blurred profile image behind
        Glide.with(imageViewBlur)
            .asBitmap()
            .load(R.drawable.ic_baseline_cloud_off_24)
            .transform(BlurTransformation(imageViewBlur.context))
            .into(imageViewBlur)
    }
    else {
        // blurred profile image behind
        Glide.with(imageViewBlur)
            .asBitmap()
            .load(userInfo.profileImgUrl)
            .transform(BlurTransformation(imageViewBlur.context))
            .into(imageViewBlur)

    }
}

@BindingAdapter(value = ["loadRating"])
fun loadRating(ratingbar: RatingBar, userInfo: UserInfo?){
    if(userInfo == null){
        ratingbar.rating = 0f
    } else {
        ratingbar.rating = userInfo.rating
    }
}

@BindingAdapter(value = ["loadTotalRates"])
fun loadTotalRates(tvTotalRates: TextView, userInfo: UserInfo?){
    if(userInfo == null)
        tvTotalRates.text = tvTotalRates.context.getString(R.string.error)

     else {
        ("(${userInfo.totalRates})").also { tvTotalRates.text = it }
    }

}

//   <-------------- conversations fragment ------------------->

@BindingAdapter(value = ["submitConversations"])
fun submitConversations(recyclerView: RecyclerView, result: QuerySnapshot?){
    if(result == null){
        return
    }
    val myAdapter = recyclerView.adapter as ConversationsAdapter
    myAdapter.submitList(result.documents as ArrayList<DocumentSnapshot>)

}

//   <-------------- collectible details fragment ------------------->


@BindingAdapter(value = ["setCollectibleImage"])
fun setCollectibleImage( imageView: ImageView, viewModel: MainViewModel){
    Glide.with(imageView)
        .asBitmap()
        .load(viewModel.collectible.imageUrl)
        .listener(object : RequestListener<Bitmap?> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Bitmap?>?,
                isFirstResource: Boolean
            ): Boolean {
                val errorCode = e!!.rootCauses.get(0).message!!.substringAfter("status code: ")
                if(errorCode == imageView.context.getString(R.string.http404)){
                    viewModel.collectibleDeletedLiveData.value = true
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
        .into(imageView)
}







