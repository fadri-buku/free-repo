package com.bukuwarung.paymentsadapter.presentation.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bukuwarung.paymentsadapter.R
import com.bukuwarung.paymentsadapter.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnAuth.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_auth)
        }
        binding.btnDisbursement.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_accountInquiry)
        }
        binding.btnPayment.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_createPayment)
        }
        binding.btnAdmin.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_admin)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
