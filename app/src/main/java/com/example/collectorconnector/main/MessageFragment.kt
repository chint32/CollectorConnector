package com.example.collectorconnector.main

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.collectorconnector.R
import com.example.collectorconnector.adapters.CollectibleAdapter
import com.example.collectorconnector.adapters.MessageAdapter
import com.example.collectorconnector.databinding.FragmentMessageBinding
import com.example.collectorconnector.models.*
import com.example.collectorconnector.util.Constants
import com.example.collectorconnector.util.Constants.PICK_IMAGE_REQUEST
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*


class MessageFragment : Fragment(), CollectibleAdapter.OnItemClickListener,
    CollectibleAdapter.OnFavoriteClickListener, MessageAdapter.OnItemClickListener {

    private lateinit var binding: FragmentMessageBinding
    val viewModel: MainViewModel by viewModels()

    //in message fragment, current user is messaging other user.
    //this is id of other user
    private lateinit var otherUserId: String
    private lateinit var otherUserInfo: UserInfo

    //booleans to determine which layout for trade dialog as well as which users
    //collectibles to display
    private var viewingThisUsersCollectibles = true
    private var readyToOfferTrade = false


    //collectibles separated by sender/receiver
    private val thisUsersSelectedCollectiblesForTrade = ArrayList<Collectible>()
    private val user1SelectedCollectibles = ArrayList<Collectible>()
    private val otherUsersSelectedCollectiblesForTrade = ArrayList<Collectible>()
    private val user2SelectedCollectibles = ArrayList<Collectible>()

    //dialog that will be used to facilitate trade offer
    //(picking trade collectibles, confirming trade, etc)
    private lateinit var dialog: Dialog

    // dialog that will be used to show trade details
    // when user clicks on a trade message
    private lateinit var tradeDetailsDialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_message, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        val activity = (requireActivity() as MainActivity)

        //add back navigation arrow
        activity.binding.toolbar.navigationIcon =
            resources.getDrawable(R.drawable.ic_baseline_arrow_back_24)
        activity.binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // make sure messages tab in bottom nav bar is highlighted
        activity.binding.bottomNavigationView.menu.getItem(0).isChecked = true

        // progress dialog for uploading image
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setTitle(getString(R.string.sending_trade_offer))
        progressDialog.setCancelable(false)
        // dialog used to facilitate trade offers
        dialog = Dialog(requireContext())
        //dialog user to show trade details
        tradeDetailsDialog = Dialog(requireContext())
        // dialog that will be used to facilitate rating users
        val userRatingDialog = Dialog(requireContext())

        val messages = ArrayList<Message>()
        val adapter = MessageAdapter(messages, activity.currentUser!!.uid,this)
        binding.messagesRecycler.adapter = adapter

        var collectibleAdapter =
            CollectibleAdapter(activity.userInfo.collectibles, activity.userInfo, this,
                this, true, false)
        collectibleAdapter.submitList(activity.userInfo.collectibles)

        //get the other users id passed in to messagesFragment as argument
        otherUserId = requireArguments().get("otherUserId").toString()
        viewModel.getUserInfo(otherUserId)
        viewModel.userInfoLiveData.observe(viewLifecycleOwner) {
            if (it == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_getting_user_info),
                    Toast.LENGTH_SHORT
                ).show()
                return@observe
            }
            otherUserInfo = it
            binding.textView9.text = "Conversation with: ${otherUserInfo.screenName}"
        }

        //observe messages and handle each type of message accordingly (TEXT, IMAGE, TRADE)
        viewModel.listenForMessagesFromOtherUser(activity.currentUser.uid, otherUserId)

        //observe whether or not text message is successfully sent
        activity.viewModel.isTextMessageSentLiveData.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer {
                if (!it) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.text_message_failed_to_send),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    return@Observer
                }
                binding.etMessageToSend.setText("")
            })

        viewModel.isTradeStatusAcceptanceReceivedUpdatedLiveData.observe(viewLifecycleOwner){ tradeMessage ->
            if(tradeMessage == null){
                Toast.makeText(requireContext(), getString(R.string.error_updating_trade_accepted), Toast.LENGTH_SHORT).show()
                return@observe
            }

            userRatingDialog.setContentView(R.layout.dialog_rate_user)
            userRatingDialog.setCancelable(false)
            if (userRatingDialog.getWindow() != null) {
                val configuration: Configuration = (requireActivity() as MainActivity).getResources().getConfiguration()
                if(configuration.smallestScreenWidthDp > 600)
                    userRatingDialog.window!!.setLayout(1800, 2000)
                else if(configuration.smallestScreenWidthDp <= 600 && configuration.smallestScreenWidthDp >= 380)
                    userRatingDialog.window!!.setLayout(1080, 1700)
                else userRatingDialog.window!!.setLayout(480, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            }

            setOnClickListenerToSubmitUserRating(userRatingDialog)
            userRatingDialog.show()
        }

        activity.viewModel.isTradeMessageSentLiveData.observe(viewLifecycleOwner){
            if(it) Toast.makeText(requireContext(), getString(R.string.trade_offer_sent), Toast.LENGTH_SHORT).show()
            else Toast.makeText(requireContext(), getString(R.string.error_sending_trade_offer), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            progressDialog.dismiss()
        }

        // send trade message
        binding.ivSendTradeIcon.setOnClickListener {

            buildTradeOfferDialog()

            val rv1: RecyclerView = dialog.findViewById(R.id.trade_collectibles_recycler_1)
            val rv2: RecyclerView = dialog.findViewById(R.id.trade_collectibles_recycler_2)
            dialog.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
                collectibleAdapter.submitList(activity.userInfo.collectibles)
                rv1.visibility = View.VISIBLE
                rv2.visibility = View.GONE
                dialog.findViewById<TextView>(R.id.tv_tradeOfferTitle).visibility = View.VISIBLE
                dialog.findViewById<TextView>(R.id.tv_tradeOfferTitle).text = getString(R.string.select_collectibles_to_give)
                dialog.findViewById<TextView>(R.id.tv_for).visibility = View.GONE

                user1SelectedCollectibles.clear()
                user2SelectedCollectibles.clear()
                thisUsersSelectedCollectiblesForTrade.clear()
                otherUsersSelectedCollectiblesForTrade.clear()
                viewingThisUsersCollectibles = true
                readyToOfferTrade = false
                dialog.dismiss()
            }


            dialog.findViewById<MaterialButton>(R.id.btn_confirm).setOnClickListener {

                collectibleAdapter = CollectibleAdapter(activity.userInfo.collectibles, activity.userInfo, this, this, true, false)
                rv1.adapter = collectibleAdapter
                rv2.adapter = collectibleAdapter
                
                if (viewingThisUsersCollectibles)
                    selectItemsToGiveForTrade(collectibleAdapter)
                else {
                    if (!readyToOfferTrade)
                        selectItemsToReceiveForTrade()
                    else
                        confirmAndSendTradeOffer(progressDialog)
                }
            }

            dialog.show()
            // create and setup recyclerview/adapter to display trade collectibles
            rv1.adapter = collectibleAdapter
            rv2.adapter = collectibleAdapter

        }

        viewModel.isTradeStatusUpdatedLiveData.observe(viewLifecycleOwner){
            if(it == null){
                Toast.makeText(requireContext(), getString(R.string.error_updating_trade_status), Toast.LENGTH_SHORT).show()
                return@observe
            }
            if(tradeDetailsDialog.isShowing) tradeDetailsDialog.dismiss()

        }

        // send text message
        binding.ivSendIcon.setOnClickListener {
            if (binding.etMessageToSend.text.toString() == "") {
                Toast.makeText(requireContext(), getString(R.string.message_is_empty), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            sendTextMessage()
        }

        //send image message
        binding.ivSendImageIcon.setOnClickListener {
            selectImage()
        }

        return binding.root
    }

    private fun sendTextMessage() {
        val activity = (requireActivity() as MainActivity)
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

        activity.viewModel.sendTextMessage(
            activity.userInfo,
            otherUserInfo,
            textMessage
        )
    }


    private fun confirmAndSendTradeOffer(progressDialog: ProgressDialog) {

        val activity = (requireActivity() as MainActivity)
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
                user1SelectedCollectibles,
                user2SelectedCollectibles
            ),
            "OPEN"
        )
        activity.viewModel.sendTradeMessage(
            activity.userInfo,
            otherUserInfo,
            tradeMessage
        )

        progressDialog.show()
    }

    private fun selectItemsToGiveForTrade(collectibleAdapter: CollectibleAdapter) {

        val btnConfirm = dialog.findViewById<MaterialButton>(R.id.btn_confirm)
        btnConfirm.isEnabled = false
        btnConfirm.alpha = .5f

        dialog.findViewById<TextView>(R.id.tv_tradeOfferTitle).text =
            getString(R.string.select_collectibles_to_recieve)
        viewingThisUsersCollectibles = false
        collectibleAdapter.submitList(otherUserInfo.collectibles)
    }

    private fun selectItemsToReceiveForTrade() {
        val activity = (requireActivity() as MainActivity)

        dialog.findViewById<TextView>(R.id.tv_tradeOfferTitle).text = getString(R.string.confirm_trade_confirm)
        dialog.findViewById<TextView>(R.id.tv_for).visibility = View.VISIBLE


        val thisUserFinalAdapter = CollectibleAdapter(
            thisUsersSelectedCollectiblesForTrade,  activity.userInfo, this, this, false, false
        )
        val thisUsersRv =
            dialog.findViewById<RecyclerView>(R.id.trade_collectibles_recycler_1)
        thisUsersRv.adapter = thisUserFinalAdapter

        val otherUserFinalAdapter = CollectibleAdapter(
            otherUsersSelectedCollectiblesForTrade, activity.userInfo, this, this, false, false
        )

        val otherUsersRv = dialog.findViewById<RecyclerView>(R.id.trade_collectibles_recycler_2)
        otherUsersRv.visibility = View.VISIBLE
        otherUsersRv.adapter = otherUserFinalAdapter
        otherUserFinalAdapter.submitList(otherUsersSelectedCollectiblesForTrade)

        readyToOfferTrade = true
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

            val activity = (requireActivity() as MainActivity)
            // Get the Uri of data
            val filePath = data.data

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
                "",
            )

            activity.viewModel.sendImageMessage(
                activity.userInfo,
                otherUserInfo,
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
                getString(R.string.select_image_from_here)
            ),
            PICK_IMAGE_REQUEST
        )
    }

    private fun buildTradeOfferDialog() {

        val configuration: Configuration = (requireActivity() as MainActivity).getResources().getConfiguration()

        dialog.setContentView(R.layout.dialog_trade_offer)
        dialog.setCancelable(false)
        if (dialog.window != null) {
            if(configuration.smallestScreenWidthDp > 600)
                dialog.window!!.setLayout(1800, 2000)
            else if(configuration.smallestScreenWidthDp <= 600 && configuration.smallestScreenWidthDp >= 380)
                dialog.window!!.setLayout(1080, 1600)
            else dialog.window!!.setLayout(480, 750)
        }
        dialog.findViewById<TextView>(R.id.tv_tradeOfferTitle).text = getString(R.string.select_collectibles_to_give)
        dialog.findViewById<TextView>(R.id.tv_for).visibility = View.GONE
        dialog.findViewById<RecyclerView>(R.id.trade_collectibles_recycler_2).visibility = View.GONE

    }

    fun setOnClickListenerToSubmitUserRating(userRatingDialog: Dialog) {
        userRatingDialog.findViewById<MaterialButton>(R.id.btn_submit)
            .setOnClickListener {
                val activity = (requireActivity() as MainActivity)
                val ratingBar = userRatingDialog.findViewById<RatingBar>(R.id.rating)
                val ratingStarsGiven = ratingBar.rating

                activity.viewModel.isUserInfoUpdatedLiveData.observe(viewLifecycleOwner) {
                    if (it == null) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.user_rating_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@observe
                    }

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.user_rated_successfully),
                        Toast.LENGTH_SHORT
                    ).show()
                    userRatingDialog.dismiss()
                    findNavController().navigate(
                        MessageFragmentDirections.actionMessageFragmentSelf(
                            otherUserId
                        )
                    )
                }
                otherUserInfo.totalRatingStars += ratingStarsGiven
                otherUserInfo.totalRates += 1
                otherUserInfo.rating =
                    otherUserInfo.totalRatingStars / otherUserInfo.totalRates
                activity.viewModel.updateProfile(otherUserInfo, null)
                dialog.dismiss()
            }
    }


    override fun onItemClick(position: Int, collectible: Collectible, isChecked: Boolean) {
        val btnConfirm = dialog.findViewById<MaterialButton>(R.id.btn_confirm)
        if(tradeDetailsDialog.isShowing){
            findNavController().navigate(MessageFragmentDirections.actionMessageFragmentToCollectibleDetailsFragment(collectible))
            tradeDetailsDialog.dismiss()
        }
        else {
            if (viewingThisUsersCollectibles) {
                if (isChecked) {
                    thisUsersSelectedCollectiblesForTrade.add((requireActivity() as MainActivity).userInfo.collectibles[position])
                    user1SelectedCollectibles.add(collectible)
                } else {
                    thisUsersSelectedCollectiblesForTrade.remove((requireActivity() as MainActivity).userInfo.collectibles[position])
                    user1SelectedCollectibles.remove(collectible)
                }

                if (thisUsersSelectedCollectiblesForTrade.isNotEmpty()) {
                    btnConfirm.isEnabled = true
                    btnConfirm.alpha = 1f
                } else {
                    btnConfirm.isEnabled = false
                    btnConfirm.alpha = .5f
                }
            } else {
                if (isChecked) {
                    otherUsersSelectedCollectiblesForTrade.add(otherUserInfo.collectibles[position])
                    user2SelectedCollectibles.add(collectible)
                } else {
                    otherUsersSelectedCollectiblesForTrade.remove(otherUserInfo.collectibles[position])
                    user2SelectedCollectibles.remove(collectible)
                }

                if (otherUsersSelectedCollectiblesForTrade.isNotEmpty()) {
                    btnConfirm.isEnabled = true
                    btnConfirm.alpha = 1f
                } else {
                    btnConfirm.isEnabled = false
                    btnConfirm.alpha = .5f
                }
            }
        }
    }

    override fun onFavoriteClick(position: Int, collectible: Collectible) {
        Toast.makeText(requireContext(), "Favorite clicked: $position", Toast.LENGTH_SHORT).show()
    }

    override fun onTradeMessageItemClick(tradeMessage: TradeMessage) {

        val activity = (requireActivity() as MainActivity)
        buildTradeDetailDialog()
        tradeDetailsDialog.show()

        val rv1: RecyclerView = tradeDetailsDialog.findViewById(R.id.this_user_collectibles_trade_recycler)
        val rv2: RecyclerView = tradeDetailsDialog.findViewById(R.id.other_user_collectibles_trade_recycler)

        rv1.adapter = CollectibleAdapter(tradeMessage.trade!!.senderCollectibles,  activity.userInfo, this, this, false, false)
        rv2.adapter =  CollectibleAdapter(tradeMessage.trade.receiverCollectibles,activity.userInfo, this, this, false, false)

        // trade details dialog is shown when trade message is clicked

        val btnNegative = tradeDetailsDialog.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnPositive = tradeDetailsDialog.findViewById<MaterialButton>(R.id.btn_confirm)
        val btnNeutral = tradeDetailsDialog.findViewById<MaterialButton>(R.id.btn_neutral)


        // handle the different cases of trade status (OPEN, CLOSED, ACCEPTED, REJECTED) and set
        // the appropriate actions for positive and negative buttons of trade details dialog
        // OPEN
        if (tradeMessage.tradeStatus == Constants.TRADE_STATUS_OPEN) {
            btnPositive.visibility = View.VISIBLE
            btnNegative.visibility = View.VISIBLE
            btnPositive.alpha = 1f
            btnPositive.isEnabled = true
            tradeDetailsDialog.findViewById<TextView>(R.id.tv_tradeDetailsTitle).text = getString(R.string.trade_offered)
            if (activity.currentUser!!.uid == tradeMessage.senderId) {

                btnNegative.text = getString(R.string.cancel_trade)
                btnNegative.setOnClickListener {
                    // cancel the trade
                    tradeMessage.tradeStatus = Constants.TRADE_STATUS_CANCELED
                    viewModel.updateTradeStatus(activity.userInfo, otherUserInfo, tradeMessage)
                }
                btnPositive.text = getString(R.string.ok2)
                btnPositive.setOnClickListener {
                    tradeDetailsDialog.dismiss()
                }
            } else {

                btnNegative.text = getString(R.string.reject_trade)
                btnNeutral.visibility = View.VISIBLE
                btnNeutral.setOnClickListener{
                    btnNeutral.visibility = View.GONE
                    tradeDetailsDialog.dismiss()
                }
                btnNegative.setOnClickListener {
                    // reject the trade
                    tradeMessage.tradeStatus = Constants.TRADE_STATUS_REJECTED
                    viewModel.updateTradeStatus(
                        activity.userInfo, otherUserInfo,
                        tradeMessage
                    )
                    btnNeutral.visibility = View.GONE
                }
                btnPositive.text = getString(R.string.accept_trade)
                btnPositive.setOnClickListener {
                    tradeMessage.tradeStatus = Constants.TRADE_STATUS_ACCEPTED
                    viewModel.updateTradeStatus(
                        activity.userInfo, otherUserInfo,
                        tradeMessage
                    )
                    tradeMessage.tradeAcceptanceReceived = true
                    viewModel.updateTradeAcceptanceReceived(tradeMessage.recipientId, tradeMessage.senderId, tradeMessage, false)
                    btnNeutral.visibility = View.GONE
                    tradeDetailsDialog.dismiss()

                }
            }
        }

        // CANCELED
        else if (tradeMessage.tradeStatus == Constants.TRADE_STATUS_CANCELED) {
            btnPositive.isEnabled = false
            tradeDetailsDialog.findViewById<TextView>(R.id.tv_tradeDetailsTitle).text = getString(R.string.trade_canceled)

            btnNegative.text = getString(R.string.ok)
            btnPositive.visibility = View.GONE
            btnNegative.setOnClickListener {
                tradeDetailsDialog.dismiss()
            }
        }

        // REJECTED
        else if (tradeMessage.tradeStatus == Constants.TRADE_STATUS_REJECTED) {
            btnPositive.isEnabled = false
            tradeDetailsDialog.findViewById<TextView>(R.id.tv_tradeDetailsTitle).text = getString(R.string.trade_rejected)

            btnNegative.text = getString(R.string.ok)
            btnPositive.visibility = View.GONE
            btnNegative.setOnClickListener {
                tradeDetailsDialog.dismiss()
            }
        }

        // ACCEPTED
        else if (tradeMessage.tradeStatus == Constants.TRADE_STATUS_ACCEPTED) {
            btnPositive.isEnabled = false
            tradeDetailsDialog.findViewById<TextView>(R.id.tv_tradeDetailsTitle).text = getString(R.string.trade_accepted)

            btnNegative.text = getString(R.string.ok)
            btnPositive.visibility = View.GONE
            btnNegative.setOnClickListener {
                tradeDetailsDialog.dismiss()
            }
        }
    }
    private fun buildTradeDetailDialog() {
        tradeDetailsDialog.setContentView(R.layout.dialog_trade_details)
        tradeDetailsDialog.setCancelable(false)
        if (tradeDetailsDialog.getWindow() != null) {
            val configuration: Configuration = (requireActivity() as MainActivity).getResources().getConfiguration()
            if(configuration.smallestScreenWidthDp > 600)
                tradeDetailsDialog.window!!.setLayout(1800, 2000)
            else if(configuration.smallestScreenWidthDp <= 600 && configuration.smallestScreenWidthDp >= 380)
                tradeDetailsDialog.window!!.setLayout(1080, 1700)
            else tradeDetailsDialog.window!!.setLayout(480, 700)
        }
        tradeDetailsDialog.findViewById<TextView>(R.id.tv_tradeDetailsTitle).text = getString(R.string.trade_offer)
    }
}