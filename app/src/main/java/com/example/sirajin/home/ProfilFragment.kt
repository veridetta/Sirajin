package com.example.sirajin.home

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.sirajin.LoginActivity
import com.example.sirajin.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class ProfilFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var fullName: String = "" // Nama lengkap pengguna
    private var currentPassword: String = "" // Kata sandi saat ini
    private lateinit var currentNik: String // Nik sebagai acuan
    private lateinit var editNameEditText: TextInputEditText
    private lateinit var editPasswordEditText: TextInputEditText
    private var progressDialog: ProgressDialog? = null
    private var documentSnapshot : DocumentSnapshot? = null
    lateinit var daate : DocumentReference
    private lateinit var logoutButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profil, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Ambil nik dari SharedPreferences sebagai acuan
        currentNik = getNikFromSharedPreferences()

        // Ambil nama lengkap pengguna dari SharedPreferences
        fullName = getFullNameFromSharedPreferences()
        editNameEditText = view.findViewById(R.id.editNameEditText)
        editPasswordEditText = view.findViewById(R.id.editPasswordEditText)
        logoutButton = view.findViewById(R.id.logoutButton)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        // Tampilkan data pengguna saat ini di bidang edit
        fetchUserData()
        logout()
        // Aksi ketika tombol "Simpan" ditekan
        saveButton.setOnClickListener {
            saveChanges()
        }
    }
    fun logout(){

        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", AppCompatActivity.MODE_PRIVATE)

        logoutButton.setOnClickListener {
            // Clear login session data
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()

            // Redirect the user back to the login screen
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)

            // Finish the current activity to prevent going back to it after logout
            requireActivity().finish()
        }
    }
    private fun fetchUserData() {
        showProgressDialog("Mengambil data...")
        val nik = getNikFromSharedPreferences()

        if (nik != null) {

            firestore.collection("users")
                .whereEqualTo("nik", nik)
                .get()
                .addOnSuccessListener { documents ->
                    hideProgressDialog()
                    if (!documents.isEmpty) {
                        val documentSnapshot = documents.documents[0]
                        //val fullName = documentSnapshot.getString("fullName") ?: ""
                        currentPassword = documentSnapshot.getString("password") ?: ""

                        editNameEditText.setText(fullName)
                        editPasswordEditText.setText(currentPassword)
                    } else {
                        // Tidak ada data pengguna dengan nik yang sesuai
                        showSnackbar("Tidak dapat menemukan data pengguna")
                    }
                }
                .addOnFailureListener { exception ->
                    hideProgressDialog()
                    showSnackbar("Gagal mengambil data pengguna")
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    private fun saveChanges() {
        val newName = editNameEditText.text.toString()
        val newPassword = editPasswordEditText.text.toString()
        showProgressDialog("Mengirim data...")
            val nik = getNikFromSharedPreferences()
            // Pastikan hanya pengguna dengan nik yang sesuai yang dapat menyimpan perubahan
            if (nik != null && nik == currentNik) {
                daate = firestore.collection("users").document(nik)
                // Simpan perubahan nama dan kata sandi ke Firestore
                val userMap = hashMapOf(
                    "fullName" to newName,
                    "password" to newPassword
                    // Tambahkan field dan nilai yang ingin diupdate
                )

                firestore.collection("users")
                    .document(documentSnapshot?.id ?: "")
                    .update(userMap as Map<String, Any>)
                    .addOnSuccessListener {
                        hideProgressDialog()
                        // Simpan nama lengkap baru ke SharedPreferences
                        saveFullNameToSharedPreferences(newName)

                        showSnackbar("Perubahan telah disimpan")
                    }
                    .addOnFailureListener { exception ->
                        hideProgressDialog()
                        showSnackbar("Gagal menyimpan perubahan")
                    }
            } else {
                showSnackbar("Anda tidak memiliki izin untuk menyimpan perubahan")
            }
    }


    private fun getNikFromSharedPreferences(): String {
        val sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("nik", "") ?: ""
    }

    private fun getFullNameFromSharedPreferences(): String {
        val sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("fullName", "") ?: ""
    }

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    private fun saveFullNameToSharedPreferences(fullName: String) {
        val sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("fullName", fullName)
        editor.apply()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }
    private fun showProgressDialog(message: String) {
        progressDialog = ProgressDialog(activity)
        progressDialog?.setMessage(message)
        progressDialog?.setCancelable(false)
        progressDialog?.show()
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}
