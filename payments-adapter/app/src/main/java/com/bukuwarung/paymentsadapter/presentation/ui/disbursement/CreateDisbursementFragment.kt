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
import androidx.navigation.fragment.navArgs
import com.bukuwarung.paymentsadapter.R
import com.bukuwarung.paymentsadapter.databinding.FragmentCreateDisbursementBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateDisbursementFragment : Fragment() {

    private var _binding: FragmentCreateDisbursementBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreateDisbursementViewModel by viewModels()
    private val args: CreateDisbursementFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateDisbursementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etBeneficiaryAccount.setText(args.beneficiaryAccountNo)
        binding.etBankCode.setText(args.beneficiaryBankCode)
        binding.etSourceAccount.setText(args.sourceAccountNo)

        binding.btnCreate.setOnClickListener {
            viewModel.create(
                partnerReferenceNo = binding.etPartnerRef.text.toString(),
                amount = binding.etAmount.text.toString(),
                beneficiaryAccountNo = binding.etBeneficiaryAccount.text.toString(),
                beneficiaryBankCode = binding.etBankCode.text.toString(),
                beneficiaryName = binding.etBeneficiaryName.text.toString(),
                sourceAccountNo = binding.etSourceAccount.text.toString(),
                channelId = binding.etChannelId.text.toString(),
                remark = binding.etRemark.text.toString(),
                inquiryKey = args.inquiryKey.ifBlank { null }
            )
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: CreateDisbursementUiState) {
        binding.progressBar.visibility = if (state is CreateDisbursementUiState.Loading) View.VISIBLE else View.GONE
        binding.btnCreate.isEnabled = state !is CreateDisbursementUiState.Loading
        when (state) {
            is CreateDisbursementUiState.Success -> {
                val d = state.disbursement
                binding.tvResult.text = buildString {
                    appendLine("Ref: ${d.referenceNo}")
                    appendLine("Status: ${d.status}")
                    appendLine("Provider: ${d.provider}")
                    appendLine("Amount: ${d.amount} ${d.currency}")
                    d.estimatedSettlementAt?.let { appendLine("Est. settlement: $it") }
                }
                binding.tvError.visibility = View.GONE
                if (!d.status.isTerminal) {
                    binding.btnCheckStatus.visibility = View.VISIBLE
                    binding.btnCheckStatus.setOnClickListener {
                        findNavController().navigate(
                            CreateDisbursementFragmentDirections
                                .actionCreateDisbursementToStatus(d.referenceNo)
                        )
                    }
                }
            }
            is CreateDisbursementUiState.Error -> {
                binding.tvError.text = state.message
                binding.tvError.visibility = View.VISIBLE
                binding.tvResult.text = ""
            }
            else -> Unit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
