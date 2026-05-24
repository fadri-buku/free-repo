package com.bukuwarung.paymentsadapter.presentation.ui.disbursement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bukuwarung.paymentsadapter.R
import com.bukuwarung.paymentsadapter.databinding.FragmentAccountInquiryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountInquiryFragment : Fragment() {

    private var _binding: FragmentAccountInquiryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AccountInquiryViewModel by viewModels()
    private var lastInquiryKey: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountInquiryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnInquire.setOnClickListener {
            viewModel.inquire(
                beneficiaryAccountNo = binding.etBeneficiaryAccount.text.toString(),
                beneficiaryBankCode = binding.etBankCode.text.toString(),
                sourceAccountNo = binding.etSourceAccount.text.toString()
            )
        }
        binding.btnProceedToDisbursement.setOnClickListener {
            val action = AccountInquiryFragmentDirections
                .actionAccountInquiryToCreateDisbursement(
                    beneficiaryAccountNo = binding.etBeneficiaryAccount.text.toString(),
                    beneficiaryBankCode = binding.etBankCode.text.toString(),
                    sourceAccountNo = binding.etSourceAccount.text.toString(),
                    inquiryKey = lastInquiryKey ?: ""
                )
            findNavController().navigate(action)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: AccountInquiryUiState) {
        binding.progressBar.visibility = if (state is AccountInquiryUiState.Loading) View.VISIBLE else View.GONE
        binding.btnInquire.isEnabled = state !is AccountInquiryUiState.Loading
        when (state) {
            is AccountInquiryUiState.Success -> {
                lastInquiryKey = state.inquiry.inquiryKey
                binding.tvResult.text = buildString {
                    appendLine("Name: ${state.inquiry.beneficiaryName}")
                    appendLine("Account: ${state.inquiry.beneficiaryAccountNo}")
                    appendLine("Status: ${state.inquiry.beneficiaryAccountStatus}")
                    appendLine("Inquiry Key: ${state.inquiry.inquiryKey}")
                    appendLine("Expires: ${state.inquiry.inquiryExpiresAt}")
                }
                binding.tvError.visibility = View.GONE
                binding.btnProceedToDisbursement.visibility = View.VISIBLE
            }
            is AccountInquiryUiState.Error -> {
                binding.tvError.text = state.message
                binding.tvError.visibility = View.VISIBLE
                binding.tvResult.text = ""
                binding.btnProceedToDisbursement.visibility = View.GONE
            }
            else -> Unit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
