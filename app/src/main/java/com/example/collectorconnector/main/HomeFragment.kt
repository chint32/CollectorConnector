package com.example.collectorconnector.main

import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.collectorconnector.R
import com.example.collectorconnector.adapters.CollectibleAdapter
import com.example.collectorconnector.databinding.FragmentHomeBinding
import com.example.collectorconnector.models.Collectible
import com.example.collectorconnector.models.UserInfo
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment(), CollectibleAdapter.OnItemClickListener,
    CollectibleAdapter.OnFavoriteClickListener {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        val activity = (requireActivity() as MainActivity)
        val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)
        if (toolbar != null)
            toolbar.navigationIcon = null

        val userRatingDialog = Dialog(requireContext())
        val tradeDetailsDialog = Dialog(requireContext())
        buildTradeDetailsDialog(tradeDetailsDialog)

        // tried to pass userInfo from login activity to this activity (MainActivity)
        // but when trying to access it here in home fragment, error saying it has not
        // been initialized. So retrieve user info data here and set activity's user info.
        // Also, now that we have the user info, we can retrieve and observe the main feed
        // collectibles and filter them based on userInfo
        viewModel.getUserInfo(activity.currentUser!!.uid)
        viewModel.userInfoLiveData.observe(viewLifecycleOwner) {
            if (it == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_getting_user_info),
                    Toast.LENGTH_SHORT
                ).show()
                return@observe
            }
            activity.userInfo = it
            setupFeedObserversAndLoadDataIntoRecyclerView()
        }
        var isList1Empty: Boolean? = null
        var isList2Empty: Boolean? = null

        viewModel.mainFeedCollectiblesNearLiveData.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            isList1Empty = it.isEmpty()
            if (isList2Empty != null) {
                if (isList2Empty as Boolean && isList1Empty as Boolean)
                    binding.tvNoResults.visibility = View.VISIBLE
                else
                    binding.tvNoResults.visibility = View.GONE
            }
        }

        viewModel.mainFeedCollectiblesFarLiveData.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            isList2Empty = it.isEmpty()
            if (isList1Empty != null) {
                if (isList2Empty as Boolean && isList1Empty as Boolean)
                    binding.tvNoResults.visibility = View.VISIBLE
                else
                    binding.tvNoResults.visibility = View.GONE
            }
        }

        viewModel.showDistanceLabel.observe(viewLifecycleOwner) {
            val downArrow =
                requireContext().resources.getDrawable(R.drawable.ic_baseline_arrow_downward_24)
            binding.tvOutOfDistanceLabel.setCompoundDrawablesWithIntrinsicBounds(
                downArrow,
                null,
                downArrow,
                null
            )
            if (!activity.userInfo.isLocationSet) {
                binding.tvWithinDistanceLabel.visibility = View.GONE
                binding.tvOutOfDistanceLabel.visibility = View.VISIBLE
                binding.tvOutOfDistanceLabel.text = getString(R.string.location_not_set_toast)
                binding.tvOutOfDistanceLabel.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    null,
                    null
                )
                return@observe
            }
            //both true
            if (it.first && it.second) {
                binding.tvWithinDistanceLabel.visibility = View.VISIBLE
                binding.ivDistanceBorder.visibility = View.VISIBLE
                binding.tvOutOfDistanceLabel.visibility = View.VISIBLE
            }
            //1 false 2 true
            else if (!it.first && it.second) {
                binding.tvOutOfDistanceLabel.visibility = View.VISIBLE
            }

            //2 false 1 true
            else if (it.first && !it.second) {
                binding.ivDistanceBorder.visibility = View.VISIBLE
                binding.tvOutOfDistanceLabel.text = getString(R.string.within_search_distance)
            }
        }

        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setCancelable(false)
        progressDialog.setTitle("Updating acceptance...")
        //observe user trade messages and check if any were accepted that the user hasn't seen.
        //if so, show the trade details to the user
        viewModel.checkForAcceptedTrades(activity.currentUser.uid)
        viewModel.checkForAcceptedTradeLiveData.observe(viewLifecycleOwner){ tradeMessage ->
                if (tradeMessage == null) return@observe

                tradeDetailsDialog.show()

                val acceptedTradeSenderAdapter = CollectibleAdapter(
                    tradeMessage.trade!!.senderCollectibles,
                    activity.userInfo,
                    this,
                    this,
                    false,
                    false
                )
                val acceptedTradeSenderRV =
                    tradeDetailsDialog.findViewById<RecyclerView>(R.id.this_user_collectibles_trade_recycler)
                acceptedTradeSenderRV.adapter = acceptedTradeSenderAdapter

                val acceptedTradeReceiverAdapter = CollectibleAdapter(
                    tradeMessage.trade.receiverCollectibles,
                    activity.userInfo,
                    this,
                    this,
                    false,
                    false
                )
                val acceptedTradeReceiverRV =
                    tradeDetailsDialog.findViewById<RecyclerView>(R.id.other_user_collectibles_trade_recycler)
                acceptedTradeReceiverRV.adapter = acceptedTradeReceiverAdapter

                val btnCancelTrade =
                    tradeDetailsDialog.findViewById<MaterialButton>(R.id.btn_cancel)
                btnCancelTrade.isEnabled = false
                btnCancelTrade.visibility = View.GONE
                val btnOk =
                    tradeDetailsDialog.findViewById<MaterialButton>(R.id.btn_confirm)
                btnOk.alpha = 1f
                btnOk.isEnabled = true
                btnOk.text = getString(R.string.ok)
                btnOk.setOnClickListener {
                    tradeMessage.tradeAcceptanceReceived = true
                    viewModel.updateTradeAcceptanceReceived(
                        tradeMessage.senderId,
                        tradeMessage.recipientId,
                        tradeMessage,
                        true
                    )
                    progressDialog.show()
                }
                return@observe

            }

        // observe updating tradeAcceptanceRecieved so that user is prompted
        // to rate other user by showing rateUserDialog to user.
        viewModel.isTradeStatusAcceptanceReceivedUpdatedLiveData.observe(viewLifecycleOwner) { otherUserInfo ->
            if (otherUserInfo == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_updating_trade_acceptance),
                    Toast.LENGTH_SHORT
                ).show()
                return@observe
            }

            tradeDetailsDialog.dismiss()
            progressDialog.dismiss()

            setUserRatingAndSubmit(progressDialog, userRatingDialog, otherUserInfo)
        }

        //observe where or not userInfo was updated successfully after rating user
        viewModel.isUserRatedSuccessfully.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.user_rating_failed),
                    Toast.LENGTH_SHORT
                ).show()
                return@Observer
            }

            Toast.makeText(
                requireContext(),
                getString(R.string.user_rated_successfully),
                Toast.LENGTH_SHORT
            ).show()
            userRatingDialog.dismiss()
            progressDialog.dismiss()
        })

        val myTags = ArrayList<String>()
        binding.selectTagsTv.setOnClickListener(View.OnClickListener { // Initialize alert dialog
            buildSelectTagsDialog(activity.tagsArray, myTags)
        })

        val filterConditions = ArrayList<String>()
        binding.selectConditionsTv.setOnClickListener(View.OnClickListener { // Initialize alert dialog
            buildSelectConditionDialog(filterConditions, activity.conditions)
        })

        // search feed collectibles call for data which will be observed above
        binding.ivSearch.setOnClickListener {
            val searchValue = binding.editText.text.toString()
            if (searchValue.length > 40)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.search_value_reqs),
                    Toast.LENGTH_SHORT
                )
                    .show()
            else
                viewModel.getMainFeedCollectibles(
                    activity.userInfo,
                    true,
                    searchValue,
                    myTags,
                    filterConditions
                )
        }

        return binding.root
    }

    private fun setupFeedObserversAndLoadDataIntoRecyclerView() {
        val activity = (requireActivity() as MainActivity)

        var feedAdapterNear =
            CollectibleAdapter(arrayListOf(), activity.userInfo, this, this, false, true)
        binding.collectiblesRecyclerNear.adapter = feedAdapterNear
        binding.collectiblesRecyclerNear.isNestedScrollingEnabled = false
        var feedAdapterFar =
            CollectibleAdapter(arrayListOf(), activity.userInfo, this, this, false, true)
        binding.collectiblesRecyclerFar.adapter = feedAdapterFar
        binding.collectiblesRecyclerFar.isNestedScrollingEnabled = false

        viewModel.getMainFeedCollectibles(activity.userInfo, false, null, null, null)
    }

    private fun setUserRatingAndSubmit(progressDialog: ProgressDialog, userRatingDialog: Dialog, tradeReceiversUserInfo: UserInfo) {
        progressDialog.setTitle("Submitting user rating...")

        buildRateUserDialog(userRatingDialog)

        val ratingbar = userRatingDialog.findViewById<RatingBar>(R.id.rating)

        val btnOk =
            userRatingDialog.findViewById<MaterialButton>(R.id.btn_submit)
        btnOk?.setOnClickListener {
            val ratingStarsGiven = ratingbar.rating
            tradeReceiversUserInfo.totalRatingStars += ratingStarsGiven
            tradeReceiversUserInfo.totalRates += 1
            tradeReceiversUserInfo.rating =
                tradeReceiversUserInfo.totalRatingStars / tradeReceiversUserInfo.totalRates
            viewModel.rateUser(tradeReceiversUserInfo)
            progressDialog.show()
        }
    }

    private fun buildRateUserDialog(rateUserDialog: Dialog) {

        rateUserDialog.setContentView(R.layout.dialog_rate_user)
        rateUserDialog.setCancelable(false)
        if (rateUserDialog.getWindow() != null) {
            val configuration: Configuration =
                (requireActivity() as MainActivity).getResources().getConfiguration()
            if (configuration.smallestScreenWidthDp > 600)
                rateUserDialog.window!!.setLayout(1800, 2000)
            else if (configuration.smallestScreenWidthDp <= 600 && configuration.smallestScreenWidthDp >= 380)
                rateUserDialog.window!!.setLayout(1080, 1700)
            else rateUserDialog.window!!.setLayout(480, 700)
        }

        rateUserDialog.show()

    }

    private fun buildTradeDetailsDialog(tradeDetailsDialog: Dialog) {

        tradeDetailsDialog.setContentView(R.layout.dialog_trade_details)
        tradeDetailsDialog.setCancelable(false)
        if (tradeDetailsDialog.getWindow() != null) {
            val configuration: Configuration =
                (requireActivity() as MainActivity).getResources().getConfiguration()
            if (configuration.smallestScreenWidthDp > 600)
                tradeDetailsDialog.window!!.setLayout(1800, 2000)
            else if (configuration.smallestScreenWidthDp <= 600 && configuration.smallestScreenWidthDp >= 380)
                tradeDetailsDialog.window!!.setLayout(1080, 1700)
            else tradeDetailsDialog.window!!.setLayout(480, 700)
        }

        tradeDetailsDialog.findViewById<TextView>(R.id.tv_tradeDetailsTitle).text =
            getString(R.string.trade_accepted)
    }

    private fun buildSelectConditionDialog(
        filterConditions: ArrayList<String>,
        conditions: Array<String?>
    ) {


        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.select_condition))
        builder.setCancelable(false)
        // initially all tags are unchecked

        val selectedConditions = BooleanArray(conditions.size)
        for (i in selectedConditions.indices) selectedConditions[i] = false
        val conditionsList: ArrayList<Int> = ArrayList()

        builder.setMultiChoiceItems(
            conditions, selectedConditions,
            DialogInterface.OnMultiChoiceClickListener { dialogInterface, i, b ->
                // check condition
                if (b) {
                    // when checkbox selected
                    // Add position  in lang list
                    conditionsList.add(i)
                    // Sort array list
                    conditionsList.sort()
                } else {
                    // when checkbox unselected
                    // Remove position from langList
                    conditionsList.remove(Integer.valueOf(i))
                }
            })

        builder.setPositiveButton(getString(R.string.ok),
            DialogInterface.OnClickListener { dialogInterface, i -> // Initialize string builder
                val stringBuilder = StringBuilder()

                for (j in 0 until conditionsList.size) {

                    // concat array value
                    stringBuilder.append(conditions.get(conditionsList.get(j)))
                    // check condition
                    if (j != conditionsList.size - 1) {
                        // When j value  not equal
                        // to lang list size - 1
                        // add comma
                        stringBuilder.append(", ")
                    }
                }
                filterConditions.add(stringBuilder.toString())
                binding.selectConditionsTv.text = stringBuilder.toString()
            })
        builder.setNegativeButton(getString(R.string.cancel),
            DialogInterface.OnClickListener { dialogInterface, i -> // dismiss dialog
                dialogInterface.dismiss()
            })
        builder.setNeutralButton(getString(R.string.clear_all),
            DialogInterface.OnClickListener { dialogInterface, i ->

                for (j in 0 until selectedConditions.size) {
                    // remove all selection
                    selectedConditions[j] = false
                    // clear language list
                    conditionsList.clear()
                    filterConditions.clear()
                    // clear text view value
                    binding.selectConditionsTv.text = ""
                }
            })

        val dialog: AlertDialog = builder.create()
        if (!dialog.isShowing) dialog.show()
    }

    private fun buildSelectTagsDialog(tagsArray: Array<String?>, myTags: ArrayList<String>) {

        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.select_categories))
        builder.setCancelable(false)
        var dialog: AlertDialog
        val selectedTags = BooleanArray(tagsArray.size)
        // initially all tags are unchecked
        for (i in selectedTags.indices) selectedTags[i] = false

        val tagsList: ArrayList<Int> = ArrayList()

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

        builder.setPositiveButton(getString(R.string.ok),
            DialogInterface.OnClickListener { dialogInterface, i -> // Initialize string builder
                val stringBuilder = StringBuilder()

                for (j in 0 until tagsList.size) {

                    // concat array value
                    stringBuilder.append(tagsArray.get(tagsList.get(j)))
                    myTags.add(tagsArray.get(tagsList.get(j))!!)
                    // check condition
                    if (j != tagsList.size - 1) {
                        // When j value  not equal
                        // to lang list size - 1
                        // add comma
                        stringBuilder.append(", ")
                    }
                }
                binding.selectTagsTv.text = stringBuilder.toString()
            })
        builder.setNegativeButton(getString(R.string.cancel),
            DialogInterface.OnClickListener { dialogInterface, i -> // dismiss dialog
                dialogInterface.dismiss()
            })
        builder.setNeutralButton(getString(R.string.clear_all),
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

    override fun onItemClick(position: Int, collectible: Collectible, isChecked: Boolean) {

        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToCollectibleDetailsFragment(
                collectible
            )
        )
    }

    override fun onFavoriteClick(position: Int, collectible: Collectible) {
        val activity = (requireActivity() as MainActivity)

        if (activity.userInfo.favoriteCollectibles.contains(collectible))
            activity.userInfo.favoriteCollectibles.remove(collectible)
        else
            activity.userInfo.favoriteCollectibles.add(collectible)

        viewModel.updateProfile(activity.userInfo, null)
    }
}