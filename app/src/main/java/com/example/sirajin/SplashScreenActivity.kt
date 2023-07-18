package com.example.sirajin
import android.app.KeyguardManager
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricPrompt;
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.biometric.BiometricManager

import androidx.core.content.ContextCompat
import java.util.concurrent.Executor



class SplashScreenActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
     lateinit var btnLogin : Button
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    var isLoggedIn:Boolean =false
    private val REQUEST_PERMISSIONS = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        supportActionBar?.hide()
        sharedPreferences = getSharedPreferences("MyPrefs", AppCompatActivity.MODE_PRIVATE)
         isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        if(isLoggedIn){
            val biometricManager = BiometricManager.from(this)
            when (biometricManager.canAuthenticate()) {
                BiometricManager.BIOMETRIC_SUCCESS ->
                    displayMessage("Biometric authentication is available")
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                    displayMessage("This device doesn't support biometric authentication")
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                    displayMessage("Biometric authentication is currently unavailable")
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    displayMessage("No biometric credentials are enrolled")
            }
            val executor = ContextCompat.getMainExecutor(this)
            biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    displayMessage("Authentication error: $errString")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    displayMessage("Authentication succeeded!")
                    checkLoginStatus()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    displayMessage("Authentication failed")
                }
            })

            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Cancel")
                .build()
        }
        btnLogin = findViewById(R.id.loginButton)
        btnLogin.setOnClickListener{
            checkForBiometrics()
        }
    }
    private fun checkForBiometrics() {
        if (isBiometricAvailable()) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            // Biometric authentication is not available on this device or not enrolled
            // Handle this case accordingly (e.g., show regular login)
            checkLoginStatus()
        }
    }
    private fun checkLoginStatus() {
        // Cek Shared Preferences apakah pengguna sudah login atau belum
        if (isLoggedIn) {
            // Pengguna sudah login, pindah ke halaman utama (MainActivity)
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        } else {
            // Pengguna belum login, pindah ke halaman login (LoginActivity)
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Tutup Splashscreen activity
        finish()
    }
    private fun displayMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    private fun isBiometricAvailable(): Boolean {
        var hh:Boolean=false
        if(isLoggedIn){
            val biometricManager = BiometricManager.from(this)
             hh = biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
        }
        return hh
    }
}
