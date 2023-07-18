package com.example.sirajin

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var fullNameEditText: EditText
    private lateinit var nikEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signupButton: Button
    private lateinit var loginButton: Button
    private var progressDialog: ProgressDialog? = null
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        supportActionBar?.hide()
        firestore = FirebaseFirestore.getInstance()

        fullNameEditText = findViewById(R.id.fullNameEditText)
        nikEditText = findViewById(R.id.nikEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        signupButton = findViewById(R.id.signupButton)
        loginButton = findViewById(R.id.loginButton)

        signupButton.setOnClickListener {
            signUp()
        }

        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun signUp() {
        showProgressDialog("Mengirim data...")
        val fullName = fullNameEditText.text.toString().trim()
        val nik = nikEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(nik) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Isi semua field yang diperlukan", Toast.LENGTH_SHORT).show()
            return
        }

        // Simpan data pengguna ke Firestore
        val user = hashMapOf(
            "fullName" to fullName,
            "nik" to nik,
            "password" to password
            // Tambahkan data pengguna lainnya yang diperlukan
        )

        firestore.collection("users")
            .document(nik)
            .set(user)
            .addOnSuccessListener {
                hideProgressDialog()
                Toast.makeText(this, "Pendaftaran berhasil. Silakan login.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Toast.makeText(this, "Pendaftaran gagal. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
            }
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

