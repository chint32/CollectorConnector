package com.example.collectorconnector.main

import androidx.fragment.app.Fragment

open class MyFragment : Fragment() {

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).supportActionBar?.title = "Conversations"
    }


}
