package com.arduinoworld.smartremote

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import com.arduinoworld.smartremote.databinding.ActivitySettingsBinding

@Suppress("DEPRECATION")
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySettingsBinding
    private lateinit var editPreferences : SharedPreferences.Editor

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSettings)
        supportActionBar!!.title = "Настройки"

        val sharedPreferences = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        editPreferences = sharedPreferences.edit()

        if (!sharedPreferences.getBoolean("HapticFeedbackEnabled", true)) {
            binding.radioGroupVibrationTypes.check(R.id.radioButtonNormalVibration)
        } else {
            binding.radioGroupVibrationTypes.check(R.id.radioButtonHapticFeedback)
        }

        binding.inputBrightnessSteps.setText(sharedPreferences.getInt("ChangeBrightnessSteps", 10).toString())
        binding.inputTemperatureColorSteps.setText(sharedPreferences.getInt("ChangeColorTemperatureSteps", 20).toString())
        binding.inputMinTemperatureColor.setText(sharedPreferences.getInt("MinColorTemperature", 3000).toString())
        binding.inputMaxTemperatureColor.setText(sharedPreferences.getInt("MaxColorTemperature", 6500).toString())
        binding.inputMinAcTemperature.setText(sharedPreferences.getInt("MinTemperature", 16).toString())
        binding.inputMaxAcTemperature.setText(sharedPreferences.getInt("MaxTemperature", 30).toString())

        binding.radioGroupVibrationTypes.setOnCheckedChangeListener { _, checkedId ->
            vibrate()
            when (checkedId) {
                R.id.radioButtonNormalVibration -> {
                    editPreferences.putBoolean("HapticFeedbackEnabled", false).apply()
                }
                R.id.radioButtonHapticFeedback -> {
                    editPreferences.putBoolean("HapticFeedbackEnabled", true).apply()
                }
            }
        }

        binding.buttonDefaultSettings.setOnClickListener {
            vibrate()
            binding.radioGroupVibrationTypes.check(R.id.radioButtonHapticFeedback)
            binding.inputBrightnessSteps.setText("10")
            binding.inputTemperatureColorSteps.setText("20")
            binding.inputMinTemperatureColor.setText("3000")
            binding.inputMaxTemperatureColor.setText("6500")
            binding.inputMinAcTemperature.setText("16")
            binding.inputMaxAcTemperature.setText("30")
        }
    }

    override fun onPause() {
        super.onPause()
        editPreferences.putInt("ChangeBrightnessSteps", binding.inputBrightnessSteps.text!!.toString().toInt())
        editPreferences.putInt("ChangeColorTemperatureSteps", binding.inputTemperatureColorSteps.text!!.toString().toInt())
        editPreferences.putInt("MinColorTemperature", binding.inputMinTemperatureColor.text!!.toString().toInt())
        editPreferences.putInt("MaxColorTemperature", binding.inputMaxTemperatureColor.text!!.toString().toInt())
        editPreferences.putInt("MinTemperature", binding.inputMinAcTemperature.text!!.toString().toInt())
        editPreferences.putInt("MaxTemperature", binding.inputMaxAcTemperature.text!!.toString().toInt()).apply()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val actionButtonsInflater = menuInflater
        actionButtonsInflater.inflate(R.menu.settings_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.buttonHomeScreen3 -> {
                vibrate()
                val activity = Intent(this, MainActivity::class.java)
                startActivity(activity)
                true
            }
            R.id.buttonUserProfile3 -> {
                vibrate()
                val activity = Intent(this, UserProfile::class.java)
                startActivity(activity)
                true
            }
            else->super.onOptionsItemSelected(item)
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (binding.radioButtonHapticFeedback.isChecked) {
                binding.buttonDefaultSettings.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(20)
                }
            }
        }
    }
}