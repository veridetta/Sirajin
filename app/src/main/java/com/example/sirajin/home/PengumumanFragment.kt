package com.example.sirajin.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.sirajin.Pengumuman

import com.example.sirajin.R
import com.google.firebase.firestore.FirebaseFirestore
import org.imaginativeworld.whynotimagecarousel.ImageCarousel
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem
import java.text.SimpleDateFormat
import java.util.*

class PengumumanFragment : Fragment() {

    private lateinit var imageCarousel: ImageCarousel
    private lateinit var firestore: FirebaseFirestore
    private val pengumumanList = mutableListOf<Pengumuman>()
    val list = mutableListOf<CarouselItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pengumuman, container, false)

        imageCarousel = view.findViewById(R.id.carouselView)
        imageCarousel.registerLifecycle(lifecycle)
        firestore = FirebaseFirestore.getInstance()

        fetchPengumumanData()

        return view
    }

    private fun fetchPengumumanData() {
        val currentDate = getCurrentDate()

        firestore.collection("pengumuman")
            .whereLessThanOrEqualTo("validUntil", currentDate)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    //val pengumuman = document.toObject(Pengumuman::class.java)
                    val imageUri = document.getString("imageUrl") ?: ""
                    val title = document.getString("title")?: ""
                    val description = document.getString("description")?: ""
                    //val carouselItem = Pengumuman(imageUrl, title, description)
                    // Image URL with caption
                    list.add(
                        CarouselItem(
                            imageUrl = imageUri,
                            caption = title
                        )
                    )
//                    pengumumanList.add(carouselItem)
                }

                // Set data pengumuman ke carousel view
                setupCarouselView()
            }
            .addOnFailureListener { exception ->
                // Handle error
            }
    }
    private fun setupCarouselView() {
        imageCarousel.setData(list)
    }

    private fun getCurrentDate(): String {
        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(currentDate)
    }

}
