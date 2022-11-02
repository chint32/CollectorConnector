package com.example.collectorconnector.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.collectorconnector.R
import com.example.collectorconnector.models.*
import com.example.collectorconnector.util.Constants


class MessageAdapter(
    private var dataSet: ArrayList<Message>,
    private val userId: String,
    private val listener: OnItemClickListener
) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.message_list_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // if current user is the sender of this message, align message to RHS
        // otherwise, current user is receiver. So, align message to LHS
        if (dataSet[position].senderId != userId) {
            viewHolder.cardView.apply {
                val lParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.START
                )
                this.layoutParams = lParams
            }
            val constraintLayout: ConstraintLayout = viewHolder.imageMessageLayout
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            constraintSet.connect(
                viewHolder.timeTVimage.id,
                ConstraintSet.LEFT,
                viewHolder.messageIV.id,
                ConstraintSet.LEFT,
                0
            )
            constraintSet.applyTo(constraintLayout)


            viewHolder.messageTV.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            viewHolder.timeTVtext.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            viewHolder.timeTVtrade.textAlignment = View.TEXT_ALIGNMENT_VIEW_START

        } else {
            viewHolder.cardView.apply {
                val lParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.END
                )
                this.layoutParams = lParams

            }
            val constraintLayout: ConstraintLayout = viewHolder.imageMessageLayout
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            constraintSet.connect(
                viewHolder.timeTVimage.id,
                ConstraintSet.RIGHT,
                viewHolder.messageIV.id,
                ConstraintSet.RIGHT,
                0
            )
            constraintSet.applyTo(constraintLayout)
            viewHolder.messageTV.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            viewHolder.timeTVtext.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            viewHolder.timeTVtrade.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
        }

        // set the message layout according to the message type
        if (dataSet[position].type == Constants.MESSAGE_TYPE_TEXT) {
            val textMessage = dataSet[position] as TextMessage
            viewHolder.imageMessageLayout.visibility = View.GONE
            viewHolder.tradeMessageLayout.visibility = View.GONE
            viewHolder.textMessageLayout.visibility = View.VISIBLE
            viewHolder.messageTV.text = textMessage.text
            viewHolder.timeTVtext.text = dataSet[position].time
        }
        else if (dataSet[position].type == Constants.MESSAGE_TYPE_IMAGE) {
            val imageMessage = dataSet[position] as ImageMessage
            viewHolder.imageMessageLayout.visibility = View.VISIBLE
            viewHolder.textMessageLayout.visibility = View.GONE
            viewHolder.tradeMessageLayout.visibility = View.GONE
            Glide.with(viewHolder.itemView.context)
                .load(imageMessage.imageUrl)
                .into(viewHolder.messageIV)
            viewHolder.timeTVimage.text = imageMessage.time
        }
        else if (dataSet[position].type == Constants.MESSAGE_TYPE_TRADE) {
            val tradeMessage = dataSet[position] as TradeMessage
            viewHolder.imageMessageLayout.visibility = View.GONE
            viewHolder.textMessageLayout.visibility = View.GONE
            viewHolder.tradeMessageLayout.visibility = View.VISIBLE
            viewHolder.timeTVtrade.text = dataSet[position].time
            if (tradeMessage.tradeStatus == Constants.TRADE_STATUS_OPEN) {
                if (userId == tradeMessage.senderId)
                    "You Offered A Trade To\n${tradeMessage.recipientScreenName}".also { viewHolder.tradeTV.text = it }
                else
                    "${tradeMessage.senderScreenName}\nHas Offered You A Trade".also { viewHolder.tradeTV.text = it }
            }
            else if (tradeMessage.tradeStatus == Constants.TRADE_STATUS_CANCELED) {
                if (userId == tradeMessage.senderId)
                    viewHolder.tradeTV.text = viewHolder.itemView.context.getString(R.string.you_canceled_trade)
                else
                        "${tradeMessage.senderScreenName}\nHas Canceled This Trade".also { viewHolder.tradeTV.text = it}

            }
            else if (tradeMessage.tradeStatus == Constants.TRADE_STATUS_REJECTED) {
                if (userId == tradeMessage.senderId) {
                    "${tradeMessage.recipientScreenName}\nHas Rejected This Trade".also { viewHolder.tradeTV.text = it }
                } else viewHolder.tradeTV.text = viewHolder.itemView.context.getString(R.string.you_rejected_trade)

            }
            else if (tradeMessage.tradeStatus == Constants.TRADE_STATUS_ACCEPTED) {
                if (userId == tradeMessage.senderId) {
                    "${tradeMessage.recipientScreenName}\nHas Accepted This Trade".also { viewHolder.tradeTV.text = it }
                } else {
                    viewHolder.tradeTV.text = viewHolder.itemView.context.getString(R.string.accepted_trade)
                }
            }
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener{

        val cardView: FrameLayout = view.findViewById(R.id.message_cardview)

        //text
        val textMessageLayout: ConstraintLayout = view.findViewById(R.id.text_message_layout)
        val messageTV: TextView = view.findViewById(R.id.tv_message)
        val timeTVtext: TextView = view.findViewById(R.id.tv_time_text)

        //image
        val imageMessageLayout: ConstraintLayout = view.findViewById(R.id.image_message_layout)
        val messageIV: ImageView = view.findViewById(R.id.image_message)
        val timeTVimage: TextView = view.findViewById(R.id.tv_time_image)

        //trade
        val tradeMessageLayout: ConstraintLayout = view.findViewById(R.id.trade_message_layout)
        val tradeTV: TextView = view.findViewById(R.id.tv_trade_offered)
        val timeTVtrade: TextView = view.findViewById(R.id.tv_time_trade)

        init {
            tradeMessageLayout.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            if (adapterPosition != RecyclerView.NO_POSITION && dataSet[adapterPosition].type == Constants.MESSAGE_TYPE_TRADE) {

                val tradeMessage = dataSet[adapterPosition] as TradeMessage
                listener.onTradeMessageItemClick(tradeMessage)
            }
        }
    }

    interface OnItemClickListener{
        fun onTradeMessageItemClick(tradeMessage: TradeMessage)
    }

    override fun getItemCount() = dataSet.size

    fun submitList(data: ArrayList<Message>){
        dataSet = data
        notifyDataSetChanged()
    }
}