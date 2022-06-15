package com.arduinoworld.smartremote

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.arduinoworld.smartremote.databinding.ActivityLightRemoteBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil
import kotlin.math.floor

@Suppress("DEPRECATION")
@SuppressLint("CommitPrefEdits", "SetTextI18n")
class LightRemote : AppCompatActivity() {
    private lateinit var binding : ActivityLightRemoteBinding
    private lateinit var lightRecyclerAdapter : LightRecyclerAdapter
    private lateinit var mainMenu : Menu
    private lateinit var editPreferences : SharedPreferences.Editor
    private lateinit var countDownTimer : CountDownTimer
    private lateinit var gson : Gson

    private var isHapticFeedbackEnabled = false
    private var remotesArrayList = ArrayList<Light>()
    private var remoteIdsArrayList = ArrayList<Int>()
    private var remotesArrayListJson = ""
    private var remoteIdsArrayListJson = ""
    private var remotePosition = 0
    private var remoteId = 0
    private var remoteName = ""
    private var brightness = 0
    private var colorTemperature = 6500
    private var changeBrightnessStep = 7.5f
    private var changeColorTemperatureStep = 175
    private var minColorTemperature = 3000
    private var maxColorTemperature = 6500
    private var lightState = false
    private var isTimerStarted = false
    private var isTimerStarted2 = false
    private var timerScreenOpened = false
    private var timerTime = 1
    private var timeLeftInMillis : Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLightRemoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarLightRemote)
        supportActionBar!!.title = "Добавление Пульта"

        val firebaseAuth = FirebaseAuth.getInstance()
        val realtimeDatabase = FirebaseDatabase.getInstance()

        val sharedPreferences = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        editPreferences = sharedPreferences.edit()
        gson = Gson()

        isHapticFeedbackEnabled = sharedPreferences.getBoolean("HapticFeedbackEnabled", true)
        remotesArrayListJson = sharedPreferences.getString("RemotesArrayList", "").toString()
        remoteIdsArrayListJson = sharedPreferences.getString("RemoteIdsArrayList", "").toString()
        minColorTemperature = sharedPreferences.getInt("MinColorTemperature", 3000)
        maxColorTemperature = sharedPreferences.getInt("MaxColorTemperature", 6500)
        isTimerStarted = sharedPreferences.getBoolean("TimerStarted", false)

        changeBrightnessStep = (75 / (sharedPreferences.getInt("ChangeBrightnessSteps", 10))).toFloat()
        changeColorTemperatureStep = (maxColorTemperature - minColorTemperature) / (sharedPreferences.getInt("ChangeColorTemperatureSteps", 20))

        if (remotesArrayListJson != "") {
            supportActionBar!!.title = "Освещение"
            remotesArrayList = gson.fromJson(remotesArrayListJson, object : TypeToken<ArrayList<Light?>?>() {}.type)
            remoteIdsArrayList = gson.fromJson(remoteIdsArrayListJson, object : TypeToken<ArrayList<Int?>?>() {}.type)
            lightRecyclerAdapter = LightRecyclerAdapter(remotesArrayList)
            lightRecyclerAdapter.setOnItemClickListener(recyclerAdapterItemClickListener)
            binding.lightRecyclerView.apply {
                adapter = lightRecyclerAdapter
                layoutManager = GridLayoutManager(baseContext, 2)
            }
            binding.lightRecyclerView.visibility = View.VISIBLE
            binding.fabAddRemote.visibility = View.VISIBLE
        }

        binding.buttonAddRemote.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (binding.inputRemoteID.text!!.isNotEmpty() && binding.inputRemoteName.text!!.isNotEmpty()) {
                    hideKeyboard()
                    supportActionBar!!.title = "Освещение"
                    supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                    supportActionBar!!.setDisplayShowHomeEnabled(false)
                    mainMenu.findItem(R.id.buttonHomeScreen1).isVisible = true
                    mainMenu.findItem(R.id.buttonUserProfile2).isVisible = true
                    mainMenu.findItem(R.id.buttonSettings2).isVisible = true
                    remotesArrayList.add(Light(binding.inputRemoteName.text!!.toString(), 0, 6500))
                    remoteIdsArrayList.add(binding.inputRemoteID.text.toString().toInt())
                    lightRecyclerAdapter = LightRecyclerAdapter(remotesArrayList)
                    lightRecyclerAdapter.setOnItemClickListener(recyclerAdapterItemClickListener)
                    binding.lightRecyclerView.apply {
                        adapter = lightRecyclerAdapter
                        layoutManager = GridLayoutManager(baseContext, 2)
                    }
                    binding.lightRecyclerView.visibility = View.VISIBLE
                    binding.fabAddRemote.visibility = View.VISIBLE
                    binding.inputRemoteName.setText("")
                    binding.inputRemoteID.setText("")
                    remotesArrayListJson = gson.toJson(remotesArrayList)
                    remoteIdsArrayListJson = gson.toJson(remoteIdsArrayList)
                    editPreferences.putString("RemotesArrayList", remotesArrayListJson)
                    editPreferences.putString("RemoteIdsArrayList", remoteIdsArrayListJson).apply()
                } else {
                    if (binding.inputRemoteID.text!!.isEmpty()) {
                        binding.inputLayoutRemoteID.isErrorEnabled = true
                        binding.inputLayoutRemoteID.error = "Введите ID пульта"
                    }
                    if (binding.inputRemoteName.text!!.isEmpty()) {
                        binding.inputLayoutRemoteName.isErrorEnabled = true
                        binding.inputLayoutRemoteName.error = "Введите название пульта"
                    }
                }
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.fabAddRemote.setOnClickListener {
            vibrate()
            supportActionBar!!.title = "Добавление Пульта"
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            binding.toolbarLightRemote.setNavigationOnClickListener {
                vibrate()
                supportActionBar!!.title = "Освещение"
                binding.lightRecyclerView.visibility = View.VISIBLE
                binding.fabAddRemote.visibility = View.VISIBLE
                binding.layoutLightRemote.visibility = View.GONE
                mainMenu.findItem(R.id.buttonBackLightRemote).isVisible = false
                mainMenu.findItem(R.id.buttonDeleteLightRemote).isVisible = false
                mainMenu.findItem(R.id.buttonEditLightRemote).isVisible = false
                mainMenu.findItem(R.id.buttonHomeScreen1).isVisible = true
                mainMenu.findItem(R.id.buttonUserProfile2).isVisible = true
                mainMenu.findItem(R.id.buttonSettings2).isVisible = true
                supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                supportActionBar!!.setDisplayShowHomeEnabled(false)
            }
            mainMenu.findItem(R.id.buttonHomeScreen1).isVisible = false
            mainMenu.findItem(R.id.buttonUserProfile2).isVisible = false
            mainMenu.findItem(R.id.buttonSettings2).isVisible = false
            binding.lightRecyclerView.visibility = View.GONE
            binding.inputLayoutRemoteID.visibility = View.VISIBLE
            binding.inputLayoutRemoteName.visibility = View.VISIBLE
            binding.buttonAddRemote.visibility = View.VISIBLE
        }

        binding.buttonOnOff.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (!isTimerStarted) {
                    realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child(remoteId.toString()).child("remoteButton")
                            .setValue("OnOff" + (1000..9999).random())
                    lightState = !lightState
                    if (lightState) {
                        if (brightness == 0) brightness = 100
                        binding.labelCurrentBrightness.text = "${brightness}%"
                        remotesArrayList[remotePosition] = Light(remoteName, brightness, colorTemperature)
                    } else {
                        binding.labelCurrentBrightness.text = "Выкл"
                        remotesArrayList[remotePosition] = Light(remoteName, 0, colorTemperature)
                    }
                    lightRecyclerAdapter.notifyItemChanged(remotePosition)
                    remotesArrayListJson = gson.toJson(remotesArrayList)
                    editPreferences.putString("RemotesArrayList", remotesArrayListJson).apply()
                } else {
                    Toast.makeText(this, "Вы не можете\nвыключать свет,\nпока запущен таймер!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonNightMode.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (lightState) {
                    realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child(remoteId.toString()).child("remoteButton")
                            .setValue("NightMode" + (1000..9999).random())
                    brightness = 25
                    binding.labelCurrentBrightness.text = "$brightness%"
                    remotesArrayList[remotePosition] = Light(remoteName, brightness, colorTemperature)
                    lightRecyclerAdapter.notifyItemChanged(remotePosition)
                    remotesArrayListJson = gson.toJson(remotesArrayList)
                    editPreferences.putString("RemotesArrayList", remotesArrayListJson).apply()
                } else {
                    Toast.makeText(this, "Вы не можете включать\nночной режим, пока\nвыключен свет!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonIncreaseBrightness.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (lightState) {
                    realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child(remoteId.toString()).child("remoteButton")
                            .setValue("IncreaseBrightness" + (1000..9999).random())
                    brightness = if ((floor(brightness.toFloat() + changeBrightnessStep)).toInt()%2 == 0) {
                        (floor(brightness.toFloat() + changeBrightnessStep)).toInt()
                    } else {
                        (ceil(brightness.toFloat() + changeBrightnessStep)).toInt()
                    }
                    if (100 - brightness < changeBrightnessStep) {
                        brightness = 100
                    }
                    binding.labelCurrentBrightness.text = "$brightness%"
                    remotesArrayList[remotePosition] = Light(remoteName, brightness, colorTemperature)
                    lightRecyclerAdapter.notifyItemChanged(remotePosition)
                    remotesArrayListJson = gson.toJson(remotesArrayList)
                    editPreferences.putString("RemotesArrayList", remotesArrayListJson).apply()
                } else {
                    Toast.makeText(this, "Вы не можете изменять\n яркость, пока выключен свет!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonDecreaseBrightness.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (lightState) {
                    if (brightness > 25) {
                        brightness = if ((floor(brightness.toFloat() - changeBrightnessStep)).toInt()%2 == 0) {
                            (floor(brightness.toFloat() - changeBrightnessStep)).toInt()
                        } else {
                            (ceil(brightness.toFloat() - changeBrightnessStep)).toInt()
                        }
                        if (brightness - 25 < changeBrightnessStep) {
                            brightness = 25
                        }
                        binding.labelCurrentBrightness.text = "$brightness%"
                        remotesArrayList[remotePosition] = Light(remoteName, brightness, colorTemperature)
                        lightRecyclerAdapter.notifyItemChanged(remotePosition)
                        remotesArrayListJson = gson.toJson(remotesArrayList)
                        editPreferences.putString("RemotesArrayList", remotesArrayListJson).apply()
                    }
                    realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child(remoteId.toString()).child("remoteButton").setValue("DecreaseBrightness" + (1000..9999).random())
                } else {
                    Toast.makeText(this, "Вы не можете изменять\nяркость, пока выключен свет!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonIncreaseTemperature.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (lightState) {
                    if (colorTemperature < maxColorTemperature) {
                        colorTemperature += changeColorTemperatureStep
                        binding.labelCurrentColorTemperature.text = "${colorTemperature}K"
                        remotesArrayList[remotePosition] = Light(remoteName, brightness, colorTemperature)
                        lightRecyclerAdapter.notifyItemChanged(remotePosition)
                        remotesArrayListJson = gson.toJson(remotesArrayList)
                        editPreferences.putString("RemotesArrayList", remotesArrayListJson).apply()
                    }
                    realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child(remoteId.toString()).child("remoteButton").setValue("IncreaseTemperature" + (1000..9999).random())
                } else {
                    Toast.makeText(this, "Вы не можете изменять\nтемпературу света, пока\n он выключен!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonDecreaseTemperature.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (lightState) {
                    if (colorTemperature > minColorTemperature) {
                        colorTemperature -= changeColorTemperatureStep
                        binding.labelCurrentColorTemperature.text = "${colorTemperature}K"
                        remotesArrayList[remotePosition] = Light(remoteName, brightness, colorTemperature)
                        lightRecyclerAdapter.notifyItemChanged(remotePosition)
                        remotesArrayListJson = gson.toJson(remotesArrayList)
                        editPreferences.putString("RemotesArrayList", remotesArrayListJson).apply()
                    }
                    realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child(remoteId.toString()).child("remoteButton").setValue("DecreaseTemperature" + (1000..9999).random())
                } else {
                    Toast.makeText(this, "Вы не можете изменять\nтемпературу света, пока\n он выключен!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonMemory.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (lightState) {
                    realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child(remoteId.toString()).child("remoteButton")
                            .setValue("Memory" + (1000..9999).random())
                    Toast.makeText(this, "Параметры освещения сохранены!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Вы не можете сохранять\nпараметры освещения, пока\n оно выключено!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonColdTemperature.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (lightState) {
                    realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child(remoteId.toString()).child("remoteButton")
                            .setValue("ColdColor" + (1000..9999).random())
                } else {
                    Toast.makeText(this, "Вы не можете переключать\nрежим температуры света,\nпока он выключен!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonChangeRemote.setOnClickListener {
            vibrate()
            if (binding.inputRemoteID.text!!.isNotEmpty() && binding.inputRemoteName.text!!.isNotEmpty()) {
                hideKeyboard()
                remoteId = binding.inputRemoteID.text!!.toString().toInt()
                remoteName = binding.inputRemoteName.text!!.toString()
                binding.inputRemoteName.setText("")
                binding.inputRemoteID.setText("")
                binding.layoutLightRemote.visibility = View.VISIBLE
                binding.buttonChangeRemote.visibility = View.GONE
                remoteIdsArrayList[remotePosition] = remoteId
                remotesArrayList[remotePosition] = Light(remoteName, brightness, colorTemperature)
                lightRecyclerAdapter.notifyItemChanged(remotePosition)
                remotesArrayListJson = gson.toJson(remotesArrayList)
                remoteIdsArrayListJson = gson.toJson(remoteIdsArrayList)
                editPreferences.putString("RemotesArrayList", remotesArrayListJson)
                editPreferences.putString("RemoteIdsArrayList", remoteIdsArrayListJson).apply()
                supportActionBar!!.title = remoteName
                supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                supportActionBar!!.setDisplayShowHomeEnabled(false)
                mainMenu.findItem(R.id.buttonBackLightRemote).isVisible = true
                mainMenu.findItem(R.id.buttonDeleteLightRemote).isVisible = true
                mainMenu.findItem(R.id.buttonEditLightRemote).isVisible = true
                Toast.makeText(this, "Пульт сохранён!", Toast.LENGTH_SHORT).show()
            } else {
                if (binding.inputRemoteID.text!!.isEmpty()) {
                    binding.inputLayoutRemoteID.isErrorEnabled = true
                    binding.inputLayoutRemoteID.error = "Введите ID пульта"
                }
                if (binding.inputRemoteName.text!!.isEmpty()) {
                    binding.inputLayoutRemoteName.isErrorEnabled = true
                    binding.inputLayoutRemoteName.error = "Введите название пульта"
                }
            }
        }

        binding.buttonTimer.setOnClickListener {
            vibrate()
            supportActionBar!!.title = "Таймер Выключения"
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            binding.toolbarLightRemote.setNavigationOnClickListener {
                vibrate()
                hideKeyboard()
                supportActionBar!!.title = remoteName
                supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                supportActionBar!!.setDisplayShowHomeEnabled(false)
                binding.layoutLightRemote.visibility = View.VISIBLE
                binding.inputLayoutRemoteID.visibility = View.VISIBLE
                binding.inputLayoutRemoteName.visibility = View.VISIBLE
                binding.buttonAddRemote.visibility = View.VISIBLE
                binding.layoutTimerTime.visibility = View.GONE
                binding.sliderTimerTime.visibility = View.GONE
                binding.buttonStartTimer.visibility = View.GONE
                binding.layoutTimeLeft.visibility = View.GONE
                binding.buttonStopTimer.visibility = View.GONE
                mainMenu.findItem(R.id.buttonBackLightRemote).isVisible = true
                mainMenu.findItem(R.id.buttonDeleteLightRemote).isVisible = true
                mainMenu.findItem(R.id.buttonEditLightRemote).isVisible = true
                timerScreenOpened = false
            }
            mainMenu.findItem(R.id.buttonBackLightRemote).isVisible = false
            mainMenu.findItem(R.id.buttonDeleteLightRemote).isVisible = false
            mainMenu.findItem(R.id.buttonEditLightRemote).isVisible = false
            binding.layoutLightRemote.visibility = View.GONE
            binding.inputLayoutRemoteID.visibility = View.GONE
            binding.inputLayoutRemoteName.visibility = View.GONE
            binding.buttonAddRemote.visibility = View.GONE
            if (!isTimerStarted) {
                binding.layoutTimerTime.visibility = View.VISIBLE
                binding.sliderTimerTime.visibility = View.VISIBLE
                binding.buttonStartTimer.visibility = View.VISIBLE
            } else {
                binding.layoutTimeLeft.visibility = View.VISIBLE
                binding.buttonStopTimer.visibility = View.VISIBLE
                timerTime = sharedPreferences.getInt("TimerTime", 1)
                timeLeftInMillis = sharedPreferences.getLong("TimeLeftInMillis", 0)
                binding.sliderTimerTime.max = 179
                binding.sliderTimerTime.progress = timerTime - 1
                binding.labelTimerTime.text = "$timerTime мин"
                if (isTimerStarted && !isTimerStarted2) {
                    isTimerStarted2 = true
                    startTimer()
                }
            }
            timerScreenOpened = true
        }

        binding.sliderTimerTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, position: Int, p2: Boolean) {
                timerTime = position + 1
                binding.labelTimerTime.text = "$timerTime мин"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                vibrate()
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        binding.buttonStartTimer.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                isTimerStarted = true
                isTimerStarted2 = true
                timeLeftInMillis = (timerTime * 60000).toLong()
                binding.layoutTimerTime.visibility = View.GONE
                binding.sliderTimerTime.visibility = View.GONE
                binding.buttonStartTimer.visibility = View.GONE
                binding.layoutTimeLeft.visibility = View.VISIBLE
                binding.buttonStopTimer.visibility = View.VISIBLE
                if (!lightState) {
                    lightState = true
                    if (brightness == 0) brightness = 100
                    binding.labelCurrentBrightness.text = "${brightness}%"
                    remotesArrayList[remotePosition] = Light(remoteName, brightness, colorTemperature)
                    lightRecyclerAdapter.notifyItemChanged(remotePosition)
                    remotesArrayListJson = gson.toJson(remotesArrayList)
                    editPreferences.putString("RemotesArrayList", remotesArrayListJson).apply()
                }
                editPreferences.putBoolean("TimerStarted", true)
                editPreferences.putInt("TimerTime", timerTime).apply()
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child(remoteId.toString()).child("timerTime").setValue(timerTime)
                startTimer()
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonStopTimer.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                isTimerStarted = false
                isTimerStarted2 = false
                timeLeftInMillis = (timerTime * 60000).toLong()
                binding.layoutTimerTime.visibility = View.VISIBLE
                binding.sliderTimerTime.visibility = View.VISIBLE
                binding.buttonStartTimer.visibility = View.VISIBLE
                binding.layoutTimeLeft.visibility = View.GONE
                binding.buttonStopTimer.visibility = View.GONE
                editPreferences.putBoolean("TimerStarted", false)
                editPreferences.putInt("TimerTime", timerTime).apply()
                realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child(remoteId.toString()).child("timerTime").setValue(0)
                countDownTimer.cancel()
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                if (timerTime <= 60) {
                    val minutes = (timeLeftInMillis / 1000).toInt() / 60
                    val seconds = (timeLeftInMillis / 1000).toInt() % 60
                    binding.labelTimeLeft.text = "00:${java.lang.String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)}"
                } else {
                    val hours = (timeLeftInMillis / 1000).toInt() / 3600
                    val minutes = (timeLeftInMillis / 1000 % 3600).toInt() / 60
                    val seconds = (timeLeftInMillis / 1000).toInt() % 60
                    binding.labelTimeLeft.text = "0${java.lang.String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)}"
                }
            }

            override fun onFinish() {
                Toast.makeText(baseContext, "Время таймера истекло!", Toast.LENGTH_SHORT).show()
                isTimerStarted = false
                isTimerStarted2 = false
                if (timerScreenOpened) {
                    binding.layoutTimerTime.visibility = View.VISIBLE
                    binding.sliderTimerTime.visibility = View.VISIBLE
                    binding.buttonStartTimer.visibility = View.VISIBLE
                    binding.layoutTimeLeft.visibility = View.GONE
                    binding.buttonStopTimer.visibility = View.GONE
                }
                lightState = false
                binding.labelCurrentBrightness.text = "Выкл"
                remotesArrayList[remotePosition] = Light(remoteName, 0, colorTemperature)
                lightRecyclerAdapter.notifyItemChanged(remotePosition)
                remotesArrayListJson = gson.toJson(remotesArrayList)
                editPreferences.putString("RemotesArrayList", remotesArrayListJson)
                editPreferences.putBoolean("TimerStarted", false).apply()
            }
        }.start()
    }

    private val recyclerAdapterItemClickListener = (object : LightRecyclerAdapter.OnItemClickListener {
        override fun onItemClick(position: Int) {
            vibrate()
            remotePosition = position
            remoteId = remoteIdsArrayList[remotePosition]
            remoteName = remotesArrayList[remotePosition].remoteName
            brightness = remotesArrayList[remotePosition].brightness
            colorTemperature = remotesArrayList[remotePosition].colorTemperature
            supportActionBar!!.title = remoteName
            if (brightness == 0) {
                lightState = false
                binding.labelCurrentBrightness.text = "Выкл"
            } else {
                lightState = true
                binding.labelCurrentBrightness.text = "$brightness%"
            }
            binding.labelCurrentColorTemperature.text = "${colorTemperature}K"
            binding.lightRecyclerView.visibility = View.GONE
            binding.fabAddRemote.visibility = View.GONE
            binding.layoutLightRemote.visibility = View.VISIBLE
            mainMenu.findItem(R.id.buttonBackLightRemote).isVisible = true
            mainMenu.findItem(R.id.buttonDeleteLightRemote).isVisible = true
            mainMenu.findItem(R.id.buttonEditLightRemote).isVisible = true
            mainMenu.findItem(R.id.buttonHomeScreen1).isVisible = false
            mainMenu.findItem(R.id.buttonUserProfile2).isVisible = false
            mainMenu.findItem(R.id.buttonSettings2).isVisible = false
        }
    })

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val actionButtonsInflater = menuInflater
        actionButtonsInflater.inflate(R.menu.secondary_activity_menu, menu)
        mainMenu = menu!!
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

            R.id.buttonBackLightRemote -> {
                vibrate()
                supportActionBar!!.title = "Освещение"
                binding.lightRecyclerView.visibility = View.VISIBLE
                binding.fabAddRemote.visibility = View.VISIBLE
                binding.layoutLightRemote.visibility = View.GONE
                mainMenu.findItem(R.id.buttonBackLightRemote).isVisible = false
                mainMenu.findItem(R.id.buttonDeleteLightRemote).isVisible = false
                mainMenu.findItem(R.id.buttonEditLightRemote).isVisible = false
                mainMenu.findItem(R.id.buttonHomeScreen1).isVisible = true
                mainMenu.findItem(R.id.buttonUserProfile2).isVisible = true
                mainMenu.findItem(R.id.buttonSettings2).isVisible = true
                true
            }
            R.id.buttonDeleteLightRemote -> {
                vibrate()
                val alertDialogDeleteRemoteBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
                alertDialogDeleteRemoteBuilder.setTitle("Удаление Пульта")
                alertDialogDeleteRemoteBuilder.setMessage("Вы точно хотите удалить пульт ${remotesArrayList[remotePosition].remoteName}?")
                alertDialogDeleteRemoteBuilder.setPositiveButton("Подтвердить") { _, _ ->
                    vibrate()
                    supportActionBar!!.title = "Освещение"
                    remotesArrayList.removeAt(remotePosition)
                    remoteIdsArrayList.removeAt(remotePosition)
                    lightRecyclerAdapter.notifyItemRemoved(remotePosition)
                    if (remotesArrayList.size > 0) {
                        binding.lightRecyclerView.visibility = View.VISIBLE
                        binding.fabAddRemote.visibility = View.VISIBLE
                        remotesArrayListJson = gson.toJson(remotesArrayList)
                        remoteIdsArrayListJson = gson.toJson(remoteIdsArrayList)
                        editPreferences.putString("RemotesArrayList", remotesArrayListJson)
                        editPreferences.putString("RemoteIdsArrayList", remoteIdsArrayListJson).apply()
                    } else {
                        supportActionBar!!.title = "Освещение"
                        binding.inputLayoutRemoteName.visibility = View.VISIBLE
                        binding.inputLayoutRemoteID.visibility = View.VISIBLE
                        binding.buttonAddRemote.visibility = View.VISIBLE
                        editPreferences.putString("RemotesArrayList", "")
                        editPreferences.putString("RemoteIdsArrayList", "").apply()
                    }
                    binding.layoutLightRemote.visibility = View.GONE
                    mainMenu.findItem(R.id.buttonBackLightRemote).isVisible = false
                    mainMenu.findItem(R.id.buttonDeleteLightRemote).isVisible = false
                    mainMenu.findItem(R.id.buttonEditLightRemote).isVisible = false
                    mainMenu.findItem(R.id.buttonHomeScreen1).isVisible = true
                    mainMenu.findItem(R.id.buttonUserProfile2).isVisible = true
                    mainMenu.findItem(R.id.buttonSettings2).isVisible = true
                    Toast.makeText(this, "Пульт удалён!", Toast.LENGTH_LONG).show()
                }
                alertDialogDeleteRemoteBuilder.setNegativeButton("Отмена") { _, _ ->
                    vibrate()
                }
                val alertDialogDeleteRemote = alertDialogDeleteRemoteBuilder.create()
                alertDialogDeleteRemote.show()
                true
            }
            R.id.buttonEditLightRemote -> {
                vibrate()
                supportActionBar!!.title = "Изменение Пульта"
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                supportActionBar!!.setDisplayShowHomeEnabled(true)
                binding.toolbarLightRemote.setNavigationOnClickListener {
                    vibrate()
                    hideKeyboard()
                    supportActionBar!!.title = remoteName
                    supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                    supportActionBar!!.setDisplayShowHomeEnabled(false)
                    binding.layoutLightRemote.visibility = View.VISIBLE
                    binding.buttonChangeRemote.visibility = View.GONE
                    mainMenu.findItem(R.id.buttonBackLightRemote).isVisible = true
                    mainMenu.findItem(R.id.buttonDeleteLightRemote).isVisible = true
                    mainMenu.findItem(R.id.buttonEditLightRemote).isVisible = true
                }
                binding.inputRemoteName.setText(remoteName)
                binding.inputRemoteID.setText(remoteId.toString())
                binding.layoutLightRemote.visibility = View.GONE
                binding.buttonAddRemote.visibility = View.GONE
                binding.buttonChangeRemote.visibility = View.VISIBLE
                mainMenu.findItem(R.id.buttonBackLightRemote).isVisible = false
                mainMenu.findItem(R.id.buttonDeleteLightRemote).isVisible = false
                mainMenu.findItem(R.id.buttonEditLightRemote).isVisible = false
                true
            }
            else->super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isTimerStarted) {
            editPreferences.putLong("TimeLeftInMillis", timeLeftInMillis).apply()
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
                binding.lightRecyclerView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
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