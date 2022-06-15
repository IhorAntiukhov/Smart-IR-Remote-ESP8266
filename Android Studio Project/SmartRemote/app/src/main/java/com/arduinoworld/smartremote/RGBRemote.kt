package com.arduinoworld.smartremote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.MenuItem
import android.widget.Toast
import com.arduinoworld.smartremote.databinding.ActivityRgbRemoteBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Suppress("DEPRECATION")
class RGBRemote : AppCompatActivity() {
    lateinit var binding : ActivityRgbRemoteBinding
    private var isHapticFeedbackEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRgbRemoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarRGBRemote)
        supportActionBar!!.title = "RGB Контроллер"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val firebaseAuth = FirebaseAuth.getInstance()
        val realtimeDatabase = FirebaseDatabase.getInstance()

        val sharedPreferences = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        isHapticFeedbackEnabled = sharedPreferences.getBoolean("HapticFeedbackEnabled", true)

        binding.buttonRGBOnOff.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("OnOff" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonIncreaseRGBBrightness.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("IncreaseBrightness" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonDecreaseRGBBrightness.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("DecreaseBrightness" + (1000..9999).random())
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonRed.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Red")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
        binding.buttonRed2.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Red2")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
        binding.buttonRed3.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Red3")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
        binding.buttonRed4.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Red4")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
        binding.buttonRed5.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Red5")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonBlue.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Blue")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
        binding.buttonBlue2.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Blue2")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
        binding.buttonBlue3.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Blue3")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
        binding.buttonBlue4.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Blue4")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
        binding.buttonBlue5.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Blue5")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonGreen.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Green")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
        binding.buttonGreen2.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Green2")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
        binding.buttonGreen3.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Green3")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
        binding.buttonGreen4.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Green4")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
        binding.buttonGreen5.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Green5")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonWhite.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("White")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonFlash.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Flash")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonFade.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Fade")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonStrobe.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Strobe")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonSmooth.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton")
                    .setValue("Smooth")
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onOptionsItemSelected(item : MenuItem): Boolean {
        val id : Int = item.itemId
        if (id == android.R.id.home) {
            vibrate()
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
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
                binding.buttonRGBOnOff.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
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