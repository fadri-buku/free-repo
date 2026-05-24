package com.bukuwarung.paymentsadapter.presentation.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bukuwarung.paymentsadapter.databinding.FragmentAuthBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnGetToken.setOnClickListener {
            viewModel.getToken(
                clientKey = binding.etClientKey.text.toString(),
                privateKeyPem = binding.etPrivateKey.text.toString()
            )
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: AuthUiState) {
        binding.progressBar.visibility = if (state is AuthUiState.Loading) View.VISIBLE else View.GONE
        binding.btnGetToken.isEnabled = state !is AuthUiState.Loading
        when (state) {
            is AuthUiState.Success -> {
                binding.tvResult.text = buildString {
                    appendLine("Token: ${state.token.accessToken}")
                    appendLine("Type: ${state.token.tokenType}")
                    appendLine("Expires in: ${state.token.expiresIn}s")
                }
                binding.tvError.visibility = View.GONE
            }
            is AuthUiState.Error -> {
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
