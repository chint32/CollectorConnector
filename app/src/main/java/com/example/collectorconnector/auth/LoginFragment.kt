package com.example.collectorconnector.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
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
import com.example.collectorconnector.main.MainActivity
import com.example.collectorconnector.R
import com.example.collectorconnector.databinding.FragmentLoginBinding
import com.example.collectorconnector.models.UserInfo
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import java.io.FileOutputStream


class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        val activity = (requireActivity() as LoginActivity)

        activity.viewModel.authenticatedUserLiveData.observe(viewLifecycleOwner, Observer{
            if (it == null || it.user == null){
                binding.progressBar.visibility = View.GONE
                return@Observer
            }

            binding.progressBar.visibility = View.GONE
            activity.viewModel.getUserInfo(it.user!!.uid)

        })

        binding.tvRegHere.setOnClickListener{
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToRegistrationFragment())
        }

        binding.btnLogin.setOnClickListener{
            if(binding.etLoginEmail.text.isNullOrEmpty()){
                Toast.makeText(requireContext(), "Email must not be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(binding.etLoginPass.text.isNullOrEmpty()){
                Toast.makeText(requireContext(), "Password must not be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.progressBar.visibility = View.VISIBLE
            activity.viewModel.signInWithEmailAndPw(binding.etLoginEmail.text.toString(), binding.etLoginPass.text.toString())
        }

        return binding.root
    }
}