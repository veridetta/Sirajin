package com.example.sirajin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.sirajin.home.CatatanFragment
import com.example.sirajin.home.HomeFragment
import com.example.sirajin.home.PengumumanFragment
import com.example.sirajin.home.ProfilFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fragmentContainer: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        supportActionBar?.hide()
        bottomNavigationView = findViewById(R.id.bottomNavigationBar)
        fragmentContainer = findViewById(R.id.fragmentContainer)

        // Set the initial fragment to display
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, HomeFragment())
                        .commit()
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_catatan -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, CatatanFragment())
                        .commit()
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_pengumuman -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, PengumumanFragment())
                        .commit()
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_profil -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ProfilFragment())
                        .commit()
                    return@setOnNavigationItemSelectedListener true
                }
                else -> return@setOnNavigationItemSelectedListener false
            }
        }
    }

}
