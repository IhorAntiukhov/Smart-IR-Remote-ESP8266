package com.arduinoworld.smartremote

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Patterns
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.arduinoworld.smartremote.databinding.ActivityUserProfileBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Suppress("DEPRECATION")
@SuppressLint("CommitPrefEdits", "SetTextI18n")
class UserProfile : AppCompatActivity() {
    private lateinit var binding : ActivityUserProfileBinding
    private lateinit var mainMenu : Menu
    private var isHapticFeedbackEnabled = false
    private var passwordToggle = false
    private var changeCredential = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val firebaseAuth = FirebaseAuth.getInstance()
        val realtimeDatabase = FirebaseDatabase.getInstance()

        val sharedPreferences = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        val editPreferences = sharedPreferences.edit()

        val isUserLogged = sharedPreferences.getBoolean("UserLogged", false)
        isHapticFeedbackEnabled = sharedPreferences.getBoolean("HapticFeedbackEnabled", true)

        binding.inputUserEmail.setText(sharedPreferences.getString("UserEmail", "").toString())
        binding.inputUserPassword.setText(sharedPreferences.getString("UserPassword", "").toString())

        setSupportActionBar(binding.toolbarUserProfile)
        if (isUserLogged) {
            supportActionBar!!.title = "Профиль"
            binding.inputLayoutUserEmail.visibility = View.GONE
            binding.inputLayoutUserPassword.visibility = View.GONE
            binding.buttonSignIn.visibility = View.GONE
            binding.buttonResetPassword.visibility = View.GONE
            binding.imageViewUserProfile.visibility = View.VISIBLE
            binding.layoutEmailPassword.visibility = View.VISIBLE
            binding.layoutUserSettings.visibility = View.VISIBLE
            binding.labelEmailPassword.text = "${sharedPreferences.getString("UserEmail", "")}\n${hidePassword(sharedPreferences.getString("UserPassword", "").toString())}"
        } else {
            supportActionBar!!.title = "Вход"
        }

