package com.arduinoworld.smartremote

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Patterns
import android.view.HapticFeedbackConstants
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arduinoworld.smartremote.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth


@Suppress("DEPRECATION")
@SuppressLint("InflateParams")
class SignUpActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySignUpBinding
    private var isHapticFeedbackEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSignUp)
        supportActionBar!!.title = "Регистрация"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val firebaseAuth = FirebaseAuth.getInstance()

        val sharedPreferences = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)
        isHapticFeedbackEnabled = sharedPreferences.getBoolean("HapticFeedbackEnabled", true)

        var userCredentialNumber = 0
        binding.buttonForward.setOnClickListener {
            vibrate()
            if (userCredentialNumber == 0) {
                if (binding.inputEmail.text!!.isNotEmpty()) {
                    if (isValidEmail(binding.inputEmail.text!!)) {
                        hideKeyboard()
                        userCredentialNumber += 1
                        binding.inputLayoutEmail.isErrorEnabled = false
                        binding.layoutUserEmail.visibility = View.GONE
                        binding.layoutUserPassword.visibility = View.VISIBLE
                        binding.buttonBack.visibility = View.VISIBLE
                    } else {
                        binding.inputLayoutEmail.isErrorEnabled = true
                        binding.inputLayoutEmail.error = "Вы ввели неправильную почту"
                    }
                } else {
                    binding.inputLayoutEmail.isErrorEnabled = true
                    binding.inputLayoutEmail.error = "Введите почту пользователя"
                }
            } else if (userCredentialNumber == 1) {
                if (binding.inputPassword.text!!.isNotEmpty()) {
                    if (binding.inputPassword.text!!.length >= 6) {
                        if (containsNumber(binding.inputPassword.text!!.toString())) {
                            hideKeyboard()
                            userCredentialNumber += 1
                            binding.inputLayoutPassword.isErrorEnabled = false
                            binding.layoutUserPassword.visibility = View.GONE
                            binding.buttonForward.visibility = View.GONE
                            binding.layoutConfirmPassword.visibility = View.VISIBLE
                            binding.buttonSignUp.visibility = View.VISIBLE
                        } else {
                            binding.inputLayoutPassword.isErrorEnabled = true
                            binding.inputLayoutPassword.error = "Пароль должен иметь хотя бы 1 цифру"
                        }
                    } else {
                        binding.inputLayoutPassword.isErrorEnabled = true
                        binding.inputLayoutPassword.error = "Пароль должен быть не меньше 6 символов"
                    }
                } else {
                    binding.inputLayoutPassword.isErrorEnabled = true
                    binding.inputLayoutPassword.error = "Введите пароль пользователя"
                }
            }
        }

        binding.buttonBack.setOnClickListener {
            vibrate()
            hideKeyboard()
            if (userCredentialNumber == 2) {
                userCredentialNumber -= 1
                binding.layoutConfirmPassword.visibility = View.GONE
                binding.layoutUserPassword.visibility = View.VISIBLE
                binding.buttonForward.visibility = View.VISIBLE
                binding.buttonSignUp.visibility = View.VISIBLE
            } else if (userCredentialNumber == 1) {
                userCredentialNumber -= 1
                binding.layoutUserPassword.visibility = View.GONE
                binding.layoutUserEmail.visibility = View.VISIBLE
                binding.buttonBack.visibility = View.GONE
            }
        }

        binding.buttonSignUp.setOnClickListener {
            vibrate()
            if (isNetworkConnected()) {
                if (binding.inputConfirmPassword.text!!.isNotEmpty()) {
                    if (binding.inputConfirmPassword.text!!.toString() == binding.inputPassword.text!!.toString()) {
                        hideKeyboard()
                        val alertDialogBuilder : AlertDialog.Builder = AlertDialog.Builder(this)
                        alertDialogBuilder.setView(layoutInflater.inflate(R.layout.sign_up_progress_bar, null))
                        val alertDialog = alertDialogBuilder.create()
                        alertDialog.setCanceledOnTouchOutside(false)
                        alertDialog.show()
                        firebaseAuth.createUserWithEmailAndPassword(binding.inputEmail.text.toString(), binding.inputPassword.text.toString())
                                .addOnCompleteListener(this) { createUserTask ->
                                    if (createUserTask.isSuccessful) {
                                        Toast.makeText(this, "Пользователь успешно зарегистрирован!", Toast.LENGTH_SHORT).show()
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            if (sharedPreferences.getBoolean("UserLogged", false)) {
                                                supportActionBar?.title = "Профиль"
                                            } else {
                                                supportActionBar?.title = "Вход"
                                            }
                                        }
                                        val activity = Intent(this, UserProfile::class.java)
                                        startActivity(activity)
                                        finish()
                                    } else {
                                        userCredentialNumber = 0
                                        binding.layoutConfirmPassword.visibility = View.GONE
                                        binding.buttonBack.visibility = View.GONE
                                        binding.layoutUserEmail.visibility = View.VISIBLE
                                        binding.buttonForward.visibility = View.VISIBLE
                                        binding.inputLayoutEmail.isErrorEnabled = true
                                        binding.inputLayoutEmail.error = "Эта почта уже существует"
                                    }
                                    alertDialog.cancel()
                                }
                    } else {
                        binding.inputLayoutConfirmPassword.isErrorEnabled = true
                        binding.inputLayoutConfirmPassword.error = "Пароль не совпадает"
                    }
                } else {
                    binding.inputLayoutConfirmPassword.isErrorEnabled = true
                    binding.inputLayoutConfirmPassword.error = "Подтвердите пароль пользователя"
                }
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

    private fun isValidEmail(inputText: CharSequence) : Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(inputText).matches()
    }

    private fun containsNumber(inputText: String) : Boolean {
        return inputText.contains("1") || inputText.contains("2") || inputText.contains("3") || inputText.contains("4") ||
                inputText.contains("5") || inputText.contains("6") || inputText.contains("7") || inputText.contains("8") ||
                inputText.contains("9") || inputText.contains("0")
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
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
                binding.buttonForward.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING + HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
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