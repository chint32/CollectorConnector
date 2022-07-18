package com.example.collectorconnector.main

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collectorconnector.R
import com.example.collectorconnector.adapters.CollectibleAdapter
import com.example.collectorconnector.adapters.ImageAdapter
import com.example.collectorconnector.adapters.MessageAdapter
import com.example.collectorconnector.databinding.FragmentMessageBinding
import com.example.collectorconnector.models.*
import com.example.collectorconnector.util.Constants
import com.example.collectorconnector.util.Constants.ONE_HUNDRED_MEGABYTE
import com.example.collectorconnector.util.Constants.PICK_IMAGE_REQUEST
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*


class MessageFragment : MyFragment(), CollectibleAdapter.OnItemClickListener {

    private lateinit var binding: FragmentMessageBinding
    private lateinit var activity: MainActivity
    val viewModel:MainViewModel by viewModels()

    //in message fragment, current user is messaging other user.
    //this is id of other user
    private lateinit var otherUserId: String
    private lateinit var otherUserInfo: UserInfo

    //messages that will be displayed in recyclerview
    val messages = ArrayList<Message>()
    private lateinit var adapter: MessageAdapter

    // Uri indicates, where the image will be picked from
    private var filePath: Uri? = null

    //booleans to determine which layout for trade dialog as well as which users
    //collectibles to display
    private var viewingThisUsersCollectibles = true
    private var readyToOfferTrade = false

    //all collectibles in the trade
    val collectiblesForTrade = ArrayList<Collectible>()

    //collectibles separated by sender/receiver
    private val thisUsersSelectedCollectiblesForTrade = ArrayList<Collectible>()
    private val otherUsersSelectedCollectiblesForTrade = ArrayList<Collectible>()

    //dialog that will be used to facilitate trade offer
    //(picking trade collectibles, confirming trade)
    private lateinit var dialog: Dialog

    //collectibles shown in the trade details dialog after a trade has been offered
    private val tradeDetailsSenderCollectibleImages = ArrayList<ByteArray>()
    private val tradeDetailsReceiverCollectibleImages = ArrayList<ByteArray>()

