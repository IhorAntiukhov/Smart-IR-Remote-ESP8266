package com.arduinoworld.smartremote

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.arduinoworld.smartremote.databinding.ActivityMainBinding

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private var isHapticFeedbackEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMainActivity)
        supportActionBar!!.title = "Домашняя"

        val sharedPreferences = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        val isUserLogged = sharedPreferences.getBoolean("UserLogged", false)
        isHapticFeedbackEnabled = sharedPreferences.getBoolean("HapticFeedbackEnabled", true)

        binding.buttonLightRemote.setOnClickListener {
            vibrate()
            if (isUserLogged) {
                val activity = Intent(this, LightRemote::class.java)
                startActivity(activity)
            } else {
                Toast.makeText(this, "Вы не вошли\nв пользователя!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonTVRemote.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (isUserLogged) {
                    val activity = Intent(this, TvRemote::class.java)
                    startActivity(activity)
                } else {
                    Toast.makeText(this, "Вы не вошли\nв пользователя!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonConditionerRemote.setOnClickListener {
            vibrate()
            if (isUserLogged) {
                val activity = Intent(this, AcRemote::class.java)
                startActivity(activity)
            } else {
                Toast.makeText(this, "Вы не вошли\nв пользователя!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val actionButtonsInflater = menuInflater
        actionButtonsInflater.inflate(R.menu.activity_main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.buttonUserProfile -> {
                vibrate()
                val activity = Intent(this, UserProfile::class.java)
                startActivity(activity)
                true
            }
            R.id.buttonSettings1 -> {
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
                binding.buttonLightRemote.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
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