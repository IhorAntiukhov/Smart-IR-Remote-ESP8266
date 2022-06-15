package com.arduinoworld.smartremote

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.arduinoworld.smartremote.databinding.ActivityAcRemoteBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Suppress("DEPRECATION")
@SuppressLint("CommitPrefEdits", "SetTextI18n")
class AcRemote : AppCompatActivity() {
    private lateinit var binding : ActivityAcRemoteBinding

    private var isHapticFeedbackEnabled = false
    private var acStarted = false
    private var temperature = 25
    private var mode = 0
    private var fanSpeed = 0
    private var turboMode = false
    private var swingV = false
    private var swingH = false
    private var sleepMode = false
    private var minTemperature = 16
    private var maxTemperature = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAcRemoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarAcRemote)
        supportActionBar!!.title = "Кондиционер"

        val firebaseAuth = FirebaseAuth.getInstance()
        val realtimeDatabase = FirebaseDatabase.getInstance()

        val sharedPreferences = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        val editPreferences = sharedPreferences.edit()

        isHapticFeedbackEnabled = sharedPreferences.getBoolean("HapticFeedbackEnabled", true)
        acStarted = sharedPreferences.getBoolean("AcStarted", false)
        temperature = sharedPreferences.getInt("Temperature", 25)
        mode = sharedPreferences.getInt("Mode", 0)
        fanSpeed = sharedPreferences.getInt("FanSpeed", 0)
        turboMode = sharedPreferences.getBoolean("TurboMode", false)
        swingV = sharedPreferences.getBoolean("SwingV", false)
        swingH = sharedPreferences.getBoolean("SwingH", false)
        sleepMode = sharedPreferences.getBoolean("SleepMode", false)
        minTemperature = sharedPreferences.getInt("MinTemperature", 16)
        maxTemperature = sharedPreferences.getInt("MaxTemperature", 30)

        if (acStarted) {
            binding.buttonStartAc.visibility = View.GONE
            binding.buttonStopAc.visibility = View.VISIBLE
        }
        if (temperature == maxTemperature) {
            binding.buttonIncreaseTemperature.setImageResource(R.drawable.ic_increase_temperature_blocked)
        }
        if (temperature == minTemperature) {
            binding.buttonDecreaseTemperature.setImageResource(R.drawable.ic_decrease_temperature_blocked)
        }
        binding.labelTemperature.text = "$temperature°C"
        when (mode) {
            0 -> {
                binding.buttonMode.setImageResource(R.drawable.ic_fan_mode)
                binding.labelMode.text = "Обдув"
            }
            1 -> {
                binding.buttonMode.setImageResource(R.drawable.ic_cool)
                binding.labelMode.text = "Холод"
            }
            2 -> {
                binding.buttonMode.setImageResource(R.drawable.ic_heat)
                binding.labelMode.text = "Обогрев"
            }
            3 -> {
                binding.buttonMode.setImageResource(R.drawable.ic_dry)
                binding.labelMode.text = "Осушение"
            }
            4 -> {
                binding.buttonMode.setImageResource(R.drawable.ic_auto)
                binding.labelMode.text = "Авто"
                binding.buttonDecreaseTemperature.setImageResource(R.drawable.ic_decrease_temperature_blocked)
                binding.buttonIncreaseTemperature.setImageResource(R.drawable.ic_increase_temperature_blocked)
                binding.labelTemperature.setTextColor(Color.parseColor("#bfe37f"))
            }
        }
        when (fanSpeed) {
            0 -> {
                binding.labelFanSpeed.text = "Слабый"
            }
            1 -> {
                binding.labelFanSpeed.text = "Средний"
            }
            2 -> {
                binding.labelFanSpeed.text = "Сильный"
            }
            3 -> {
                binding.labelFanSpeed.text = "Авто"
            }
        }

        if (turboMode) {
            binding.labelTurboMode.text = "Турбо: вкл"
        } else {
            binding.labelTurboMode.text = "Турбо: выкл"
        }
        if (swingV) {
            binding.labelSwingV.text = "SwingV: вкл"
        } else {
            binding.labelSwingV.text = "SwingV: выкл"
        }
        if (swingH) {
            binding.labelSwingH.text = "SwingH: вкл"
        } else {
            binding.labelSwingH.text = "SwingH: выкл"
        }
        if (sleepMode) {
            binding.labelSleepMode.text = "Режим сна: вкл"
        } else {
            binding.labelSleepMode.text = "Режим сна: выкл"
        }

        binding.buttonDecreaseTemperature.setOnClickListener {
            vibrate()
            if (mode != 4) {
                if (temperature > minTemperature) {
                    if (temperature == maxTemperature)
                        binding.buttonIncreaseTemperature.setImageResource(R.drawable.ic_increase_temperature)
                    temperature -= 1
                    binding.labelTemperature.text = "$temperature°C"
                    if (temperature == minTemperature)
                        binding.buttonDecreaseTemperature.setImageResource(R.drawable.ic_decrease_temperature_blocked)
                    if (acStarted) {
                        realtimeDatabase.reference.child("/users/").child(firebaseAuth.currentUser!!.uid).child("acRemote")
                                .setValue("$temperature#$mode#$fanSpeed#$turboMode#$swingV#$swingH#$sleepMode")
                    }
                    editPreferences.putInt("Temperature", temperature).apply()
                } else {
                    Toast.makeText(this, "Вы установили минимальную температуру!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Вы не можете изменять\nтемпературу в авто режиме!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonIncreaseTemperature.setOnClickListener {
            vibrate()
            if (mode != 4) {
                if (temperature < maxTemperature) {
                    if (temperature == minTemperature)
                        binding.buttonDecreaseTemperature.setImageResource(R.drawable.ic_decrease_temperature)
                    temperature += 1
                    binding.labelTemperature.text = "$temperature°C"
                    if (temperature == maxTemperature)
                        binding.buttonIncreaseTemperature.setImageResource(R.drawable.ic_increase_temperature_blocked)
                    if (acStarted) {
                        realtimeDatabase.reference.child("/users/").child(firebaseAuth.currentUser!!.uid).child("acRemote")
                                .setValue("$temperature#$mode#$fanSpeed#$turboMode#$swingV#$swingH#$sleepMode")
                    }
                    editPreferences.putInt("Temperature", temperature).apply()
                } else {
                    Toast.makeText(this, "Вы установили максимальную температуру!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Вы не можете изменять\nтемпературу в авто режиме!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonMode.setOnClickListener {
            vibrate()
            mode += 1
            if (mode == 5) mode = 0
            when (mode) {
                0 -> {
                    binding.buttonMode.setImageResource(R.drawable.ic_fan_mode)
                    binding.labelMode.text = "Обдув"
                    if (temperature != minTemperature) {
                        binding.buttonDecreaseTemperature.setImageResource(R.drawable.ic_decrease_temperature)
                    }
                    if (temperature != maxTemperature) {
                        binding.buttonIncreaseTemperature.setImageResource(R.drawable.ic_increase_temperature)
                    }
                    binding.labelTemperature.setTextColor(ResourcesCompat.getColor(resources, R.color.primary_color, null))
                }
                1 -> {
                    binding.buttonMode.setImageResource(R.drawable.ic_cool)
                    binding.labelMode.text = "Холод"
                }
                2 -> {
                    binding.buttonMode.setImageResource(R.drawable.ic_heat)
                    binding.labelMode.text = "Обогрев"
                }
                3 -> {
                    binding.buttonMode.setImageResource(R.drawable.ic_dry)
                    binding.labelMode.text = "Осушение"
                }
                4 -> {
                    binding.buttonMode.setImageResource(R.drawable.ic_auto)
                    binding.labelMode.text = "Авто"
                    binding.buttonDecreaseTemperature.setImageResource(R.drawable.ic_decrease_temperature_blocked)
                    binding.buttonIncreaseTemperature.setImageResource(R.drawable.ic_increase_temperature_blocked)
                    binding.labelTemperature.setTextColor(Color.parseColor("#bfe37f"))
                }
            }
            if (acStarted) {
                realtimeDatabase.reference.child("/users/").child(firebaseAuth.currentUser!!.uid).child("acRemote")
                        .setValue("$temperature#$mode#$fanSpeed#$turboMode#$swingV#$swingH#$sleepMode")
            }
            editPreferences.putInt("Mode", mode).apply()
        }

        binding.buttonFanSpeed.setOnClickListener {
            vibrate()
            fanSpeed += 1
            if (fanSpeed == 4) fanSpeed = 0
            when (fanSpeed) {
                0 -> {
                    binding.labelFanSpeed.text = "Слабый"
                }
                1 -> {
                    binding.labelFanSpeed.text = "Средний"
                }
                2 -> {
                    binding.labelFanSpeed.text = "Сильный"
                }
                3 -> {
                    binding.labelFanSpeed.text = "Авто"
                }
            }
            if (acStarted) {
                realtimeDatabase.reference.child("/users/").child(firebaseAuth.currentUser!!.uid).child("acRemote")
                        .setValue("$temperature#$mode#$fanSpeed#$turboMode#$swingV#$swingH#$sleepMode")
            }
            editPreferences.putInt("FanSpeed", fanSpeed).apply()
        }

        binding.buttonTurboMode.setOnClickListener {
            vibrate()
            turboMode = !turboMode
            if (turboMode) {
                binding.labelTurboMode.text = "Турбо: вкл"
            } else {
                binding.labelTurboMode.text = "Турбо: выкл"
            }
            if (acStarted) {
                realtimeDatabase.reference.child("/users/").child(firebaseAuth.currentUser!!.uid).child("acRemote")
                        .setValue("$temperature#$mode#$fanSpeed#$turboMode#$swingV#$swingH#$sleepMode")
            }
            editPreferences.putBoolean("TurboMode", turboMode).apply()
        }

        binding.buttonSwingV.setOnClickListener {
            vibrate()
            swingV = !swingV
            if (swingV) {
                binding.labelSwingV.text = "SwingV:\nвкл"
            } else {
                binding.labelSwingV.text = "SwingV:\nвыкл"
            }
            if (acStarted) {
                realtimeDatabase.reference.child("/users/").child(firebaseAuth.currentUser!!.uid).child("acRemote")
                        .setValue("$temperature#$mode#$fanSpeed#$turboMode#$swingV#$swingH#$sleepMode")
            }
            editPreferences.putBoolean("SwingV", swingV).apply()
        }

        binding.buttonSwingH.setOnClickListener {
            vibrate()
            swingH = !swingH
            if (swingH) {
                binding.labelSwingH.text = "SwingH:\nвкл"
            } else {
                binding.labelSwingH.text = "SwingH:\nвыкл"
            }
            if (acStarted) {
                realtimeDatabase.reference.child("/users/").child(firebaseAuth.currentUser!!.uid).child("acRemote")
                        .setValue("$temperature#$mode#$fanSpeed#$turboMode#$swingV#$swingH#$sleepMode")
            }
            editPreferences.putBoolean("SwingH", swingH).apply()
        }

        binding.buttonSleepMode.setOnClickListener {
            vibrate()
            sleepMode = !sleepMode
            if (sleepMode) {
                binding.labelSleepMode.text = "Режим сна:\nвкл"
            } else {
                binding.labelSleepMode.text = "Режим сна:\nвыкл"
            }
            if (acStarted) {
                realtimeDatabase.reference.child("/users/").child(firebaseAuth.currentUser!!.uid).child("acRemote")
                        .setValue("$temperature#$mode#$fanSpeed#$turboMode#$swingV#$swingH#$sleepMode")
            }
            editPreferences.putBoolean("SleepMode", sleepMode).apply()
        }

        binding.buttonStartAc.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("/users/").child(firebaseAuth.currentUser!!.uid).child("acRemote")
                        .setValue("$temperature#$mode#$fanSpeed#$turboMode#$swingV#$swingH#$sleepMode")
                binding.buttonStartAc.visibility = View.GONE
                binding.buttonStopAc.visibility = View.VISIBLE
                acStarted = true
                editPreferences.putBoolean("AcStarted", true).apply()
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonStopAc.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("/users/").child(firebaseAuth.currentUser!!.uid).child("acRemote").setValue("off")
                binding.buttonStartAc.visibility = View.VISIBLE
                binding.buttonStopAc.visibility = View.GONE
                acStarted = false
                editPreferences.putBoolean("AcStarted", false).apply()
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val actionButtonsInflater = menuInflater
        actionButtonsInflater.inflate(R.menu.secondary_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.buttonHomeScreen1 -> {
                vibrate()
                val activity = Intent(this, MainActivity::class.java)
                startActivity(activity)
                true
            }
            R.id.buttonUserProfile2 -> {
                vibrate()
                val activity = Intent(this, UserProfile::class.java)
                startActivity(activity)
                true
            }
            R.id.buttonSettings2 -> {
                vibrate()
                val activity = Intent(this, SettingsActivity::class.java)
                startActivity(activity)
                true
            }
            else->super.onOptionsItemSelected(item)
        }
    }

    private fun isNetworkConnected() : Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            return networkInfo.isConnected
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (isHapticFeedbackEnabled) {
                binding.buttonStartAc.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
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