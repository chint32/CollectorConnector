package com.example.collectorconnector.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
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
            if(binding.etRegEmail.text.length < 6 || binding.etRegEmail.text.length > 30){
                Toast.makeText(requireContext(), requireContext().getString(R.string.email_req_toast), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(binding.etRegPass.text.length < 4 || binding.etRegPass.text.length > 20){
                Toast.makeText(requireContext(), requireContext().getString(R.string.pw_req_toast), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            findNavController().navigate(RegistrationFragmentDirections
                .actionRegistrationFragmentToQuestionnaireFragment(binding.etRegEmail.text.toString(), binding.etRegPass.text.toString())
            )
        }

        binding.tvLoginHere.setOnClickListener{
            findNavController().navigate(RegistrationFragmentDirections.actionRegistrationFragmentToLoginFragment())
        }

        return binding.root
    }
}