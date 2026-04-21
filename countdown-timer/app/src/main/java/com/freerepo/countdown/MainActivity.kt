package com.freerepo.countdown

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.freerepo.countdown.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CountdownViewModel by viewModels()

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        maybeRequestNotificationPermission()
        wireInputs()
        observeState()
    }

    private fun wireInputs() {
        binding.startButton.setOnClickListener {
            val durationMs = readDurationMs() ?: return@setOnClickListener
            if (durationMs == 0L) {
                Toast.makeText(this, R.string.error_zero_duration, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            hideKeyboard()
            viewModel.start(durationMs)
        }

        binding.pauseResumeButton.setOnClickListener {
            when (viewModel.state.value) {
                CountdownLogic.State.RUNNING -> viewModel.pause()
                CountdownLogic.State.PAUSED -> viewModel.resume()
                else -> Unit
            }
        }

        binding.resetButton.setOnClickListener {
            viewModel.reset()
        }
    }

    private fun observeState() {
        viewModel.remainingMs.observe(this) { ms ->
            binding.timeDisplay.text = TimeFormatter.format(ms)
        }

        viewModel.state.observe(this) { state ->
            renderControls(state)
        }

        viewModel.finishedEvent.observe(this) { finished ->
            if (finished == true) {
                CountdownNotifier.notifyFinished(this)
                viewModel.consumeFinishedEvent()
            }
        }
    }

    private fun renderControls(state: CountdownLogic.State) {
        when (state) {
            CountdownLogic.State.IDLE -> {
                binding.stateLabel.setText(R.string.state_idle)
                binding.startButton.isEnabled = true
                binding.pauseResumeButton.isEnabled = false
                binding.pauseResumeButton.setText(R.string.button_pause)
                binding.resetButton.isEnabled = false
                setInputsEnabled(true)
            }
            CountdownLogic.State.RUNNING -> {
                binding.stateLabel.setText(R.string.state_running)
                binding.startButton.isEnabled = false
                binding.pauseResumeButton.isEnabled = true
                binding.pauseResumeButton.setText(R.string.button_pause)
                binding.resetButton.isEnabled = true
                setInputsEnabled(false)
            }
            CountdownLogic.State.PAUSED -> {
                binding.stateLabel.setText(R.string.state_paused)
                binding.startButton.isEnabled = false
                binding.pauseResumeButton.isEnabled = true
                binding.pauseResumeButton.setText(R.string.button_resume)
                binding.resetButton.isEnabled = true
                setInputsEnabled(false)
            }
            CountdownLogic.State.FINISHED -> {
                binding.stateLabel.setText(R.string.state_finished)
                binding.startButton.isEnabled = true
                binding.pauseResumeButton.isEnabled = false
                binding.pauseResumeButton.setText(R.string.button_pause)
                binding.resetButton.isEnabled = true
                setInputsEnabled(true)
            }
            null -> Unit
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.hoursInput.isEnabled = enabled
        binding.minutesInput.isEnabled = enabled
        binding.secondsInput.isEnabled = enabled
    }

    private fun readDurationMs(): Long? {
        val hours = parseField(binding.hoursInput.text?.toString()) ?: return invalid()
        val minutes = parseField(binding.minutesInput.text?.toString()) ?: return invalid()
        val seconds = parseField(binding.secondsInput.text?.toString()) ?: return invalid()
        if (minutes >= 60 || seconds >= 60) return invalid()
        val totalSeconds = hours * 3600L + minutes * 60L + seconds
        return totalSeconds * 1000L
    }

    private fun parseField(raw: String?): Long? {
        if (raw.isNullOrBlank()) return 0L
        return raw.toLongOrNull()?.takeIf { it >= 0 }
    }

    private fun invalid(): Long? {
        Toast.makeText(this, R.string.error_invalid_input, Toast.LENGTH_SHORT).show()
        return null
    }

    private fun hideKeyboard() {
        val focused: View = currentFocus ?: binding.root
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(focused.windowToken, 0)
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val already = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!already) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
