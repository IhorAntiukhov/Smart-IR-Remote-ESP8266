package com.arduinoworld.smartremote

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = Intent(this, MainActivity::class.java)
        startActivity(activity)
        finish()
    }
}