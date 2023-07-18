package com.example.sirajin.home

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.sirajin.R
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var sehatRadioGroup: RadioGroup
    private lateinit var rasakanEditText: EditText
    private lateinit var berobatRadioGroup: RadioGroup
    private lateinit var lanjutButton: Button
    private var progressDialog: ProgressDialog? = null
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        sehatRadioGroup = view.findViewById(R.id.sehatRadioGroup)
        rasakanEditText = view.findViewById(R.id.rasakanEditText)
        berobatRadioGroup = view.findViewById(R.id.berobatRadioGroup)
        lanjutButton = view.findViewById(R.id.lanjutButton)

        firestore = FirebaseFirestore.getInstance()

        val nik = getNikFromSharedPreferences()

        if (nik != null) {
            val currentDate = getCurrentDate()
            showProgressDialog("Mengambil data...")
            firestore.collection("status_kesehatan")
                .whereEqualTo("nik", nik)
                .whereEqualTo("tanggal", currentDate)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    hideProgressDialog()
                    if (!querySnapshot.isEmpty) {
                        val fragment = AbsensiFragment()
                        activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.fragmentContainer, fragment)
                            ?.commit()
                    } else {
                        setupForm(nik)
                    }
                }
                .addOnFailureListener { exception ->
                    hideProgressDialog()
                    handleError(exception)
                }
        }

        return view
    }

    private fun setupForm(nik: String) {
        lanjutButton.setOnClickListener {
            showProgressDialog("Mengirim data...")
            val selectedSehatRadioButtonId = sehatRadioGroup.checkedRadioButtonId
            val sehatRadioButton = view?.findViewById<RadioButton>(selectedSehatRadioButtonId)
            val sehat = sehatRadioButton?.text.toString()

            val rasakan = rasakanEditText.text.toString()

            val selectedBerobatRadioButtonId = berobatRadioGroup.checkedRadioButtonId
            val berobatRadioButton = view?.findViewById<RadioButton>(selectedBerobatRadioButtonId)
            val berobat = berobatRadioButton?.text.toString()

            val currentDate = getCurrentDate()

            val data = hashMapOf(
                "nik" to nik,
                "sehat" to sehat,
                "rasakan" to rasakan,
                "berobat" to berobat,
                "tanggal" to currentDate,
                "status" to "sudah"
            )

            firestore.collection("status_kesehatan")
                .document()
                .set(data)
                .addOnSuccessListener {
                    hideProgressDialog()
                    val fragment = AbsensiFragment()
                    activity?.supportFragmentManager?.beginTransaction()
                        ?.replace(R.id.fragmentContainer, fragment)
                        ?.commit()
                }
                .addOnFailureListener { exception ->
                    hideProgressDialog()
                    handleError(exception)
                }
        }
    }

    private fun getCurrentDate(): String {
        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(currentDate)
    }

    private fun getNikFromSharedPreferences(): String? {
        val sharedPreferences = activity?.getSharedPreferences("MyPrefs", AppCompatActivity.MODE_PRIVATE)
        return sharedPreferences?.getString("nik", null)
    }

    private fun handleError(exception: Exception) {
        // Menggunakan Firebase Exception
        if (exception is FirebaseException) {
            when (exception) {
                is FirebaseNetworkException -> {
                    // Terjadi kesalahan jaringan, tampilkan pesan kesalahan kepada pengguna
                    Toast.makeText(activity, "Terjadi kesalahan jaringan", Toast.LENGTH_SHORT).show()
                }
                is FirebaseTooManyRequestsException -> {
                    // Terlalu banyak permintaan, tampilkan pesan kesalahan kepada pengguna
                    Toast.makeText(activity, "Terlalu banyak permintaan. Coba lagi nanti.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Kesalahan Firebase lainnya, tampilkan pesan kesalahan secara umum
                    Toast.makeText(activity, "Terjadi kesalahan. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Kesalahan lainnya, tampilkan pesan kesalahan secara umum
            Toast.makeText(activity, "Terjadi kesalahan. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
        }
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
