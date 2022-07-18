package com.example.collectorconnector.adapters

import android.app.Dialog
import android.graphics.BitmapFactory
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView
import com.example.collectorconnector.R
import com.example.collectorconnector.main.MainViewModel
import com.example.collectorconnector.models.*
import com.example.collectorconnector.util.Constants
import com.google.android.material.button.MaterialButton


class MessageAdapter(
    private val dataSet: ArrayList<Message>,
    private val userId: String,
    private val viewModel: MainViewModel,
    private val tradeDetailsDialog: Dialog
) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.message_cardview)

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
    }

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
                    800,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.START
                )
                lParams.setMargins(36)
                this.layoutParams = lParams
            }
        } else {

            viewHolder.cardView.apply {
                val lParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.END
                )
                lParams.setMargins(36)
                this.layoutParams = lParams
            }
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
        if (dataSet[position].type == Constants.MESSAGE_TYPE_IMAGE) {
            val imageMessage = dataSet[position] as ImageMessage
            viewHolder.imageMessageLayout.visibility = View.VISIBLE
            viewHolder.textMessageLayout.visibility = View.GONE
            viewHolder.tradeMessageLayout.visibility = View.GONE
            viewHolder.messageIV.setImageBitmap(
                BitmapFactory.decodeByteArray(
                    imageMessage.image,
                    0,
                    imageMessage.image!!.size
                )
            )
            viewHolder.timeTVimage.text = dataSet[position].time
        }
        if (dataSet[position].type == Constants.MESSAGE_TYPE_TRADE) {
            val tradeMessage = dataSet[position] as TradeMessage
            viewHolder.imageMessageLayout.visibility = View.GONE
            viewHolder.textMessageLayout.visibility = View.GONE
            viewHolder.tradeMessageLayout.visibility = View.VISIBLE
            viewHolder.timeTVtrade.text = dataSet[position].time

            // trade details dialog is shown when trade message is clicked
            buildTradeDetailDialog()

            val btnNegative = tradeDetailsDialog.findViewById<MaterialButton>(R.id.btn_cancel)
            val btnPositive = tradeDetailsDialog.findViewById<MaterialButton>(R.id.btn_confirm)

            // handle the different cases of trade status (OPEN, CLOSED, ACCEPTED, REJECTED) and set
            // the appropriate actions for positive and negative buttons of trade details dialog
            // OPEN
            if(tradeMessage.tradeStatus == Constants.TRADE_STATUS_OPEN) {
                if(userId == tradeMessage.senderId){
                    viewHolder.tradeTV.text = "You Offered A Trade To\n${tradeMessage.recipientScreenName}"
                    btnNegative.text = "Cancel Trade"
                    btnNegative.setOnClickListener {
                        // cancel the trade
                        tradeMessage.tradeStatus = Constants.TRADE_STATUS_CANCELED
                        viewModel.updateTradeStatus(userId, tradeMessage.recipientId,  tradeMessage)
                    }
                    btnPositive.text = "Ok"
                    btnPositive.setOnClickListener {
                        tradeDetailsDialog.dismiss()
                    }
                } else {
                    viewHolder.tradeTV.text = "${tradeMessage.senderScreenName}\nHas Offered You A Trade"
                    btnNegative.text = "Reject Trade"
                    btnNegative.setOnClickListener {
                        // reject the trade
                        tradeMessage.tradeStatus = Constants.TRADE_STATUS_REJECTED
                        viewModel.updateTradeStatus(tradeMessage.senderId, tradeMessage.recipientId, tradeMessage)
                    }
                    btnPositive.text = "Accept Trade"
                    btnPositive.setOnClickListener {
                        tradeMessage.tradeStatus =Constants.TRADE_STATUS_ACCEPTED
                        viewModel.updateTradeStatus(tradeMessage.senderId, tradeMessage.recipientId, tradeMessage)
                        for(collectible in tradeMessage.trade!!.senderCollectibles)
                            viewModel.deleteCollectible(tradeMessage.senderId, collectible.uid)
                        for(collectible in tradeMessage.trade.receiverCollectibles)
                            viewModel.deleteCollectible(tradeMessage.recipientId, collectible.uid)

                    }
                }
            }

            // CANCELED
            else if(tradeMessage.tradeStatus == Constants.TRADE_STATUS_CANCELED) {
                btnPositive.isEnabled = false
                if(userId == tradeMessage.senderId){
                    viewHolder.tradeTV.text = "You Canceled This Trade"
                }else {
                    viewHolder.tradeTV.text = "${tradeMessage.senderScreenName}\nHas Canceled This Trade"
                }
                btnNegative.setOnClickListener{
                    tradeDetailsDialog.dismiss()
                }
            }

            // REJECTED
            else if(tradeMessage.tradeStatus == Constants.TRADE_STATUS_REJECTED) {
                btnPositive.isEnabled = false
                if(userId == tradeMessage.senderId){
                    viewHolder.tradeTV.text = "${tradeMessage.recipientScreenName}\nHas Rejected This Trade"
                }else {
                    viewHolder.tradeTV.text = "You Rejected This Trade"
                }
                btnNegative.setOnClickListener{
                    tradeDetailsDialog.dismiss()
                }
            }

            // ACCEPTED
            else if(tradeMessage.tradeStatus == Constants.TRADE_STATUS_ACCEPTED) {
                btnPositive.isEnabled = false
                if(userId == tradeMessage.senderId){
                    viewHolder.tradeTV.text = "${tradeMessage.recipientScreenName}\nHas Accepted This Trade"
                }else {
                    viewHolder.tradeTV.text = "You Accepted This Trade"
                }
                btnNegative.setOnClickListener{
                    tradeDetailsDialog.dismiss()
                }
            }

            // when user clicks on a trade message, get the collectible images for the trade
            // and show the trade details dialog
            viewHolder.tradeMessageLayout.setOnClickListener {

                println("Trade clicked on: $tradeMessage")
                viewModel.getImagesForTradeSender(
                    tradeMessage.senderId, tradeMessage.recipientId, tradeMessage.messageId
                )
                viewModel.getImagesForTradeReceiver(
                    tradeMessage.senderId, tradeMessage.recipientId, tradeMessage.messageId
                )

                tradeDetailsDialog.show()
            }
        }
    }

    override fun getItemCount() = dataSet.size

    private fun buildTradeDetailDialog(){
        tradeDetailsDialog.setContentView(R.layout.dialog_trade_offer)
        tradeDetailsDialog.findViewById<RecyclerView>(R.id.trade_collectibles_recycler).visibility =
            View.GONE
        tradeDetailsDialog.findViewById<LinearLayout>(R.id.final_trade_offer_layout).visibility =
            View.VISIBLE
        tradeDetailsDialog.setCancelable(false)
        if (tradeDetailsDialog.getWindow() != null)
            tradeDetailsDialog.getWindow()!!.setLayout(900, LinearLayout.LayoutParams.WRAP_CONTENT)
        tradeDetailsDialog.findViewById<TextView>(R.id.textView5).text = "Trade Offer"
    }
}