package com.example.sirajin.home

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.sirajin.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CatatanFragment : Fragment() {

    private lateinit var tugasEditText: Spinner
    private lateinit var uraianEditText: EditText
    private lateinit var outputEditText: Spinner
    private lateinit var satuanSpinner: Spinner
    private lateinit var waktuMulaiEditText: EditText
    private lateinit var waktuSelesaiEditText: EditText
    private lateinit var lampiranButton: Button
    private lateinit var uploadButton: Button
    private var progressDialog: ProgressDialog? = null
    private lateinit var selectedFileUri: Uri
    private lateinit var storageReference: StorageReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var diluar : LinearLayout
    private lateinit var didalam : LinearLayout

    private val FILE_PICKER_REQUEST_CODE = 1
    private val PERMISSIONS_REQUEST_CODE = 2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_catatan, container, false)

        tugasEditText = view.findViewById(R.id.tugasEditText)
        uraianEditText = view.findViewById(R.id.uraianKegiatanEditText)
        outputEditText = view.findViewById(R.id.outputEditText)
        satuanSpinner = view.findViewById(R.id.satuanSpinner)
        waktuMulaiEditText = view.findViewById(R.id.waktuMulaiEditText)
        waktuSelesaiEditText = view.findViewById(R.id.waktuSelesaiEditText)
        lampiranButton = view.findViewById(R.id.pilihFileButton)
        uploadButton = view.findViewById(R.id.uploadButton)
        diluar = view.findViewById(R.id.nonkelas)
        didalam = view.findViewById(R.id.dikelas)

        storageReference = FirebaseStorage.getInstance().reference
        firestore = FirebaseFirestore.getInstance()
        getAbsen()
        val satuanAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.satuan_array,
            android.R.layout.simple_spinner_item
        )
        satuanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        satuanSpinner.adapter = satuanAdapter

        val tugasAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.tugas_array,
            android.R.layout.simple_spinner_item
        )
        tugasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tugasEditText.adapter = tugasAdapter

        val outputAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.output_array,
            android.R.layout.simple_spinner_item
        )
        outputAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        outputEditText.adapter = outputAdapter

        lampiranButton.setOnClickListener {
            openFilePicker()
        }

        uploadButton.setOnClickListener {
            uploadCatatan()
        }

        return view
    }
    private fun getCurrentDateTime(): Pair<String, String> {
        val currentTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = dateFormat.format(currentTime)
        val time = timeFormat.format(currentTime)
        return Pair(date, time)
    }

    private fun getNikFromSharedPreferences(): String {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("nik", "") ?: ""
    }
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        val mimeTypes = arrayOf(
            "image/jpeg", "image/png", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "application/rtf", "application/pdf"
        )
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
    }

    private fun uploadCatatan() {

        val tugas = tugasEditText.selectedItem.toString()
        val uraian = uraianEditText.text.toString()
        val output = outputEditText.selectedItem.toString()
        val satuan = satuanSpinner.selectedItem.toString()
        val waktuMulai = waktuMulaiEditText.text.toString()
        val waktuSelesai = waktuSelesaiEditText.text.toString()

        // Cek apakah semua field telah diisi
        if (tugas.isEmpty() || uraian.isEmpty() || output.isEmpty() || satuan.isEmpty() ||
            waktuMulai.isEmpty() || waktuSelesai.isEmpty() || !this::selectedFileUri.isInitialized
        ) {
            Toast.makeText(requireContext(), "Harap lengkapi semua field", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi ukuran file
        val fileSize = getFileSize(selectedFileUri)
        if (fileSize > 20 * 1024 * 1024) { // 20 MB
            Toast.makeText(requireContext(), "Ukuran file terlalu besar. Maksimal 20 MB", Toast.LENGTH_SHORT).show()
            return
        }

        // Upload file ke Firebase Storage
        val storageRef = storageReference.child("lampiran/${selectedFileUri.lastPathSegment}")
        showProgressDialog("Mengirim data...")
        storageRef.putFile(selectedFileUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val lampiranUrl = uri.toString()
                    val nik = getNikFromSharedPreferences()
                    val (tanggal, waktu) = getCurrentDateTime() // Mendapatkan tanggal dan waktu terpisah
                    // Simpan data catatan ke Firestore
                    val catatan = hashMapOf(
                        "tugas" to tugas,
                        "uraian" to uraian,
                        "output" to output,
                        "satuan" to satuan,
                        "waktuMulai" to waktuMulai,
                        "waktuSelesai" to waktuSelesai,
                        "lampiranUrl" to lampiranUrl,
                        "nik" to nik,
                        "tanggal" to tanggal
                    )

                    firestore.collection("catatan")
                        .add(catatan)
                        .addOnSuccessListener {
                            hideProgressDialog()
                            Toast.makeText(requireContext(), "Berhasil menambah catatan", Toast.LENGTH_SHORT).show()

                            // Clear input fields
                            tugasEditText.setSelection(0)
                            uraianEditText.text.clear()
                            outputEditText.setSelection(0)
                            satuanSpinner.setSelection(0)
                            waktuMulaiEditText.text.clear()
                            waktuSelesaiEditText.text.clear()
                            selectedFileUri = Uri.EMPTY
                            val fragmentManager = requireActivity().supportFragmentManager
                            fragmentManager.beginTransaction()
                                .replace(R.id.fragmentContainer, HomeFragment()) // Replace fragment_container with your container id
                                .addToBackStack(null)
                                .commit()
                        }
                        .addOnFailureListener { exception ->
                            hideProgressDialog()
                            Toast.makeText(requireContext(), "Gagal menyimpan catatan", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                hideProgressDialog()
                Toast.makeText(requireContext(), "Gagal mengunggah file", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getFileSize(uri: Uri): Long {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
        cursor?.moveToFirst()
        val size = cursor?.getLong(sizeIndex ?: 0) ?: 0
        cursor?.close()
        return size
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                selectedFileUri = it
                try {
                    val fileName = requireContext().contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        cursor.getString(nameIndex)
                    }
                    lampiranButton.text = fileName
                } catch (e: IOException) {
                    Toast.makeText(requireContext(), "Gagal membaca file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        fun newInstance(): CatatanFragment {
            return CatatanFragment()
        }
    }
    private fun showProgressDialog(message: String) {
        progressDialog = ProgressDialog(activity)
        progressDialog?.setMessage(message)
        progressDialog?.setCancelable(false)
        progressDialog?.show()
    }
    private fun getCurrentDate(): String {
        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(currentDate)
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
    private fun getAbsen(){
        showProgressDialog("Mengambil data...")
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
                            if(status.equals("masuk")){
                                diluar.visibility = View.GONE
                                didalam.visibility = View.VISIBLE
                            }else{
                                diluar.visibility = View.VISIBLE
                                didalam.visibility = View.GONE
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
}
