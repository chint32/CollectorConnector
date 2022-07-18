package com.example.collectorconnector.main

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
import com.example.collectorconnector.models.TradeMessage
import com.example.collectorconnector.models.UserInfo
import com.example.collectorconnector.util.Constants
import com.example.collectorconnector.util.Constants.ONE_HUNDRED_MEGABYTE
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment(), CollectibleAdapter.OnItemClickListener {

    private lateinit var binding: FragmentHomeBinding
    private val searchFeedCollectibles = ArrayList<Collectible>()
    var searchFeedAdapter = CollectibleAdapter(searchFeedCollectibles, this, false)

    //reference to allow user to filter search with city, state, tags
    private var cityStatesList = ArrayList<Pair<String, String>>()
    private var statesList = ArrayList<String>()
    private val citiesList = ArrayList<String>()
    private var selectedState = ""
    private var selectedCity = ""
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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        val activity = (requireActivity() as MainActivity)

        statesList = activity.intent.extras!!.get("states") as ArrayList<String>
        cityStatesList =
            activity.intent.extras!!.get("cities_states") as ArrayList<Pair<String, String>>

        println(statesList)
        val statesAdapter =
            ArrayAdapter(requireContext(), R.layout.spinner_item, statesList)
        binding.stateSpinner.adapter = statesAdapter

        //observe current user info data and load it into search filters
        activity.viewModel.userInfoLiveData.observe(viewLifecycleOwner) {
            if (it == null) {
                Toast.makeText(requireContext(), "Error getting user info", Toast.LENGTH_SHORT).show()
                return@observe
            }
            userInfo = it.toObject(UserInfo::class.java)!!
            setSpinnerListeners()
            binding.stateSpinner.setSelection(statesList.indexOf(userInfo.state))
            binding.citySpinner.setSelection(citiesList.indexOf(userInfo.city))
        }
        //call to get user info
        activity.viewModel.getUserInfo(activity.currentUser!!.uid)



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

        binding.collectiblesRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.collectiblesRecycler.adapter = activity.feedAdapter

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
                        activity.currentUser.uid
                    )
                }
            })

        //call to get trade messages for this user to check if any were accepted
        activity.viewModel.getTradeMessagesForUser(activity.currentUser.uid)

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
                                tradeMessage.recipientId,
                                tradeMessage.senderId,
                                tradeMessage
                            )
                            buildUserRatingDialog(userRatingDialog)
                            userRatingDialog.show()
                            setUserRatingAndSubmit(userRatingDialog, activity)
                        }
                    }
                }
            }
        })

        //observe where or not userInfo was updated successfully after rating user
        activity.viewModel.isUserInfoUpdatedLiveData.observe(viewLifecycleOwner, Observer {
            if (!it) {
                Toast.makeText(requireContext(), "User rating failed", Toast.LENGTH_SHORT).show()
                return@Observer
            }

            Toast.makeText(requireContext(), "User rated successfully", Toast.LENGTH_SHORT).show()
            userRatingDialog.dismiss()
        })

        //when showing trade acceptance details, retrieve the collectibles from the sender
        activity.viewModel.tradeImagesSenderLiveData.observe(viewLifecycleOwner, Observer {
            if(it == null) {
                Toast.makeText(requireContext(), "Error getting trade images for sender", Toast.LENGTH_SHORT).show()
                return@Observer
            }

            val rv =
                tradeDetailsDialog.findViewById<RecyclerView>(R.id.this_user_collectibles_trade_recycler)
            rv.adapter = ImageAdapter(tradeDetailsSenderCollectibleImages, false)

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
        activity.viewModel.tradeImagesReceiverLiveData.observe(viewLifecycleOwner, Observer {
            if(it == null) {
                Toast.makeText(requireContext(), "Error getting trade images for receiver", Toast.LENGTH_SHORT).show()
                return@Observer
            }

            val rv =
                tradeDetailsDialog.findViewById<RecyclerView>(R.id.other_user_collectibles_trade_recycler)
            rv.adapter = ImageAdapter(tradeDetailsReceiverCollectibleImages, false)

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
            binding.myProgressBar.visibility = View.VISIBLE
            searchFeedCollectibles.clear()
            searchFeedAdapter = CollectibleAdapter(searchFeedCollectibles, this, false)
            binding.collectiblesRecycler.adapter = searchFeedAdapter
            searchValue = binding.editText.text.toString()
            activity.viewModel.getAllUsersWithCollectibles()
        }

        return binding.root
    }

    private fun observeDataFromUserSearch(activity: MainActivity) {

        // search feed collectibles - Observer 1
        activity.viewModel.usersWithCollectiblesLiveData.observe(
            viewLifecycleOwner,
            Observer {
                if (it == null) {
                    Toast.makeText(requireContext(), "Error getting collectibles for search", Toast.LENGTH_SHORT).show()
                    return@Observer
                }

                for (item in it.prefixes) {
                    if (item.name != activity.currentUser!!.uid)
                        activity.viewModel.getSearchFeedCollectiblesByUid(item.name)

                }
            })


        activity.viewModel.searchFeedCollectiblesLiveData.observe(viewLifecycleOwner, Observer {
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
                            metadata.getCustomMetadata("state") == selectedState &&
                            metadata.getCustomMetadata("city") == selectedCity &&
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

    private fun setUserRatingAndSubmit(userRatingDialog: Dialog, activity: MainActivity) {
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
        userRatingDialog.findViewById<MaterialButton>(R.id.btn_submit)
            .setOnClickListener {
                activity.userInfo.totalRatingStars += ratingStarsGiven
                activity.userInfo.totalRates += 1
                activity.userInfo.rating =
                    activity.userInfo.totalRatingStars / activity.userInfo.totalRates
                viewModel.updateUserInfo(activity.userInfo)
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

    private fun loadCitySpinnerBasedOnState(position: Int) {
        citiesList.clear()

        for (cityState in cityStatesList) {
            if (statesList[position] == cityState.first)
                citiesList.add(cityState.second)

        }
    }

    private fun setSpinnerListeners() {

        binding.stateSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?, position: Int, id: Long
            ) {
                if (view == null) return
                selectedState = statesList[position]
                loadCitySpinnerBasedOnState(position)

                val citiesAdapter =
                    ArrayAdapter(requireContext(), R.layout.spinner_item, citiesList)
                binding.citySpinner.adapter = citiesAdapter
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        binding.citySpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?, position: Int, id: Long
            ) {
                if (view == null) return
                selectedCity = citiesList[position]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
    }

    override fun onItemClick(position: Int) {
        val collectible = searchFeedCollectibles[position]
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToCollectibleDetailsFragment(
                collectible
            )
        )
    }
}