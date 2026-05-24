package com.bukuwarung.paymentsadapter.presentation.ui.payment

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
import com.bukuwarung.paymentsadapter.databinding.FragmentCreatePaymentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreatePaymentFragment : Fragment() {

    private var _binding: FragmentCreatePaymentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreatePaymentViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreatePaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCreate.setOnClickListener {
            viewModel.create(
                partnerReferenceNo = binding.etPartnerRef.text.toString(),
                amount = binding.etAmount.text.toString(),
                channelId = binding.etChannelId.text.toString(),
                payerId = binding.etPayerId.text.toString(),
                payerAccountNo = binding.etPayerAccount.text.toString(),
                expiredAt = binding.etExpiredAt.text.toString(),
                orderId = binding.etOrderId.text.toString()
            )
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: CreatePaymentUiState) {
        binding.progressBar.visibility = if (state is CreatePaymentUiState.Loading) View.VISIBLE else View.GONE
        binding.btnCreate.isEnabled = state !is CreatePaymentUiState.Loading
        when (state) {
            is CreatePaymentUiState.Success -> {
                val p = state.payment
                binding.tvResult.text = buildString {
                    appendLine("Ref: ${p.referenceNo}")
                    appendLine("Status: ${p.status}")
                    appendLine("Provider: ${p.provider}")
                    appendLine("Amount: ${p.amount} ${p.currency}")
                    p.paymentUrl?.let { appendLine("URL: $it") }
                    p.expiredAt?.let { appendLine("Expires: $it") }
                }
                binding.tvError.visibility = View.GONE
                binding.btnCheckStatus.visibility = View.VISIBLE
                binding.btnCheckStatus.setOnClickListener {
                    findNavController().navigate(
                        CreatePaymentFragmentDirections.actionCreatePaymentToStatus(p.referenceNo)
                    )
                }
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnCancel.setOnClickListener {
                    findNavController().navigate(
                        CreatePaymentFragmentDirections.actionCreatePaymentToCancel(p.referenceNo)
                    )
                }
            }
            is CreatePaymentUiState.Error -> {
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
