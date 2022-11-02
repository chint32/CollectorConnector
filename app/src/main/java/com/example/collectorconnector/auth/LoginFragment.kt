package com.example.collectorconnector.auth

import android.app.ProgressDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.collectorconnector.R
import com.example.collectorconnector.databinding.FragmentLoginBinding
import com.example.collectorconnector.main.MainActivity
import com.google.firebase.auth.FirebaseAuth


class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    val currUser = FirebaseAuth.getInstance().currentUser
    val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel


        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setCancelable(false)
        progressDialog.setTitle(getString(R.string.signing_in))

        viewModel.showProgressBar.observe(viewLifecycleOwner){
            if(it) progressDialog.show()
        }

        viewModel.authenticatedUserLiveData.observe(viewLifecycleOwner, Observer {
            progressDialog.dismiss()
            if (it == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.incorrect_email_toast),
                    Toast.LENGTH_SHORT
                ).show()
                return@Observer
            } else if (it.user == null) {
                Toast.makeText(requireContext(), getString(R.string.error_logging_in), Toast.LENGTH_SHORT).show()
                return@Observer
            }

            viewModel.getUserInfo(it.user!!.uid)
        })

        viewModel.userInfoLiveData.observe(viewLifecycleOwner, Observer {
            progressDialog.dismiss()
            if (it == null) {
                Toast.makeText(requireContext(), getString(R.string.error_logging_in), Toast.LENGTH_SHORT).show()
                return@Observer
            }
            viewModel.userInfo = it

            Toast.makeText(requireContext(), "${viewModel.userInfo.screenName} logged in", Toast.LENGTH_SHORT)
                .show()
            startMainActivity()
        })

        binding.btnLogin.setOnClickListener {
            progressDialog.show()
        }

        binding.tvRegHere.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToRegistrationFragment())
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (currUser != null)
            ViewModelProvider(this).get(AuthViewModel::class.java).getUserInfo(currUser.uid)
    }

    private fun startMainActivity() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra("user_info", viewModel.userInfo)
        intent.putExtra("categories", (requireActivity() as LoginActivity).tagsArray)
        intent.putExtra("conditions", (requireActivity() as LoginActivity).conditions)
        startActivity(intent)
    }

}