package com.bukuwarung.paymentsadapter.presentation.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bukuwarung.paymentsadapter.databinding.FragmentAdminBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnRefresh.setOnClickListener { viewModel.loadAll() }
        binding.btnReloadRouting.setOnClickListener { viewModel.reloadRouting() }
        viewModel.loadAll()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: AdminUiState) {
        binding.progressBar.visibility = if (state is AdminUiState.Loading) View.VISIBLE else View.GONE
        when (state) {
            is AdminUiState.Success -> {
                binding.tvHealth.text = buildString {
                    appendLine("=== Provider Health ===")
                    state.health.providers.forEach { (k, v) -> appendLine("$k: $v") }
                }
                binding.tvFlags.text = buildString {
                    appendLine("=== Feature Flags ===")
                    state.flags.flags.forEach { (k, v) -> appendLine("$k: $v") }
                }
                binding.tvReloadedAt.text = state.routingReloadedAt?.let { "Last reload: $it" } ?: ""
                binding.tvError.visibility = View.GONE
                renderFlagToggles(state)
            }
            is AdminUiState.Error -> {
                binding.tvError.text = state.message
                binding.tvError.visibility = View.VISIBLE
            }
            else -> Unit
        }
    }

    private fun renderFlagToggles(state: AdminUiState.Success) {
        binding.flagTogglesContainer.removeAllViews()
        state.flags.flags.forEach { (key, enabled) ->
            val switch = com.google.android.material.switchmaterial.SwitchMaterial(requireContext()).apply {
                text = key
                isChecked = enabled
                setOnCheckedChangeListener { _, isChecked -> viewModel.toggleFlag(key, isChecked) }
            }
            binding.flagTogglesContainer.addView(switch)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
