package com.example.sirajin.home

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.sirajin.R
import com.example.sirajin.home.CatatanFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*


class AbsensiFragment : Fragment() {

    private lateinit var radioGroup: RadioGroup
    private lateinit var hadirRadio: RadioButton
    private lateinit var learningRadio: RadioButton
    private lateinit var uploadFotoButton: Button
    private lateinit var checkinButton: Button
    private var progressDialog: ProgressDialog? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var storageReference: StorageReference
    private lateinit var firestore: FirebaseFirestore

    private var currentLocation: Location? = null
    private var selectedImage: Bitmap? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_absensi, container, false)

        radioGroup = view.findViewById(R.id.radioGroup)
        hadirRadio = view.findViewById(R.id.hadirRadio)
        learningRadio = view.findViewById(R.id.learningRadio)
        uploadFotoButton = view.findViewById(R.id.uploadFotoButton)
        checkinButton = view.findViewById(R.id.checkinButton)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        getLastKnownLocation()
        storageReference = FirebaseStorage.getInstance().reference
        firestore = FirebaseFirestore.getInstance()

        uploadFotoButton.setOnClickListener {
            dispatchTakePictureIntent()
        }
        val (tanggal, _) = getCurrentDateTime()

        checkAbsensiOnDate(tanggal,
            onSuccess = {
                // Data absensi sudah ada pada tanggal ini, alihkan ke CekOutFragment
                val cekOutFragment = CekoutFragment()
                val fragmentManager = requireActivity().supportFragmentManager
                fragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, cekOutFragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        checkinButton.setOnClickListener {
            if (currentLocation != null) {
                val radius = calculateDistance(currentLocation!!)
                val isHadirDiKampus = hadirRadio.isChecked

                if (isHadirDiKampus && radius < 100) {
                    if (selectedImage != null) {
                        uploadImageToFirebase()
                    } else {
                        Toast.makeText(requireContext(), "Pilih foto sebelum Check-in", Toast.LENGTH_SHORT).show()
                    }
                } else if (!isHadirDiKampus) {
                    if (selectedImage != null) {
                        uploadImageToFirebase()
                    } else {
                        Toast.makeText(requireContext(), "Pilih foto sebelum Check-in", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Anda belum berada di kampus", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireContext().packageManager) != null) {
            if (checkCameraPermission()) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } else {
                requestCameraPermission()
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Camera permission not granted. Unable to take a picture.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            selectedImage = imageBitmap
            showSelectedImage(imageBitmap)
            Log.d(TAG, "Image selected and displayed")
        } else {
            Log.d(TAG, "Image selection canceled or failed")
        }
    }
    private fun showSelectedImage(imageBitmap: Bitmap) {
        val selectedImageView: ImageView = view?.findViewById(R.id.selectedImageView) ?: return
        Glide.with(requireContext())
            .load(imageBitmap)
            .into(selectedImageView)
        Log.d(TAG, "Image displayed in ImageView using Glide $imageBitmap")
    }

    private fun uploadImageToFirebase() {
        showProgressDialog("Mengirim data...")
        val nik = getNikFromSharedPreferences()
        val nama = getNamaFromSharedPreferences()
        val (tanggal, waktu) = getCurrentDateTime() // Mendapatkan tanggal dan waktu terpisah

        val baos = ByteArrayOutputStream()
        selectedImage?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val storageRef = storageReference.child("images/$nik-$tanggal-$waktu.jpg")
        val uploadTask = storageRef.putBytes(data)
        uploadTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    if (currentLocation != null) {
                        // Mendapatkan latitude dan longitude dari lokasi terakhir
                        val latitude = currentLocation?.latitude ?: 0.0
                        val longitude = currentLocation?.longitude ?: 0.0

                        // Menyimpan data ke Firestore termasuk latitude dan longitude
                        saveDataToFirestore(nik, nama, tanggal, waktu, downloadUrl, latitude, longitude)
                    } else {
                        hideProgressDialog()
                        Toast.makeText(requireContext(), "Lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                hideProgressDialog()
                Toast.makeText(requireContext(), "Gagal mengunggah foto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveDataToFirestore(nik: String, nama: String, tanggal: String, waktu:String, downloadUrl: String,
                                    latitude: Double,
                                    longitude: Double) {
        val isHadirDiKampus = hadirRadio.isChecked

        val data = hashMapOf(
            "nik" to nik,
            "nama" to nama,
            "tanggal" to tanggal,
            "waktu" to waktu,
            "waktuCekOut" to "",
            "hadirDiKampus" to isHadirDiKampus,
            "imageUrl" to downloadUrl,
            "latitude" to latitude,
            "longitude" to longitude,
            "status" to "masuk"
        )

        firestore.collection("absensi")
            .add(data)
            .addOnSuccessListener { documentReference ->
                hideProgressDialog()
                // Data added successfully, navigate to CatatanFragment

                val catatanFragment = CatatanFragment()
                val bundle = Bundle()
                // Optionally, you can pass any data to the CatatanFragment using the Bundle
                // bundle.putString("key", "value")
                catatanFragment.arguments = bundle

                // Perform fragment transaction to navigate to CatatanFragment
                val fragmentManager = requireActivity().supportFragmentManager
                fragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, catatanFragment) // Replace fragment_container with your container id
                    .addToBackStack(null)
                    .commit()
            }
            .addOnFailureListener { exception ->
                hideProgressDialog()
                Toast.makeText(requireContext(), "Gagal menyimpan data absensi", Toast.LENGTH_SHORT).show()
            }

    }

    private fun calculateDistance(location: Location): Float {
        val kampusLocation = Location("")
        kampusLocation.latitude = -6.366752  // Koordinat latitude Fakultas Teknik UI
        kampusLocation.longitude = 106.824961  // Koordinat longitude Fakultas Teknik UI
        return location.distanceTo(kampusLocation)
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

    private fun getNamaFromSharedPreferences(): String {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("fullName", "") ?: ""
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun getLastKnownLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLocation = location
                    } else {
                        fusedLocationClient.requestLocationUpdates(
                            LocationRequest.create(),
                            object : LocationCallback() {
                                override fun onLocationResult(result: LocationResult) {
                                    result ?: return
                                    for (location in result.locations) {
                                        currentLocation = location
                                        break
                                    }
                                    fusedLocationClient.removeLocationUpdates(this)
                                }
                            },
                            null
                        )
                    }
                }
        } else {
            requestLocationPermission()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val TAG = "AbsensiFragment"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }

    private fun checkAbsensiOnDate(tanggal: String, onSuccess: () -> Unit) {
        showProgressDialog("Mengambil data...")
        val nik = getNikFromSharedPreferences()

        firestore.collection("absensi")
            .whereEqualTo("nik", nik)
            .whereEqualTo("tanggal", tanggal)
            .get()
            .addOnSuccessListener { result ->
                hideProgressDialog()
                if (result.isEmpty) {
                    // Data absensi belum ada pada tanggal ini
                } else {
                    // Data absensi sudah ada pada tanggal ini
                    onSuccess()
                }
            }
            .addOnFailureListener { exception ->
                hideProgressDialog()
                // Handle error
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