    //after a successful trade, user will rate the other party involved with the trade
    private var ratingStarsGiven = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_message, container, false)

        activity = (requireActivity() as MainActivity)

        //get the other users id passed in to messagesFragment as argument
        otherUserId = requireArguments().get("otherUserId").toString()
        viewModel.userInfoLiveData.observe(viewLifecycleOwner){
            if(it == null || it.data.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Error getting other user's info", Toast.LENGTH_SHORT).show()
                return@observe
            }
            otherUserInfo = it.toObject(UserInfo::class.java) as UserInfo
            println(otherUserInfo)
        }
        viewModel.getUserInfo(otherUserId)

        //initialize dialog used to facilitate trade
        dialog = Dialog(requireContext())

        adapter = MessageAdapter(
            messages,
            activity.currentUser!!.uid,
            activity.viewModel,
            dialog
        )
        binding.messagesRecycler.adapter = adapter

        // dialog that will be used to facilitate rating users
        val userRatingDialog = Dialog(requireContext())

        //viewModel still holds messages from previous convo so need to clear out those messages
        //activity.viewModel.clearMessagesWhenSwitchingConversations()

        //observe messages and handle each type of message accordingly (TEXT, IMAGE, TRADE)
        viewModel.messagesLiveData.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer {
                if (it == null){
                    return@Observer
                }
                for (doc in it) {
                    val type = doc!!.get("type").toString()
                    if (type == Constants.MESSAGE_TYPE_TEXT) {
                        val textMessage = doc.toObject(TextMessage::class.java)!!
                        if (!messages.contains(textMessage)) {
                            messages.add(textMessage)
                            adapter.notifyItemInserted(messages.indexOf(textMessage))
                        }
                    } else if (type == Constants.MESSAGE_TYPE_IMAGE) {
                            // message of type IMAGE retrieved from firestore
                            // need to get the actual image from firebase storage
                        val imageMessage = doc.toObject(ImageMessage::class.java)!!

                        //observe images for message coming from firebase storage
                        viewModel.imageMessagesLiveData.observe(
                            viewLifecycleOwner
                        ) { byteArray ->
                            if (byteArray == null) {
                                Toast.makeText(requireContext(), "Error getting images for image messages", Toast.LENGTH_SHORT).show()
                                return@observe
                            }

                            imageMessage.image = byteArray
                            if (!messages.contains(imageMessage)) {
                                messages.add(imageMessage)
                                adapter.notifyItemInserted(messages.indexOf(imageMessage))
                            }
                        }
                        //make call to retrieve image from firebase storage
                        viewModel.getImageFromImageMessage(
                            activity.currentUser!!.uid,
                            otherUserId,
                            imageMessage
                        )
                    } else if (type == Constants.MESSAGE_TYPE_TRADE) {
                        //message of type IMAGE retrieved from firestore.
                        //need to get the actual image from firebase storage
                            // this is handled in the message adapter class
                        val tradeMessage = doc.toObject(TradeMessage::class.java) as TradeMessage
                        if (!messages.contains(tradeMessage)) {
                            messages.add(tradeMessage)
                            adapter.notifyItemInserted(messages.indexOf(tradeMessage))
                        }

                    }
                }
            })

        // make call to listen for and retrieve messages from firebase
        viewModel.listenForMessagesFromOtherUser(activity.currentUser!!.uid, otherUserId)

        //observe updating status of trade (OPEN, CLOSED, ACCEPTED, REJECTED) and if the trade
        //was accepted, inform the current user by showing trade details dialog
        activity.viewModel.isTradeStatusUpdatedLiveData.observe(
            viewLifecycleOwner
        ) { tradeMessage ->
            if (tradeMessage == null) {
                Toast.makeText(
                    requireContext(),
                    "Failed to update trade status",
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
                return@observe
            }
            Toast.makeText(requireContext(), "Trade status updated", Toast.LENGTH_SHORT)
                .show()
            if (tradeMessage.tradeStatus == Constants.TRADE_STATUS_ACCEPTED && !tradeMessage.tradeAcceptanceReceived) {
                dialog.show()
                dialog.findViewById<TextView>(R.id.textView5).text = "Trade Details"
                activity.viewModel.getImagesForTradeSender(
                    tradeMessage.senderId,
                    tradeMessage.recipientId,
                    tradeMessage.messageId
                )
                activity.viewModel.getImagesForTradeReceiver(
                    tradeMessage.senderId,
                    tradeMessage.recipientId,
                    tradeMessage.messageId
                )

                val btnCancelTrade =
                    dialog.findViewById<MaterialButton>(R.id.btn_cancel)
                btnCancelTrade.isEnabled = false
                val btnOk =
                    dialog.findViewById<MaterialButton>(R.id.btn_confirm)
                btnOk.text = "Ok"
                btnOk.setOnClickListener {
                    tradeMessage.tradeAcceptanceReceived = true
                    activity.viewModel.updateTradeAcceptanceReceived(
                        tradeMessage.senderId,
                        tradeMessage.recipientId,
                        tradeMessage
                    )

                    dialog.dismiss()
                    userRatingDialog.setContentView(R.layout.dialog_rate_user)

                    userRatingDialog.setCancelable(false)

                    if (userRatingDialog.getWindow() != null) {
                        userRatingDialog.getWindow()!!.setLayout(
                            900,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    setOnClickListenersRatingStars(userRatingDialog)
                    setOnClickListenerToSubmitUserRating(userRatingDialog)
                    userRatingDialog.show()

                }
            }
        }

        // observe sender of the trade's collectibles to show in trade details dialog
        activity.viewModel.tradeImagesSenderLiveData.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer {
                if (it == null || it.items.isEmpty())  {
                    Toast.makeText(requireContext(), "Error getting trade images for sender", Toast.LENGTH_SHORT).show()
                    return@Observer
                }

                val imageAdapter1 = ImageAdapter(tradeDetailsSenderCollectibleImages, false)
                val rv1 =
                    dialog.findViewById<RecyclerView>(R.id.this_user_collectibles_trade_recycler)
                rv1.adapter = imageAdapter1

                for (item in it.items) {
                    item.getBytes(ONE_HUNDRED_MEGABYTE).addOnSuccessListener { byteArray ->
                        tradeDetailsSenderCollectibleImages.add(byteArray)
                        imageAdapter1.notifyItemInserted(tradeDetailsSenderCollectibleImages.lastIndex)
                    }
                }
            })

        // observe receiver of the trade's collectibles to show in trade details dialog
        activity.viewModel.tradeImagesReceiverLiveData.observe(viewLifecycleOwner) {
            if (it == null || it.items.isEmpty())  {
                Toast.makeText(requireContext(), "Error getting trade images for receiver", Toast.LENGTH_SHORT).show()
                return@observe
            }

            val imageAdapter2 = ImageAdapter(tradeDetailsReceiverCollectibleImages, false)
            val rv2 = dialog.findViewById<RecyclerView>(R.id.other_user_collectibles_trade_recycler)
            rv2.adapter = imageAdapter2

            for (item in it.items) {
                item.getBytes(ONE_HUNDRED_MEGABYTE).addOnSuccessListener { byteArray ->
                    tradeDetailsReceiverCollectibleImages.add(byteArray)
                    imageAdapter2.notifyItemInserted(tradeDetailsReceiverCollectibleImages.lastIndex)
                }
            }
        }


        // progress dialog for uploading image
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setTitle("Sending Trade Offer...")
        //observe whether or not trade message is successfully sent
        activity.viewModel.isTradeMessageSentLiveData.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer {
                if (!it) {
                    Toast.makeText(requireContext(), "Trade failed to send", Toast.LENGTH_SHORT)
                        .show()
                    return@Observer
                }
                Toast.makeText(requireContext(), "Trade sent!", Toast.LENGTH_SHORT).show()
                if (dialog.isShowing) dialog.dismiss()
                progressDialog.dismiss()
            })

        //observe whether or not text message is successfully sent
        activity.viewModel.isTextMessageSentLiveData.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer {
                if (!it) {
                    Toast.makeText(
                        requireContext(),
                        "TextMessage failed to send",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    return@Observer
                }
                binding.etMessageToSend.setText("")
            })

        binding.ivSendTradeIcon.setOnClickListener {

            buildTradeDialog()

            // create and setup recyclerview/adapter to display trade collectibles
            val rv: RecyclerView = dialog.findViewById(R.id.trade_collectibles_recycler)
            rv.layoutManager = GridLayoutManager(requireContext(), 2)
            var adapter = CollectibleAdapter(collectiblesForTrade, this, true)
            rv.adapter = adapter

            dialog.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
                collectiblesForTrade.clear()
                dialog.dismiss()
            }
            dialog.findViewById<MaterialButton>(R.id.btn_confirm).setOnClickListener {
                if (viewingThisUsersCollectibles)
                    setupAndFacilitateUserSelectItemsToGiveForTrade(rv)
                else {
                    if (!readyToOfferTrade)
                        setupAndFacilitateUserSelectItemsToReceiveForTrade(rv)
                    else
                        confirmAndSendTradeOffer(progressDialog)
                }
            }

            //observe this users collectibles and display in recyclerview in trade dialog
            activity.viewModel.thisUsersCollectiblesLiveData.observe(
                viewLifecycleOwner,
                androidx.lifecycle.Observer {
                    if (it == null) {
                        Toast.makeText(requireContext(), "Error getting this user's collectibles", Toast.LENGTH_SHORT).show()
                        return@Observer
                    }

                    // cycle through collectibles for all users
                    for (item in it.items) {
                        item.metadata.addOnSuccessListener { metadata ->
                            item.getBytes(ONE_HUNDRED_MEGABYTE)
                                .addOnSuccessListener { byteArray ->
                                    val tags = ArrayList<String>()
                                    val tagsArr =
                                        metadata.getCustomMetadata("tags").toString()
                                            .split(",")
                                    for (i in tagsArr.indices)
                                        tags.add(tagsArr[i])

                                    val collectible = Collectible(
                                        item.name,
                                        metadata.getCustomMetadata("name").toString(),
                                        metadata.getCustomMetadata("desc").toString(),
                                        metadata.getCustomMetadata("cond").toString(),
                                        byteArray,
                                        tags,
                                        (requireActivity() as MainActivity).currentUser!!.uid
                                    )
                                    if (!collectiblesForTrade.contains(collectible)) {
                                        collectiblesForTrade.add(collectible)
                                        adapter.notifyItemInserted(collectiblesForTrade.lastIndex)
                                    }

                                }.addOnFailureListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "Error: " + it.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                })

            dialog.findViewById<LinearLayout>(R.id.final_trade_offer_layout).visibility = View.GONE
            dialog.show()
        }

        //retrieve this users collectibles to allow him to pick them for trade
        activity.viewModel.getThisUsersCollectibles(activity.currentUser!!.uid)

        binding.ivSendIcon.setOnClickListener {
            if(binding.etMessageToSend.text.toString() == ""){
                Toast.makeText(requireContext(), "Message must not be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendTextMessage()
        }

        binding.ivSendImageIcon.setOnClickListener {
            selectImage()
        }

        return binding.root
    }

    private fun sendTextMessage(){
        val c = Calendar.getInstance()
        val df = SimpleDateFormat("dd-MM-yyyy HH:mm a")
        val formattedDate = df.format(c.time)
        val textMessage = TextMessage(
            UUID.randomUUID().toString(),
            activity.currentUser!!.uid,
            activity.userInfo.screenName,
            otherUserId,
            otherUserInfo.screenName,
            formattedDate,
            "TEXT",
            binding.etMessageToSend.text.toString()
        )

        activity.viewModel.sendTextMessage(activity.currentUser!!.uid, otherUserId, textMessage)
    }

    private fun confirmAndSendTradeOffer(progressDialog: ProgressDialog){

        val c = Calendar.getInstance()
        val df = SimpleDateFormat("dd-MM-yyyy HH:mm a")
        val formattedDate = df.format(c.time)
        val messageId = UUID.randomUUID().toString()
        val tradeMessage = TradeMessage(
            messageId,
            activity.currentUser!!.uid,
            activity.userInfo.screenName,
            otherUserId,
            otherUserInfo.screenName,
            formattedDate,
            "TRADE",
            Trade(
                messageId,
                activity.currentUser!!.uid,
                otherUserId,
                thisUsersSelectedCollectiblesForTrade,
                otherUsersSelectedCollectiblesForTrade
            ),
            "OPEN"
        )
        activity.viewModel.sendTradeMessage(
            activity.currentUser!!.uid,
            otherUserId,
            tradeMessage
        )


        progressDialog.show()

    }

    private fun setupAndFacilitateUserSelectItemsToReceiveForTrade(rv: RecyclerView){

        dialog.findViewById<TextView>(R.id.textView5).text = "Confirm Trade Offer"
        dialog.findViewById<LinearLayout>(R.id.final_trade_offer_layout).visibility =
            View.VISIBLE
        rv.visibility = View.GONE

        val thisUserFinalAdapter = CollectibleAdapter(
            thisUsersSelectedCollectiblesForTrade, this, false
        )
        val thisUsersRv =
            dialog.findViewById<RecyclerView>(R.id.this_user_collectibles_trade_recycler)
        thisUsersRv.adapter = thisUserFinalAdapter


        val otherUserFinalAdapter = CollectibleAdapter(
            otherUsersSelectedCollectiblesForTrade, this, false
        )
        val otherUsersRv =
            dialog.findViewById<RecyclerView>(R.id.other_user_collectibles_trade_recycler)
        otherUsersRv.adapter = otherUserFinalAdapter


        readyToOfferTrade = true
    }

    private fun setupAndFacilitateUserSelectItemsToGiveForTrade(rv: RecyclerView){
        collectiblesForTrade.clear()
        activity.viewModel.getOtherUsersCollectibles(otherUserId)
        activity.viewModel.otherUsersCollectiblesLiveData.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer {
                if (it == null) {
                    Toast.makeText(requireContext(), "Error getting other user's collectibles", Toast.LENGTH_SHORT).show()
                    return@Observer
                }

                // cycle through collectibles for all users
                for (item in it.items) {
                    item.metadata.addOnSuccessListener { metadata ->
                        item.getBytes(ONE_HUNDRED_MEGABYTE)
                            .addOnSuccessListener { byteArray ->
                                val tags = ArrayList<String>()
                                val tagsArr =
                                    metadata.getCustomMetadata("tags").toString()
                                        .split(",")
                                for (i in tagsArr.indices)
                                    tags.add(tagsArr[i])

                                val collectible = Collectible(
                                    item.name,
                                    metadata.getCustomMetadata("name").toString(),
                                    metadata.getCustomMetadata("desc").toString(),
                                    metadata.getCustomMetadata("cond").toString(),
                                    byteArray,
                                    tags,
                                    otherUserId
                                )
                                if (!collectiblesForTrade.contains(collectible)) {
                                    collectiblesForTrade.add(collectible)
                                }

                                rv.adapter = CollectibleAdapter(collectiblesForTrade, this, true)


                            }.addOnFailureListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Error: " + it.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }

            })
        dialog.findViewById<TextView>(R.id.textView5).text =
            "Select Collectibles to Receive"
        viewingThisUsersCollectibles = false
    }

    // Override onActivityResult method
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        // checking request code and result code
        // if request code is PICK_IMAGE_REQUEST and
        // resultCode is RESULT_OK
        // then set image in the image view
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {

            // Get the Uri of data
            filePath = data.data

            val c = Calendar.getInstance()
            val df = SimpleDateFormat("dd-MM-yyyy HH:mm a")
            val formattedDate = df.format(c.time)
            val imageMessage = ImageMessage(
                UUID.randomUUID().toString(),
                activity.currentUser!!.uid,
                activity.userInfo.screenName,
                otherUserId,
                otherUserInfo.screenName,
                formattedDate,
                Constants.MESSAGE_TYPE_IMAGE,
                null,
            )

            activity.viewModel.sendImageMessage(
                activity.currentUser!!.uid,
                otherUserId,
                imageMessage,
                filePath!!
            )
        }
    }

    // Select Image method
    private fun selectImage() {

        // Defining Implicit Intent to mobile gallery
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(
                intent,
                "Select Image from here..."
            ),
            PICK_IMAGE_REQUEST
        )
    }

    private fun buildTradeDialog(){

        dialog.setContentView(R.layout.dialog_trade_offer)
        dialog.setCancelable(false)

        if (dialog.getWindow() != null) {
            dialog.getWindow()!!.setLayout(
                900,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    fun setOnClickListenerToSubmitUserRating(userRatingDialog: Dialog){
        userRatingDialog.findViewById<MaterialButton>(R.id.btn_submit)
            .setOnClickListener {

                activity.viewModel.isUserInfoUpdatedLiveData.observe(viewLifecycleOwner) {
                    if (!it) {
                        Toast.makeText(
                            requireContext(),
                            "User rating failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@observe
                    }

                    Toast.makeText(
                        requireContext(),
                        "User rated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    userRatingDialog.dismiss()
                    findNavController().navigate(MessageFragmentDirections.actionMessageFragmentSelf(otherUserId))
                }
                activity.userInfo.totalRatingStars += ratingStarsGiven
                activity.userInfo.totalRates += 1
                activity.userInfo.rating =
                    activity.userInfo.totalRatingStars / activity.userInfo.totalRates
                activity.viewModel.updateUserInfo(activity.userInfo)
                dialog.dismiss()
            }
    }

    private fun setOnClickListenersRatingStars(userRatingDialog: Dialog){

        val star1 = userRatingDialog.findViewById<ImageView>(R.id.star1)
        val star2 = userRatingDialog.findViewById<ImageView>(R.id.star2)
        val star3 = userRatingDialog.findViewById<ImageView>(R.id.star3)
        val star4 = userRatingDialog.findViewById<ImageView>(R.id.star4)
        val star5 = userRatingDialog.findViewById<ImageView>(R.id.star5)
        star1.setOnClickListener {
            ratingStarsGiven = 1f
            star1.setImageResource(R.drawable.ic_baseline_star_24)
        }
        star2.setOnClickListener {
            ratingStarsGiven = 2f
            star1.setImageResource(R.drawable.ic_baseline_star_24)
            star2.setImageResource(R.drawable.ic_baseline_star_24)
        }
        star3.setOnClickListener {
            ratingStarsGiven = 3f
            star1.setImageResource(R.drawable.ic_baseline_star_24)
            star2.setImageResource(R.drawable.ic_baseline_star_24)
            star3.setImageResource(R.drawable.ic_baseline_star_24)
        }
        star4.setOnClickListener {
            ratingStarsGiven = 4f
            star1.setImageResource(R.drawable.ic_baseline_star_24)
            star2.setImageResource(R.drawable.ic_baseline_star_24)
            star3.setImageResource(R.drawable.ic_baseline_star_24)
            star4.setImageResource(R.drawable.ic_baseline_star_24)
        }
        star5.setOnClickListener {
            ratingStarsGiven = 5f
            star1.setImageResource(R.drawable.ic_baseline_star_24)
            star2.setImageResource(R.drawable.ic_baseline_star_24)
            star3.setImageResource(R.drawable.ic_baseline_star_24)
            star4.setImageResource(R.drawable.ic_baseline_star_24)
            star5.setImageResource(R.drawable.ic_baseline_star_24)
        }
    }

    override fun onItemClick(position: Int) {
        dialog.findViewById<MaterialButton>(R.id.btn_confirm).isEnabled = true

        if (viewingThisUsersCollectibles) {
            thisUsersSelectedCollectiblesForTrade.add(collectiblesForTrade[position])
        } else {
            otherUsersSelectedCollectiblesForTrade.add(collectiblesForTrade[position])
        }
    }
}