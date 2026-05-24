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
import androidx.navigation.fragment.navArgs
import com.bukuwarung.paymentsadapter.databinding.FragmentPaymentStatusBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PaymentStatusFragment : Fragment() {

    private var _binding: FragmentPaymentStatusBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PaymentStatusViewModel by viewModels()
    private val args: PaymentStatusFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaymentStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvReferenceNo.text = args.referenceNo
        binding.btnRefresh.setOnClickListener { viewModel.fetchStatus(args.referenceNo) }
        viewModel.fetchStatus(args.referenceNo)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: PaymentStatusUiState) {
        binding.progressBar.visibility = if (state is PaymentStatusUiState.Loading) View.VISIBLE else View.GONE
        when (state) {
            is PaymentStatusUiState.Success -> {
                val p = state.payment
                binding.tvResult.text = buildString {
                    appendLine("Status: ${p.status}")
                    appendLine("Amount: ${p.amount} ${p.currency}")
                    appendLine("Provider: ${p.provider}")
                    appendLine("Occurred: ${p.occurredAt}")
                    p.completedAt?.let { appendLine("Completed: $it") }
                }
                binding.tvError.visibility = View.GONE
            }
            is PaymentStatusUiState.Error -> {
                binding.tvError.text = state.message
                binding.tvError.visibility = View.VISIBLE
            }
            else -> Unit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