        binding.buttonSignIn.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (binding.inputUserEmail.text!!.isNotEmpty() && binding.inputUserPassword.text!!.isNotEmpty()) {
                    hideKeyboard()
                    val alertDialogBuilder : AlertDialog.Builder = AlertDialog.Builder(this)
                    alertDialogBuilder.setView(layoutInflater.inflate(R.layout.sign_in_progress_bar, null))
                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.setCanceledOnTouchOutside(false)
                    alertDialog.show()
                    firebaseAuth.signInWithEmailAndPassword(binding.inputUserEmail.text.toString(), binding.inputUserPassword.text.toString())
                            .addOnCompleteListener(this) { signInTask ->
                                if (signInTask.isSuccessful) {
                                    supportActionBar?.title = "Профиль"
                                    binding.inputLayoutUserEmail.visibility = View.GONE
                                    binding.inputLayoutUserPassword.visibility = View.GONE
                                    binding.buttonSignIn.visibility = View.GONE
                                    binding.buttonResetPassword.visibility = View.GONE
                                    binding.imageViewUserProfile.visibility = View.VISIBLE
                                    binding.layoutEmailPassword.visibility = View.VISIBLE
                                    binding.layoutUserSettings.visibility = View.VISIBLE
                                    binding.inputLayoutUserEmail.isErrorEnabled = false
                                    binding.inputLayoutUserPassword.isErrorEnabled = false
                                    binding.labelEmailPassword.text = "${binding.inputUserEmail.text!!}\n${hidePassword(binding.inputUserPassword.text!!.toString())}"
                                    editPreferences.putBoolean("UserLogged", true)
                                    editPreferences.putString("UserEmail", binding.inputUserEmail.text!!.toString())
                                    editPreferences.putString("UserPassword", binding.inputUserPassword.text!!.toString()).apply()
                                    if (!sharedPreferences.getBoolean(binding.inputUserEmail.text!!.toString(), false)) {
                                        realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton").setValue("")
                                        realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton").setValue("")
                                        realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("acRemote").setValue("")
                                        editPreferences.putBoolean(binding.inputUserEmail.text!!.toString(), true).apply()
                                    }
                                } else {
                                    try {
                                        throw signInTask.exception!!
                                    } catch (e: FirebaseAuthInvalidCredentialsException) {
                                        binding.inputLayoutUserEmail.isErrorEnabled = false
                                        binding.inputLayoutUserPassword.isErrorEnabled = true
                                        binding.inputLayoutUserPassword.error = "Введён неверный пароль"
                                    } catch (e: FirebaseAuthInvalidUserException) {
                                        when (e.errorCode) {
                                            "ERROR_USER_NOT_FOUND" -> {
                                                binding.inputLayoutUserPassword.isErrorEnabled = false
                                                binding.inputLayoutUserEmail.isErrorEnabled = true
                                                binding.inputLayoutUserEmail.error = "Введённая почта не обнаружена"
                                            }
                                        }
                                    }
                                }
                                alertDialog.cancel()
                            }
                } else {
                    if (binding.inputUserEmail.text!!.isEmpty() && binding.inputUserPassword.text!!.isEmpty()) {
                        binding.inputLayoutUserEmail.isErrorEnabled = true
                        binding.inputLayoutUserPassword.isErrorEnabled = true
                        binding.inputLayoutUserEmail.error = "Введите почту пользователя"
                        binding.inputLayoutUserPassword.error = "Введите пароль пользователя"
                    } else if (binding.inputUserEmail.text!!.isEmpty() && binding.inputUserPassword.text!!.isNotEmpty()) {
                        binding.inputLayoutUserEmail.isErrorEnabled = true
                        binding.inputLayoutUserPassword.isErrorEnabled = false
                        binding.inputLayoutUserEmail.error = "Введите почту пользователя"
                    }  else if (binding.inputUserEmail.text!!.isNotEmpty() && binding.inputUserPassword.text!!.isEmpty()) {
                        binding.inputLayoutUserEmail.isErrorEnabled = false
                        binding.inputLayoutUserPassword.isErrorEnabled = true
                        binding.inputLayoutUserPassword.error = "Введите пароль пользователя"
                    }
                }
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonResetPassword.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (binding.inputUserEmail.text!!.isNotEmpty()) {
                    hideKeyboard()
                    val alertDialogResetPasswordBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
                    alertDialogResetPasswordBuilder.setTitle("Сброс Пароля")
                    alertDialogResetPasswordBuilder.setMessage("Отправка письма для сброса пароля на почту ${binding.inputUserEmail.text}")
                    alertDialogResetPasswordBuilder.setPositiveButton("Продолжить") { _, _ ->
                        vibrate()
                        firebaseAuth.sendPasswordResetEmail(binding.inputUserEmail.text.toString()).addOnCompleteListener(this) { resetPasswordTask ->
                            if (resetPasswordTask.isSuccessful) {
                                Toast.makeText(this, "Письмо для сброса \n пароля отправлено!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "Не удалось отправить \n письмо для сброса пароля!", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    val alertDialogResetPassword = alertDialogResetPasswordBuilder.create()
                    alertDialogResetPassword.show()
                }
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.fabAddUser.setOnClickListener {
            vibrate()
            val activity = Intent(this, SignUpActivity::class.java)
            startActivity(activity)
        }

        binding.buttonPasswordToggle.setOnClickListener {
            vibrate()
            passwordToggle = !passwordToggle
            if (passwordToggle) {
                binding.labelEmailPassword.text = "${binding.inputUserEmail.text}\n${binding.inputUserPassword.text}"
                binding.buttonPasswordToggle.setImageResource(R.drawable.ic_hide_password)
            } else {
                binding.labelEmailPassword.text = "${binding.inputUserEmail.text}\n${hidePassword(binding.inputUserPassword.text.toString())}"
                binding.buttonPasswordToggle.setImageResource(R.drawable.ic_show_password)
            }
        }

        binding.buttonLogout.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                firebaseAuth.signOut()
                supportActionBar!!.title = "Вход"
                binding.inputLayoutUserEmail.visibility = View.VISIBLE
                binding.inputLayoutUserPassword.visibility = View.VISIBLE
                binding.buttonSignIn.visibility = View.VISIBLE
                binding.buttonResetPassword.visibility = View.VISIBLE
                binding.imageViewUserProfile.visibility = View.GONE
                binding.layoutEmailPassword.visibility = View.GONE
                binding.layoutUserSettings.visibility = View.GONE
                editPreferences.putBoolean("UserLogged", false).apply()
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonChangeEmail.setOnClickListener {
            vibrate()
            changeCredential = false
            binding.layoutEmailPassword.visibility = View.GONE
            binding.layoutUserSettings.visibility = View.GONE
            binding.layoutChangePassword.visibility = View.GONE
            binding.imageViewUserProfile.visibility = View.GONE
            binding.fabAddUser.visibility = View.GONE
            binding.layoutChangeEmail.visibility = View.VISIBLE
            binding.buttonUpdateUser.visibility = View.VISIBLE
            supportActionBar!!.title = "Обновить Пользователя"
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            binding.toolbarUserProfile.setNavigationOnClickListener(navigationClickListener)
            mainMenu.findItem(R.id.buttonHomeScreen2).isVisible = false
            mainMenu.findItem(R.id.buttonSettings3).isVisible = false
        }

        binding.buttonChangePassword.setOnClickListener {
            vibrate()
            changeCredential = true
            binding.layoutEmailPassword.visibility = View.GONE
            binding.layoutUserSettings.visibility = View.GONE
            binding.layoutChangeEmail.visibility = View.GONE
            binding.imageViewUserProfile.visibility = View.GONE
            binding.fabAddUser.visibility = View.GONE
            binding.layoutChangePassword.visibility = View.VISIBLE
            binding.buttonUpdateUser.visibility = View.VISIBLE
            supportActionBar!!.title = "Обновить Пользователя"
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            binding.toolbarUserProfile.setNavigationOnClickListener(navigationClickListener)
            mainMenu.findItem(R.id.buttonHomeScreen2).isVisible = false
            mainMenu.findItem(R.id.buttonSettings3).isVisible = false
        }

        binding.buttonUpdateUser.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (!changeCredential) {
                    if (binding.inputChangeEmail.text!!.isNotEmpty()) {
                        if (isValidEmail(binding.inputChangeEmail.text!!)) {
                            if (binding.inputChangeEmail.text!! != binding.inputUserEmail.text!!) {
                                firebaseAuth.currentUser!!.reauthenticate(EmailAuthProvider.getCredential(
                                        sharedPreferences.getString("UserEmail", "").toString(), sharedPreferences.getString("UserPassword", "").toString())).addOnCompleteListener { reauthTask ->
                                    if (reauthTask.isSuccessful) {
                                        supportActionBar!!.title = "Профиль"
                                        firebaseAuth.currentUser!!.updateEmail(binding.inputChangeEmail.text.toString())
                                        binding.inputLayoutChangeEmail.isErrorEnabled = false
                                        binding.layoutEmailPassword.visibility = View.VISIBLE
                                        binding.layoutUserSettings.visibility = View.VISIBLE
                                        binding.imageViewUserProfile.visibility = View.VISIBLE
                                        binding.fabAddUser.visibility = View.VISIBLE
                                        binding.layoutChangeEmail.visibility = View.GONE
                                        binding.buttonUpdateUser.visibility = View.GONE
                                        binding.inputUserEmail.text = binding.inputChangeEmail.text
                                        binding.labelEmailPassword.text = "${binding.inputUserEmail.text!!}\n${hidePassword(binding.inputUserPassword.text!!.toString())}"
                                        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                                        supportActionBar!!.setDisplayShowHomeEnabled(false)
                                        mainMenu.findItem(R.id.buttonHomeScreen2).isVisible = true
                                        mainMenu.findItem(R.id.buttonSettings3).isVisible = true
                                        editPreferences.putString("UserEmail", binding.inputChangeEmail.text!!.toString()).apply()
                                        Toast.makeText(this, "Почта пользователя обновлена!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this, "Не удалось переавторизироваться!", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                binding.inputLayoutChangeEmail.isErrorEnabled = true
                                binding.inputLayoutChangeEmail.error = "Введите новую почту"
                            }
                        } else {
                            binding.inputLayoutChangeEmail.isErrorEnabled = true
                            binding.inputLayoutChangeEmail.error = "Вы ввели неправильную почту"
                        }
                    } else {
                        binding.inputLayoutChangeEmail.isErrorEnabled = true
                        binding.inputLayoutChangeEmail.error = "Введите почту пользователя"
                    }
                } else {
                    if (binding.inputChangePassword.text!!.isNotEmpty()) {
                        if (binding.inputChangePassword.text!!.length >= 6) {
                            if (containsNumber(binding.inputChangePassword.text!!.toString())) {
                                if (binding.inputChangePassword.text!! != binding.inputUserPassword.text!!) {
                                    firebaseAuth.currentUser!!.reauthenticate(EmailAuthProvider.getCredential(
                                            binding.inputUserEmail.text!!.toString(), binding.inputUserPassword.text!!.toString())).addOnCompleteListener { reauthTask ->
                                        if (reauthTask.isSuccessful) {
                                            supportActionBar!!.title = "Профиль"
                                            firebaseAuth.currentUser!!.updatePassword(binding.inputChangePassword.text.toString())
                                            binding.inputLayoutChangePassword.isErrorEnabled = false
                                            binding.layoutEmailPassword.visibility = View.VISIBLE
                                            binding.layoutUserSettings.visibility = View.VISIBLE
                                            binding.imageViewUserProfile.visibility = View.VISIBLE
                                            binding.fabAddUser.visibility = View.VISIBLE
                                            binding.layoutChangePassword.visibility = View.GONE
                                            binding.buttonUpdateUser.visibility = View.GONE
                                            binding.inputUserPassword.text = binding.inputChangePassword.text
                                            binding.labelEmailPassword.text = "${binding.inputUserEmail.text!!}\n${hidePassword(binding.inputUserPassword.text!!.toString())}"
                                            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                                            supportActionBar!!.setDisplayShowHomeEnabled(false)
                                            mainMenu.findItem(R.id.buttonHomeScreen2).isVisible = true
                                            mainMenu.findItem(R.id.buttonSettings3).isVisible = true
                                            editPreferences.putString("UserPassword", binding.inputChangePassword.text!!.toString()).apply()
                                            Toast.makeText(this, "Пароль пользователя обновлён!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(this, "Не удалось переавторизироваться!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    binding.inputLayoutChangePassword.isErrorEnabled = true
                                    binding.inputLayoutChangePassword.error = "Введите новый пароль"
                                }
                            } else {
                                binding.inputLayoutChangePassword.isErrorEnabled = true
                                binding.inputLayoutChangePassword.error = "Пароль должен иметь хотя бы 1 цифру"
                            }
                        } else {
                            binding.inputLayoutChangePassword.isErrorEnabled = true
                            binding.inputLayoutChangePassword.error = "Пароль должен быть не меньше 6 символов"
                        }
                    } else {
                        binding.inputLayoutChangePassword.isErrorEnabled = true
                        binding.inputLayoutChangePassword.error = "Введите пароль пользователя"
                    }
                }
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonDeleteUser.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                val alertDialogDeleteUserBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
                alertDialogDeleteUserBuilder.setTitle("Удаление Пользователя")
                alertDialogDeleteUserBuilder.setMessage("Вы точно хотите удалить пользователя ${sharedPreferences.getString("UserEmail", "").toString()}?")
                alertDialogDeleteUserBuilder.setPositiveButton("Подтвердить") { _, _ ->
                    vibrate()
                    val alertDialogBuilder : AlertDialog.Builder = AlertDialog.Builder(this)
                    alertDialogBuilder.setView(layoutInflater.inflate(R.layout.delete_user_progress_bar, null))
                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.setCanceledOnTouchOutside(false)
                    alertDialog.show()
                    val gson = Gson()
                    val remoteIdsArrayListJson = sharedPreferences.getString("RemoteIdsArrayList", "").toString()
                    if (remoteIdsArrayListJson != "") {
                        val remoteIdsArrayList : ArrayList<Int> = gson.fromJson(remoteIdsArrayListJson, object : TypeToken<ArrayList<Int?>?>() {}.type)
                        remoteIdsArrayList.forEach {
                            realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child(it.toString()).child("remoteButton").removeValue()
                            realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child(it.toString()).child("timerTime").removeValue()
                        }
                        realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("acRemote").removeValue()
                        realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("tvRemoteButton").removeValue()
                        realtimeDatabase.reference.child("users").child(firebaseAuth.currentUser!!.uid).child("rgbRemoteButton").removeValue()
                    }
                    firebaseAuth.currentUser!!.reauthenticate(
                            EmailAuthProvider.getCredential(binding.inputUserEmail.text.toString(),
                                    binding.inputUserPassword.text.toString())).addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            firebaseAuth.currentUser!!.delete().addOnCompleteListener { deleteUserTask ->
                                if (deleteUserTask.isSuccessful) {
                                    editPreferences.putString("UserEmail", "")
                                    editPreferences.putString("UserPassword", "")
                                    binding.inputUserEmail.setText("")
                                    binding.inputUserPassword.setText("")
                                    Toast.makeText(this, "Пользователь удалён!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this, "Не удалось удалить пользователя!", Toast.LENGTH_LONG).show()
                                }
                                supportActionBar!!.title = "Вход"
                                binding.inputLayoutUserEmail.visibility = View.VISIBLE
                                binding.inputLayoutUserPassword.visibility = View.VISIBLE
                                binding.buttonSignIn.visibility = View.VISIBLE
                                binding.buttonResetPassword.visibility = View.VISIBLE
                                binding.imageViewUserProfile.visibility = View.GONE
                                binding.layoutEmailPassword.visibility = View.GONE
                                binding.layoutUserSettings.visibility = View.GONE
                                editPreferences.putBoolean(binding.inputUserEmail.text!!.toString(), false)
                                editPreferences.putBoolean("UserLogged", false).apply()
                                alertDialog.cancel()
                            }
                        } else {
                            supportActionBar!!.title = "Вход"
                            binding.inputLayoutUserEmail.visibility = View.VISIBLE
                            binding.inputLayoutUserPassword.visibility = View.VISIBLE
                            binding.buttonSignIn.visibility = View.VISIBLE
                            binding.buttonResetPassword.visibility = View.VISIBLE
                            binding.imageViewUserProfile.visibility = View.GONE
                            binding.layoutEmailPassword.visibility = View.GONE
                            binding.layoutUserSettings.visibility = View.GONE
                            editPreferences.putBoolean("UserLogged", false).apply()
                            Toast.makeText(this, "Не удалось переавторизироваться!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                alertDialogDeleteUserBuilder.setNegativeButton("Отмена") { _, _ ->
                    vibrate()
                }
                val alertDialogDeleteUser = alertDialogDeleteUserBuilder.create()
                alertDialogDeleteUser.show()
            } else {
                Toast.makeText(this, "Вы не подключены\nк Интернету!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isValidEmail(inputText: CharSequence) : Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(inputText).matches()
    }

    private fun hidePassword(inputText: String) : String {
        var hiddenPassword = ""
        for (i in inputText.indices) {
            hiddenPassword += "•"
        }
        return hiddenPassword
    }

    private val navigationClickListener = (View.OnClickListener {
        vibrate()
        supportActionBar!!.title = "Профиль"
        binding.layoutEmailPassword.visibility = View.VISIBLE
        binding.layoutUserSettings.visibility = View.VISIBLE
        binding.imageViewUserProfile.visibility = View.VISIBLE
        binding.fabAddUser.visibility = View.VISIBLE
        binding.layoutChangeEmail.visibility = View.GONE
        binding.layoutChangePassword.visibility = View.GONE
        binding.buttonUpdateUser.visibility = View.GONE
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        supportActionBar!!.setDisplayShowHomeEnabled(false)
        mainMenu.findItem(R.id.buttonHomeScreen2).isVisible = true
        mainMenu.findItem(R.id.buttonSettings3).isVisible = true
    })

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun containsNumber(inputText: String) : Boolean {
        return inputText.contains("1") || inputText.contains("2") || inputText.contains("3") || inputText.contains("4") ||
                inputText.contains("5") || inputText.contains("6") || inputText.contains("7") || inputText.contains("8") ||
                inputText.contains("9") || inputText.contains("0")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val actionButtonsInflater = menuInflater
        actionButtonsInflater.inflate(R.menu.user_profile_activity_menu, menu)
        mainMenu = menu!!
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.buttonHomeScreen2 -> {
                vibrate()
                val activity = Intent(this, MainActivity::class.java)
                startActivity(activity)
                true
            }
            R.id.buttonSettings3 -> {
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
                binding.buttonSignIn.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
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