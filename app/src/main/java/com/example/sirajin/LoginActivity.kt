package com.example.sirajin
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var nikEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: Button
    //private lateinit var showPasswordCheckBox: CheckBox
    private var progressDialog: ProgressDialog? = null

    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()
        // Initialize FirebaseApp
        FirebaseApp.initializeApp(this);
        firestore = FirebaseFirestore.getInstance()

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        nikEditText = findViewById(R.id.nikEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        signupButton = findViewById(R.id.signupButton)
        ///showPasswordCheckBox = findViewById(R.id.showPasswordCheckBox)
// Set a listener to the CheckBox to toggle password visibility
        /* showPasswordCheckBox.setOnCheckedChangeListener { _, isChecked ->
            togglePasswordVisibility(isChecked)
        }*/
        loginButton.setOnClickListener {
            login()
        }

        signupButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    private fun login() {
        showProgressDialog("Logging in...")
        val nik = nikEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (TextUtils.isEmpty(nik) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Isi semua field yang diperlukan", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil data pengguna dari Firestore
        firestore.collection("users")
            .document(nik)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                hideProgressDialog()
                val user = documentSnapshot.toObject(User::class.java)

                if (user != null && user.password == password) {
                    // Simpan data pengguna ke Shared Preferences
                    val editor = sharedPreferences.edit()
                    editor.putString("nik", user.nik)
                    editor.putBoolean("isLoggedIn",true)
                    editor.putString("fullName", user.fullName)
                    editor.apply()

                    Toast.makeText(this, "Login berhasil.", Toast.LENGTH_SHORT).show()
                    // Lakukan sesuatu setelah berhasil login
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Login gagal. Periksa kembali nik dan password Anda.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Toast.makeText(this, "Login gagal. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
            }

    }
    private fun togglePasswordVisibility(showPassword: Boolean) {
        // Get the input type of the EditText
        val inputType = if (showPassword) {
            // If showPassword is true, show the password text
            android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            // If showPassword is false, hide the password text
            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        // Set the new input type to the EditText
        passwordEditText.inputType = inputType

        // Move the cursor to the end of the text to maintain the cursor position
        passwordEditText.setSelection(passwordEditText.text?.length ?: 0)
    }
    private fun showProgressDialog(message: String) {
        progressDialog = ProgressDialog(this)
        progressDialog?.setMessage(message)
        progressDialog?.setCancelable(false)
        progressDialog?.show()
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

}
