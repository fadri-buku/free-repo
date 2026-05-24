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
import com.bukuwarung.paymentsadapter.databinding.FragmentCancelPaymentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CancelPaymentFragment : Fragment() {

    private var _binding: FragmentCancelPaymentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CancelPaymentViewModel by viewModels()
    private val args: CancelPaymentFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCancelPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvReferenceNo.text = args.referenceNo
        binding.btnCancel.setOnClickListener {
            viewModel.cancel(
                referenceNo = args.referenceNo,
                reason = binding.etReason.text.toString().ifBlank { "customer requested cancellation" }
            )
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: CancelPaymentUiState) {
        binding.progressBar.visibility = if (state is CancelPaymentUiState.Loading) View.VISIBLE else View.GONE
        binding.btnCancel.isEnabled = state !is CancelPaymentUiState.Loading
        when (state) {
            is CancelPaymentUiState.Success -> {
                binding.tvResult.text = "Status: ${state.result.status}"
                binding.tvError.visibility = View.GONE
                binding.btnCancel.isEnabled = false
            }
            is CancelPaymentUiState.Error -> {
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
