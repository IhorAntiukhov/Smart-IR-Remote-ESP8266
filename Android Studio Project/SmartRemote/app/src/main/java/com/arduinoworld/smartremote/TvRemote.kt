package com.arduinoworld.smartremote

import android.annotation.SuppressLint
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
import com.arduinoworld.smartremote.databinding.ActivityTvRemoteBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Suppress("DEPRECATION")
@SuppressLint("CommitPrefEdits")
class TvRemote : AppCompatActivity() {
    private lateinit var binding : ActivityTvRemoteBinding

    private var isHapticFeedbackEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTvRemoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarTvRemote)
        supportActionBar!!.title = "Телевизор"

        val firebaseAuth = FirebaseAuth.getInstance()
        val realtimeDatabase = FirebaseDatabase.getInstance()

        val sharedPreferences = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        isHapticFeedbackEnabled = sharedPreferences.getBoolean("HapticFeedbackEnabled", true)

        binding.buttonTvOnOff.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton")
                        .setValue("OnOff" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonTvDtv.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton")
                        .setValue("TvDtv" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonIncreaseVolume.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton")
                        .setValue("IncreaseVolume" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonDecreaseVolume.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton")
                        .setValue("DecreaseVolume" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonNextChannel.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton")
                        .setValue("NextChannel" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonPreviousChannel.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton")
                        .setValue("PreviousChannel" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonMute.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton")
                        .setValue("Mute" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonSource.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton")
                        .setValue("Source" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonUp.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton")
                        .setValue("Up" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonLeft.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton")
                        .setValue("Left" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonRight.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton")
                        .setValue("Right" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonDown.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton")
                        .setValue("Down" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonScreen.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton")
                        .setValue("Screen" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonRGBRemote.setOnClickListener {
            vibrate()
            val activity = Intent(this, RGBRemote::class.java)
            startActivity(activity)
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
                binding.buttonTvOnOff.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
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