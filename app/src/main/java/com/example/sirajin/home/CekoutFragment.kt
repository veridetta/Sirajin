package com.example.sirajin.home

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.sirajin.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class CekoutFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var progressDialog: ProgressDialog? = null
    private lateinit var catatanHarianTextView: TextView
    private lateinit var btnTambahCatatanHarian: Button
    private lateinit var btnCekOut: Button
    lateinit var  cardBelum : CardView
    lateinit var  cardIsi : CardView
    lateinit var txJam :TextView
    lateinit var txTempat :TextView
    lateinit var txJamOut :TextView
    lateinit var txTempatOut :TextView
    lateinit var belumCekout:CardView
    lateinit var sudahCekout:CardView
    lateinit var daate : DocumentReference
    private var documentSnapshot : DocumentSnapshot? = null
     var siap : Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cekout, container, false)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        catatanHarianTextView = view.findViewById(R.id.catatan)
        btnTambahCatatanHarian = view.findViewById(R.id.btnTambahCatatanHarian)
        btnCekOut = view.findViewById(R.id.btnCekOut)
        cardBelum = view.findViewById(R.id.belumIsi)
        cardIsi = view.findViewById(R.id.sudahIsi)
        txJam = view.findViewById(R.id.txJam)
        txTempat = view.findViewById(R.id.txTempat)
        txJamOut = view.findViewById(R.id.txJamOut)
        txTempatOut = view.findViewById(R.id.txTempatOut)
        belumCekout = view.findViewById(R.id.belumCekout)
        sudahCekout = view.findViewById(R.id.sudahCekout)
        // Load data dari Firestore dan tampilkan ke TextView
        loadDataFromFirestore()

        btnTambahCatatanHarian.setOnClickListener {
            // Tampilkan modal form untuk menambahkan catatan harian
            val fragment = CatatanFragment()
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentContainer, fragment)
                ?.commit()
        }

        btnCekOut.setOnClickListener {
            if (siap){
                updateDataCekOut()
            }else{
                Toast.makeText(requireContext(), "Tambahkan catatan harian dahulu.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
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
    private fun getAbsen(){
        val nik = getNikFromSharedPreferences()
        // Ambil data catatan harian dari Firestore berdasarkan user ID (dalam contoh ini menggunakan UID dari FirebaseAuth)
        if (nik != null) {
            val currentDate = getCurrentDate()
            firestore.collection("absensi")
                .whereEqualTo("nik", nik)
                .whereEqualTo("tanggal", currentDate)
                .get()
                .addOnSuccessListener { documents ->
                    hideProgressDialog()
                    val jumlahData = documents.size()
                    if (!documents.isEmpty) {
                        // Jika ada catatan harian, tampilkan ke TextView
                        val catatan = StringBuilder()
                        for (document in documents) {
                            val hadirDiKampus = document.get("hadirDiKampus").toString()
                            val waktu = document.get("waktu").toString()
                            val status = document.get("status").toString()
                            val waktuSelesai = document.get("waktuCekOut").toString()
                            txJam.text = waktu
                            if(hadirDiKampus.equals("false")){
                                txTempat.text = "Learning from Home"
                                txTempatOut.text = "Learning from Home"
                            }else{
                                txTempat.text = "Hadir di Kampus"
                                txTempatOut.text = "Hadir di Kampus"
                            }
                            if(status.equals("masuk")){

                            }else{
                                sudahCekout.visibility = View.VISIBLE
                                belumCekout.visibility = View.GONE
                                txJamOut.text = waktuSelesai

                            }
                        }

                    } else {
                        // Jika belum ada catatan harian, tampilkan pesan
                    }
                }
                .addOnFailureListener { exception ->
                    hideProgressDialog()
                    // Gagal mengambil data dari Firestore
                    Toast.makeText(requireContext(), "Gagal mengambil data catatan harian.", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun loadDataFromFirestore() {
        val nik = getNikFromSharedPreferences()
        showProgressDialog("Mengambil data...")
        // Ambil data catatan harian dari Firestore berdasarkan user ID (dalam contoh ini menggunakan UID dari FirebaseAuth)
        if (nik != null) {
            getAbsen()
            val currentDate = getCurrentDate()
            firestore.collection("catatan")
                .whereEqualTo("nik", nik)
                .whereEqualTo("tanggal", currentDate)
                .get()
                .addOnSuccessListener { documents ->
                    hideProgressDialog()
                    val jumlahData = documents.size()
                    if (!documents.isEmpty) {
                        // Jika ada catatan harian, tampilkan ke TextView
                        val catatan = StringBuilder()
                        for (document in documents) {
                            val lampiranUrl = document.get("lampiranUrl").toString()
                            val output = document.get("output").toString()
                            val satuan = document.get("satuan").toString()
                            val tugas = document.get("tugas").toString()
                            val uraian = document.get("uraian").toString()
                            val waktuMulai = document.get("waktuMulai").toString()
                            val waktuSelesai = document.get("waktuSelesai").toString()
                            //catatan.append("Catatan Pokok: $catatanPokok, Catatan Tambahan: $catatanTambahan, Kreativitas: $kreativitas, Upacara: $upacara\n")
                        }
                        siap = true

                        catatanHarianTextView.text = jumlahData.toString()
                        cardIsi.visibility = View.VISIBLE
                        cardBelum.visibility = View.GONE
                    } else {
                        siap = false
                        // Jika belum ada catatan harian, tampilkan pesan
                        catatanHarianTextView.text = "0"
                        cardIsi.visibility = View.GONE
                        cardBelum.visibility = View.VISIBLE
                    }
                    if (siap) {
                        // Update waktuCekOut dan status saat tombol "Cek-out" diklik
                        btnCekOut.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.red)

                    } else {
                        btnCekOut.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey)

                    }
                }
                .addOnFailureListener { exception ->
                    siap = false
                    if (siap) {
                        // Update waktuCekOut dan status saat tombol "Cek-out" diklik
                        btnCekOut.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.red)

                    } else {
                        btnCekOut.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey)

                    }
                    hideProgressDialog()
                    // Gagal mengambil data dari Firestore
                    Toast.makeText(requireContext(), "Gagal mengambil data catatan harian.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateDataCekOut() {
        // Update waktuCekOut (jam) di Firestore saat tombol "Cek-out" diklik
        val nik = getNikFromSharedPreferences()
        showProgressDialog("Mengirim data...")
        if (nik != null) {
            firestore.collection("absensi")
                .whereEqualTo("nik", nik)
                .whereEqualTo("tanggal",getCurrentDate())
                .get()
                .addOnSuccessListener { documents ->
                    hideProgressDialog()
                    val documentSnapshot = documents.documents[0]
                    Log.d("Tag",documentSnapshot.id)
                    for (document in documents) {
                        // Update waktuCekOut (jam) ke waktu sekarang
                        val catatanRef = firestore.collection("absensi").document(documentSnapshot?.id ?: "")
                        val waktuCekOut = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val data = hashMapOf(
                            "waktuCekOut" to waktuCekOut,
                            "status" to "keluar"
                        )

                        catatanRef.update(data as Map<String, Any>)
                            .addOnSuccessListener {
                                // Data berhasil diupdate, tampilkan pesan sukses
                                Toast.makeText(requireContext(), "Data berhasil diupdate.", Toast.LENGTH_SHORT).show()
                                val fragment = CekoutFragment()
                                activity?.supportFragmentManager?.beginTransaction()
                                    ?.replace(R.id.fragmentContainer, fragment)
                                    ?.commit()
                            }
                            .addOnFailureListener { exception ->
                                // Gagal mengupdate data di Firestore
                                Toast.makeText(requireContext(), "Gagal mengupdate data.", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    hideProgressDialog()
                    // Gagal mengambil data dari Firestore
                    Toast.makeText(requireContext(), "Gagal mengambil data catatan harian.", Toast.LENGTH_SHORT).show()
                }
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
