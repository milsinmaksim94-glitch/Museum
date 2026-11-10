package com.museum.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.museum.app.R
import com.museum.app.utils.SessionManager

// Главный экран с нижним меню. Сам почти ничего не делает,
// только переключает фрагменты
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // на всякий случай: если сессии нет, отправляем на вход
        if (!SessionManager(this).isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> openFragment(HomeFragment(), R.string.nav_home)
                R.id.nav_search -> openFragment(SearchFragment(), R.string.nav_search)
                R.id.nav_tours -> openFragment(ToursFragment(), R.string.nav_tours)
                R.id.nav_profile -> openFragment(ProfileFragment(), R.string.nav_profile)
            }
            true
        }

        // при повороте экрана фрагмент восстановится сам, поэтому
        // открываем главную только при первом запуске
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        } else {
            title = savedInstanceState.getString("title", getString(R.string.app_name))
        }
    }

    private fun openFragment(fragment: Fragment, titleRes: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        setTitle(titleRes)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("title", title.toString())
    }
}
