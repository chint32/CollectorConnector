package com.example.collectorconnector.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.collectorconnector.R
import com.example.collectorconnector.databinding.FragmentRegistrationBinding


class RegistrationFragment : Fragment() {

    private lateinit var binding: FragmentRegistrationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_registration, container, false)

        binding.btnRegister.setOnClickListener{
            if(binding.etRegEmail.text.isNullOrEmpty()){
                Toast.makeText(requireContext(), "Email must not be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(binding.etRegPass.text.isNullOrEmpty()){
                Toast.makeText(requireContext(), "Password must not be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            findNavController().navigate(RegistrationFragmentDirections
                .actionRegistrationFragmentToQuestionnaireFragment(
                    binding.etRegEmail.text.toString(), binding.etRegPass.text.toString()
                )
            )

        }

        binding.tvRegHere.setOnClickListener{
            findNavController().navigate(RegistrationFragmentDirections.actionRegistrationFragmentToLoginFragment())
        }

        return binding.root
    }
}