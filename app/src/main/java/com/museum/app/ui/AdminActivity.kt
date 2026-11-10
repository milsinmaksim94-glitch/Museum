package com.museum.app.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.museum.app.R
import com.museum.app.utils.SessionManager

// Панель администратора: 3 вкладки - экспонаты, залы, брони
class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // защита: обычный пользователь сюда попасть не должен
        if (!SessionManager(this).isAdmin()) {
            Toast.makeText(this, R.string.error_no_access, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.activity_admin)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = AdminPagerAdapter(this)

        val titles = arrayOf(
            getString(R.string.tab_exhibits),
            getString(R.string.tab_halls),
            getString(R.string.tab_bookings)
        )
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }

    class AdminPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AdminExhibitsFragment()
                1 -> AdminHallsFragment()
                else -> AdminBookingsFragment()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
