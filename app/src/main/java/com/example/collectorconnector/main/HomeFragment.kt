package com.example.collectorconnector.main

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collectorconnector.R
import com.example.collectorconnector.adapters.CollectibleAdapter
import com.example.collectorconnector.adapters.ImageAdapter
import com.example.collectorconnector.databinding.FragmentHomeBinding
import com.example.collectorconnector.models.Collectible
import com.example.collectorconnector.models.FavoriteCollectible
import com.example.collectorconnector.models.TradeMessage
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.util.Constants
import com.example.collectorconnector.util.Constants.ONE_HUNDRED_MEGABYTE
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment(), CollectibleAdapter.OnItemClickListener, CollectibleAdapter.OnFavoriteClickListener {

    private lateinit var binding: FragmentHomeBinding
    private val searchFeedCollectibles = ArrayList<Collectible>()
    private val mainFeedCollectibles = ArrayList<Collectible>()
    private var searchFeedAdapter = CollectibleAdapter(searchFeedCollectibles, null,this, this, false, true)


    private var tagsList: ArrayList<Int> = ArrayList()

    private var myTags = ArrayList<String>()

    //user search value which will match words in collectible name and description
    private var searchValue = ""

    //dialog will pop up if to inform user that their trade offer was accepted by other user
    private lateinit var tradeDetailsDialog: Dialog

    //list of collectibles from both users for trade
    private val tradeDetailsSenderCollectibleImages = ArrayList<ByteArray>()
    private val tradeDetailsReceiverCollectibleImages = ArrayList<ByteArray>()

    //rating stars when user gives rating after performing a trade
    private var ratingStarsGiven = 0f

    //current user info
    private lateinit var userInfo: UserInfo
    private val viewModel: MainViewModel by viewModels()
    private var isUsingSearchFeed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        val activity = (requireActivity() as MainActivity)
        val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)
        if(toolbar != null)
            toolbar.navigationIcon = null

        userInfo = activity.intent.extras!!.get("user_info") as UserInfo
        var feedAdapter = CollectibleAdapter(mainFeedCollectibles, userInfo, this, this, false, true)
        binding.collectiblesRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.collectiblesRecycler.adapter = feedAdapter

        binding.myProgressBar.visibility = View.VISIBLE
        // main feed collectibles observer part 1 - get all users with collectibles (except current user)
        if(!viewModel.usersWithCollectiblesLiveData.hasObservers()) {
            viewModel.usersWithCollectiblesLiveData.observe(activity, Observer {
                binding.myProgressBar.visibility = View.GONE
                if (it == null) {
                    Toast.makeText(
                        requireContext(),
                        "Error getting other users",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Observer
                }
                if(it.items.isEmpty()) {
                    binding.tvNoResults.text = "No one has uploaded anything yet. Try again later."
                    binding.tvNoResults.visibility = View.VISIBLE
                }
                binding.tvNoResults.visibility = View.GONE
                for (item in it.prefixes) {
                    if (item.name != activity.currentUser!!.uid)
                        viewModel.getOtherUserInfo(item.name)
                }
            })
            viewModel.getAllUsersWithCollectibles()
        }

        // main feed collectibles observer part 2 - check if other users are within user's search distance
        // if so, make network call to get each user's collectibles
        if(!viewModel.otherUserInfoLiveData.hasObservers()) {
            viewModel.otherUserInfoLiveData.observe(activity) {
                if (it == null) {
                    Toast.makeText(
                        activity,
                        "Error getting user info for main feed collectibles",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@observe
                }


                val otherUserInfo = it.toObject(UserInfo::class.java) ?: return@observe
                val distance = distance(
                    userInfo.latitude.toFloat(), userInfo.longitude.toFloat(),
                    otherUserInfo.latitude.toFloat(), otherUserInfo.longitude.toFloat()
                )
                if (distance < userInfo.searchDistance) {
                    viewModel.getMainFeedCollectiblesByUid(otherUserInfo.uid)
                }
            }
        }


        // main feed collectibles observer part 3 - get the collectibles of the users within search distance
        // and filter out the ones that don't match the users interests (tags)
        if(!viewModel.mainFeedCollectiblesLiveData.hasObservers()) {
            viewModel.mainFeedCollectiblesLiveData.observe(activity, Observer {
                if (it == null) {
                    Toast.makeText(
                        activity,
                        "Error getting collectible for home feed",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Observer
                }

                // not sure why commented out code below is not working =[

//                if(it.items.isEmpty()){
//                    binding.tvNoResults.text = "0 results. Try adjusting your search distance and/or interests to capture more results"
//                    binding.tvNoResults.visibility = View.VISIBLE
//                    return@Observer
//                } else binding.tvNoResults.visibility = View.GONE

                feedAdapter = CollectibleAdapter(mainFeedCollectibles, activity.userInfo, this, this, false, true)
                binding.collectiblesRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
                binding.collectiblesRecycler.adapter = feedAdapter

                // cycle through collectibles for all users
                for (collectibleData in it.items) {
                    // make sure current users own collectibles are not returned in the search
                    collectibleData.metadata.addOnSuccessListener { metadata ->

                        // if not same tags, skip
                        if (!userInfo.interests.toString().contains(
                                metadata.getCustomMetadata("tags")!!.substringAfter("[")
                                    .substringBefore("]")
                            )
                        ) return@addOnSuccessListener

                        collectibleData.getBytes(ONE_HUNDRED_MEGABYTE)
                            .addOnSuccessListener { byteArray ->

                                val tags = ArrayList<String>()
                                val tagsArr = metadata.getCustomMetadata("tags")
                                    .toString()
                                    .split(",")
                                for (i in tagsArr.indices)
                                    tags.add(tagsArr[i])
                                // construct model
                                val collectible = Collectible(
                                    collectibleData.name,
                                    metadata.getCustomMetadata("name").toString(),
                                    metadata.getCustomMetadata("desc").toString(),
                                    metadata.getCustomMetadata("cond").toString(),
                                    byteArray,
                                    metadata.getCustomMetadata("views").toString(),
                                    tags,
                                    metadata.getCustomMetadata("ownerId").toString()
                                )
                                if (!mainFeedCollectibles.contains(collectible)) {

                                    //add to recycler list
                                    mainFeedCollectibles.add(collectible)
                                    feedAdapter.notifyItemInserted(
                                        mainFeedCollectibles.indexOf(
                                            collectible
                                        )
                                    )
                                }

                            }.addOnFailureListener {
                                Toast.makeText(
                                    activity,
                                    "Error: " + it.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                binding.myProgressBar.visibility = View.GONE
            })
        }

        val userRatingDialog = Dialog(requireContext())

        tradeDetailsDialog = Dialog(requireContext())
        tradeDetailsDialog.setContentView(R.layout.dialog_trade_offer)

        tradeDetailsDialog.findViewById<RecyclerView>(R.id.trade_collectibles_recycler).visibility =
            View.GONE
        tradeDetailsDialog.findViewById<LinearLayout>(R.id.final_trade_offer_layout).visibility =
            View.VISIBLE

        tradeDetailsDialog.setCancelable(false)

        if (tradeDetailsDialog.getWindow() != null) {
            tradeDetailsDialog.getWindow()!!.setLayout(
                900,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        tradeDetailsDialog.findViewById<TextView>(R.id.textView5).text = "Trade Accepted"



        //observe trade messages for this user and listen for more just in case a trade is accepted
        activity.viewModel.tradeMessagesForUser.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer {
                if (it == null) {
                    Toast.makeText(requireContext(), "Error getting user trade messages.", Toast.LENGTH_SHORT).show()
                    return@Observer
                }

                for (doc in it.documents) {
                    activity.viewModel.listenForMessagesFromOtherUser(
                        doc.id,
                        activity.currentUser!!.uid
                    )
                }
            })

        //call to get trade messages for this user to check if any were accepted
        activity.viewModel.getTradeMessagesForUser(activity.currentUser!!.uid)

        //observe user trade messages and check if any were accepted that the user hasnt seen.
        //if so, show the trade details to the user
        activity.viewModel.messagesLiveData.observe(viewLifecycleOwner, Observer {
            if (it.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Error getting user messages.\nUnable to check for accepted trade offers", Toast.LENGTH_SHORT).show()
                return@Observer
            }

            for (doc in it!!) {
                val type = doc!!.get("type").toString()
                if (type == Constants.MESSAGE_TYPE_TRADE) {
                    val tradeMessage = doc.toObject(TradeMessage::class.java)!!

                    if (tradeMessage.tradeStatus == Constants.TRADE_STATUS_ACCEPTED && !tradeMessage.tradeAcceptanceReceived) {
                        tradeDetailsDialog.show()
                        println("making call to get images of traded collectibles")
                        viewModel.getImagesForTradeSender(
                            tradeMessage.senderId,
                            tradeMessage.recipientId,
                            tradeMessage.messageId
                        )
                        viewModel.getImagesForTradeReceiver(
                            tradeMessage.senderId,
                            tradeMessage.recipientId,
                            tradeMessage.messageId
                        )

                        val btnCancelTrade =
                            tradeDetailsDialog.findViewById<MaterialButton>(R.id.btn_cancel)
                        btnCancelTrade.isEnabled = false
                        val btnOk =
                            tradeDetailsDialog.findViewById<MaterialButton>(R.id.btn_confirm)
                        btnOk.text = "Ok"
                        btnOk.setOnClickListener {
                            tradeMessage.tradeAcceptanceReceived = true
                            viewModel.updateTradeAcceptanceReceived(
                                tradeMessage.senderId,
                                tradeMessage.recipientId,
                                tradeMessage
                            )
                            viewModel.getUserInfo(tradeMessage.senderId)
                            viewModel.userInfoLiveData.observe(viewLifecycleOwner){
                                if(it == null){
                                    Toast.makeText(requireContext(),
                                        "error getting users info for rating",
                                        Toast.LENGTH_SHORT).show()
                                    return@observe
                                }
                                val tradeReceiversUserInfo = it.toObject(UserInfo::class.java)
                                buildUserRatingDialog(userRatingDialog)
                                userRatingDialog.show()
                                setUserRatingAndSubmit(userRatingDialog, tradeReceiversUserInfo!!)
                            }

                        }
                    }
                }
            }
        })


        //observe where or not userInfo was updated successfully after rating user
        viewModel.isUserRatedSuccessfully.observe(viewLifecycleOwner, Observer {
            if (!it) {
                Toast.makeText(requireContext(), "User rating failed", Toast.LENGTH_SHORT).show()
                return@Observer
            }

            Toast.makeText(requireContext(), "User rated successfully", Toast.LENGTH_SHORT).show()
            userRatingDialog.dismiss()
        })

        //when showing trade acceptance details, retrieve the collectibles from the sender
        viewModel.tradeImagesSenderLiveData.observe(viewLifecycleOwner, Observer {
            if(it == null) {
                Toast.makeText(requireContext(), "Error getting trade images for sender", Toast.LENGTH_SHORT).show()
                return@Observer
            }
            println("received images of traded collectibles")

            val rv =
                tradeDetailsDialog.findViewById<RecyclerView>(R.id.this_user_collectibles_trade_recycler)
            rv.adapter = ImageAdapter(tradeDetailsSenderCollectibleImages)

            for (item in it.items) {
                item.getBytes(ONE_HUNDRED_MEGABYTE).addOnSuccessListener { byteArray ->
                    if (!tradeDetailsSenderCollectibleImages.contains(byteArray)) {
                        tradeDetailsSenderCollectibleImages.add(byteArray)
                        rv.adapter!!.notifyItemInserted(tradeDetailsSenderCollectibleImages.lastIndex)
                    }
                }
            }
        })

        //when showing trade acceptance details, retrieve the collectibles from the receiver
        viewModel.tradeImagesReceiverLiveData.observe(viewLifecycleOwner, Observer {
            if(it == null) {
                Toast.makeText(requireContext(), "Error getting trade images for receiver", Toast.LENGTH_SHORT).show()
                return@Observer
            }

            val rv =
                tradeDetailsDialog.findViewById<RecyclerView>(R.id.other_user_collectibles_trade_recycler)
            rv.adapter = ImageAdapter(tradeDetailsReceiverCollectibleImages)

            for (item in it.items) {
                item.getBytes(ONE_HUNDRED_MEGABYTE).addOnSuccessListener { byteArray ->
                    if (!tradeDetailsReceiverCollectibleImages.contains(byteArray)) {
                        tradeDetailsReceiverCollectibleImages.add(byteArray)
                        rv.adapter!!.notifyItemInserted(tradeDetailsReceiverCollectibleImages.lastIndex)
                    }
                }
            }
        })

        observeDataFromUserSearch(activity)

        binding.selectTagsTv.setOnClickListener(View.OnClickListener { // Initialize alert dialog
            buildSelectTagsDialog(activity.tagsArray)

        })

        // search feed collectibles call for data which will be observed above
        binding.ivSearch.setOnClickListener {
            isUsingSearchFeed = true
            binding.myProgressBar.visibility = View.VISIBLE
            searchFeedCollectibles.clear()
            searchFeedAdapter = CollectibleAdapter(searchFeedCollectibles, userInfo, this, this, false, true)
            binding.collectiblesRecycler.adapter = searchFeedAdapter
            searchValue = binding.editText.text.toString()
            viewModel.getAllUsersWithCollectibles()
        }

        return binding.root
    }

    private fun distance(lat_a: Float, lng_a: Float, lat_b: Float, lng_b: Float): Float {
        val earthRadius = 3958.75
        val latDiff = Math.toRadians((lat_b - lat_a).toDouble())
        val lngDiff = Math.toRadians((lng_b - lng_a).toDouble())
        val a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(lat_a.toDouble())) * Math.cos(Math.toRadians(lat_b.toDouble())) *
                Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distance = earthRadius * c

        return distance.toFloat()
    }

    private fun observeDataFromUserSearch(activity: MainActivity) {

        // search feed collectibles - Observer 1
        viewModel.usersWithCollectiblesLiveData.observe(
            viewLifecycleOwner,
            Observer {
                if (it == null) {
                    Toast.makeText(requireContext(), "Error getting other users", Toast.LENGTH_SHORT).show()
                    return@Observer
                }
                for (item in it.prefixes) {
                    if (item.name != activity.currentUser!!.uid) {
                        //get collectible
                        viewModel.getUserInfo(item.name)
                    }
                }
            })

        viewModel.userInfoLiveData.observe(activity) {
            if (it == null) {
                Toast.makeText(
                    activity,
                    "Error getting user info for main feed collectibles",
                    Toast.LENGTH_SHORT
                ).show()
                return@observe
            }

            val otherUserInfo = it.toObject(UserInfo::class.java) ?: return@observe
            val distance = distance(
                userInfo.latitude.toFloat(), userInfo.longitude.toFloat(),
                otherUserInfo.latitude.toFloat(), otherUserInfo.longitude.toFloat()
            )
            if (distance < userInfo.searchDistance) {
                viewModel.getSearchFeedCollectiblesByUid(otherUserInfo.uid)
            }
        }


        viewModel.searchFeedCollectiblesLiveData.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                Toast.makeText(requireContext(), "Error getting collectibles for search", Toast.LENGTH_SHORT).show()
                return@Observer
            }

            // cycle through collectibles for all users
            for (collectibleData in it.items) {
                collectibleData.metadata.addOnSuccessListener { metadata ->
                    if (metadata.getCustomMetadata("ownerId") as String != activity.currentUser!!.uid) {

                        val nameAndDesc = metadata.getCustomMetadata("name").toString() + ", " +
                                metadata.getCustomMetadata("desc").toString()

                        // if filters for name, location, and tags return false, skip
                        if (nameAndDesc.contains(searchValue) &&
                            myTags.toString().contains(metadata.getCustomMetadata("tags")!!)
                        ) {
                            collectibleData.getBytes(ONE_HUNDRED_MEGABYTE)
                                .addOnSuccessListener { byteArray ->

                                    val tags = ArrayList<String>()
                                    val tagsArr = metadata.getCustomMetadata("tags")
                                        .toString()
                                        .split(",")
                                    for (i in tagsArr.indices)
                                        tags.add(tagsArr[i])
                                    // construct model
                                    val collectible = Collectible(
                                        collectibleData.name,
                                        metadata.getCustomMetadata("name").toString(),
                                        metadata.getCustomMetadata("desc").toString(),
                                        metadata.getCustomMetadata("cond").toString(),
                                        byteArray,
                                        metadata.getCustomMetadata("state").toString(),
                                        tags,
                                        metadata.getCustomMetadata("ownerId").toString()
                                    )


                                    if (!searchFeedCollectibles.contains(collectible)) {

                                        //add to recycler list
                                        searchFeedCollectibles.add(collectible)
                                        println(searchFeedCollectibles.size)
                                        searchFeedAdapter.notifyItemInserted(
                                            searchFeedCollectibles.indexOf(collectible)
                                        )
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
                }
            }
            binding.myProgressBar.visibility = View.GONE
        })
    }

    private fun setUserRatingAndSubmit(userRatingDialog: Dialog, tradeReceiversUserInfo: UserInfo) {

        val ratingbar = userRatingDialog.findViewById<RatingBar>(R.id.rating)

        userRatingDialog.findViewById<MaterialButton>(R.id.btn_submit)
            .setOnClickListener {
                ratingStarsGiven = ratingbar.rating
                tradeReceiversUserInfo.totalRatingStars += ratingStarsGiven
                tradeReceiversUserInfo.totalRates += 1
                tradeReceiversUserInfo.rating =
                    tradeReceiversUserInfo.totalRatingStars / tradeReceiversUserInfo.totalRates
                viewModel.rateUser(tradeReceiversUserInfo)
                tradeDetailsDialog.dismiss()
            }
    }

    private fun buildUserRatingDialog(userRatingDialog: Dialog) {

        userRatingDialog.setContentView(R.layout.dialog_rate_user)

        userRatingDialog.setCancelable(false)

        if (userRatingDialog.getWindow() != null) {
            userRatingDialog.getWindow()!!.setLayout(
                900,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun buildSelectTagsDialog(tagsArray: Array<String?>) {


        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Categories")
        builder.setCancelable(false)
        var dialog: AlertDialog
        val selectedTags = BooleanArray(tagsArray.size)
        // initially all tags are unchecked
        for (i in selectedTags.indices) selectedTags[i] = false

        builder.setMultiChoiceItems(
            tagsArray, selectedTags,
            DialogInterface.OnMultiChoiceClickListener { dialogInterface, i, b ->
                // check condition
                if (b) {
                    // when checkbox selected
                    // Add position  in lang list
                    tagsList.add(i)
                    // Sort array list
                    tagsList.sort()
                } else {
                    // when checkbox unselected
                    // Remove position from langList
                    tagsList.remove(Integer.valueOf(i))
                }
            })

        builder.setPositiveButton("OK",
            DialogInterface.OnClickListener { dialogInterface, i -> // Initialize string builder
                val stringBuilder = StringBuilder()

                for (j in 0 until tagsList.size) {

                    // concat array value
                    stringBuilder.append(tagsArray.get(tagsList.get(j)))
                    // check condition
                    if (j != tagsList.size - 1) {
                        // When j value  not equal
                        // to lang list size - 1
                        // add comma
                        stringBuilder.append(", ")
                    }
                }
                myTags.add(stringBuilder.toString())
                binding.selectTagsTv.text = stringBuilder.toString()
            })
        builder.setNegativeButton("Cancel",
            DialogInterface.OnClickListener { dialogInterface, i -> // dismiss dialog
                dialogInterface.dismiss()
            })
        builder.setNeutralButton("Clear All",
            DialogInterface.OnClickListener { dialogInterface, i ->

                for (j in 0 until selectedTags.size) {
                    // remove all selection
                    selectedTags[j] = false
                    // clear language list
                    tagsList.clear()
                    myTags.clear()
                    // clear text view value
                    binding.selectTagsTv.text = ""
                }
            })

        dialog = builder.create()
        if (!dialog.isShowing) dialog.show()
    }

    override fun onItemClick(position: Int) {
        val collectible = mainFeedCollectibles[position]
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToCollectibleDetailsFragment(
                collectible
            )
        )
    }

    override fun onFavoriteClick(position: Int) {
        val activity = (requireActivity() as MainActivity)

        val favCollectible: FavoriteCollectible
        if(!isUsingSearchFeed){
            favCollectible = FavoriteCollectible(
                mainFeedCollectibles.get(position).uid,
                mainFeedCollectibles.get(position).ownerId
            )
        } else {
            favCollectible = FavoriteCollectible(
                searchFeedCollectibles.get(position).uid,
                searchFeedCollectibles.get(position).ownerId
            )
        }

        if(activity.userInfo.favoriteCollectibles.contains(favCollectible)) {
            activity.userInfo.favoriteCollectibles.remove(favCollectible)
            userInfo.favoriteCollectibles.remove(favCollectible)
        }

        else {
            activity.userInfo.favoriteCollectibles.add(favCollectible)
            userInfo.favoriteCollectibles.add(favCollectible)
        }

        viewModel.updateUserInfo(userInfo)
    }
}