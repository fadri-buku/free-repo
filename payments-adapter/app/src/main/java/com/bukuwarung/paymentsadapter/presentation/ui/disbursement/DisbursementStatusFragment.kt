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
import androidx.navigation.fragment.navArgs
import com.bukuwarung.paymentsadapter.databinding.FragmentDisbursementStatusBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DisbursementStatusFragment : Fragment() {

    private var _binding: FragmentDisbursementStatusBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DisbursementStatusViewModel by viewModels()
    private val args: DisbursementStatusFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDisbursementStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvReferenceNo.text = args.referenceNo
        binding.btnRefresh.setOnClickListener { viewModel.fetchStatus(args.referenceNo) }
        binding.btnPollUntilDone.setOnClickListener { viewModel.pollUntilTerminal(args.referenceNo) }
        viewModel.fetchStatus(args.referenceNo)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: DisbursementStatusUiState) {
        binding.progressBar.visibility = if (state is DisbursementStatusUiState.Loading) View.VISIBLE else View.GONE
        when (state) {
            is DisbursementStatusUiState.Success -> {
                val d = state.disbursement
                binding.tvResult.text = buildString {
                    appendLine("Status: ${d.status}")
                    appendLine("Amount: ${d.amount} ${d.currency}")
                    appendLine("Beneficiary: ${d.beneficiaryName} (${d.beneficiaryAccountNo})")
                    appendLine("Provider: ${d.provider}")
                    appendLine("Occurred: ${d.occurredAt}")
                    d.settledAt?.let { appendLine("Settled: $it") }
                }
                binding.tvError.visibility = View.GONE
                binding.chipTerminal.visibility = if (d.status.isTerminal) View.VISIBLE else View.GONE
            }
            is DisbursementStatusUiState.Error -> {
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
