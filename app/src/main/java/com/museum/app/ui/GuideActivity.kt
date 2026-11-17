package com.museum.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.museum.app.R
import com.museum.app.adapters.TourAdapter
import com.museum.app.data.MuseumRepository
import com.museum.app.utils.ImageHelper
import kotlinx.coroutines.launch

// Карточка экскурсовода и список его экскурсий
class GuideActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "guide_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val guideId = intent.getLongExtra(EXTRA_ID, -1)
        val repository = MuseumRepository(this)

        val rvTours = findViewById<RecyclerView>(R.id.rvTours)
        rvTours.layoutManager = LinearLayoutManager(this)
        val adapter = TourAdapter(emptyList()) { tour ->
            val intent = Intent(this, BookingActivity::class.java)
            intent.putExtra(BookingActivity.EXTRA_TOUR_ID, tour.id)
            startActivity(intent)
        }
        rvTours.adapter = adapter

        lifecycleScope.launch {
            val guide = repository.getGuide(guideId)
            if (guide == null) {
                Toast.makeText(this@GuideActivity, R.string.error_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            title = guide.name

            val ivPhoto = findViewById<ImageView>(R.id.ivPhoto)
            val tvInitials = findViewById<TextView>(R.id.tvInitials)
            if (guide.image.isEmpty()) {
                ivPhoto.visibility = View.INVISIBLE
                tvInitials.visibility = View.VISIBLE
                tvInitials.text = ImageHelper.initials(guide.name)
            } else {
                tvInitials.visibility = View.GONE
                ImageHelper.show(ivPhoto, guide.image, R.drawable.ic_person)
            }

            findViewById<TextView>(R.id.tvName).text = guide.name
            findViewById<TextView>(R.id.tvPosition).text = guide.position
            findViewById<TextView>(R.id.tvExperience).text =
                getString(R.string.format_experience, guide.experience, guide.languages)
            findViewById<TextView>(R.id.tvBio).text = guide.bio

            val tours = repository.getToursByGuide(guideId)
            adapter.setItems(tours)
            findViewById<TextView>(R.id.tvNoTours).visibility = if (tours.isEmpty()) View.VISIBLE else View.GONE
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
