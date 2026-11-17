package com.museum.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
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
import com.museum.app.data.Place
import com.museum.app.utils.ImageHelper
import kotlinx.coroutines.launch

// Подробно про площадку: адрес, как добраться, залы и экскурсии в ней
class PlaceActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "place_id"
    }

    private var place: Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val placeId = intent.getLongExtra(EXTRA_ID, -1)
        val repository = MuseumRepository(this)

        val rvTours = findViewById<RecyclerView>(R.id.rvTours)
        rvTours.layoutManager = LinearLayoutManager(this)
        val adapter = TourAdapter(emptyList()) { tour ->
            val intent = Intent(this, BookingActivity::class.java)
            intent.putExtra(BookingActivity.EXTRA_TOUR_ID, tour.id)
            startActivity(intent)
        }
        rvTours.adapter = adapter

        findViewById<Button>(R.id.btnMap).setOnClickListener { openMap() }
        findViewById<Button>(R.id.btnCall).setOnClickListener { callPhone() }

        lifecycleScope.launch {
            val p = repository.getPlace(placeId)
            if (p == null) {
                Toast.makeText(this@PlaceActivity, R.string.error_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            place = p
            title = p.name
            ImageHelper.show(findViewById<ImageView>(R.id.ivPhoto), p.cover, R.drawable.place_placeholder)
            findViewById<TextView>(R.id.tvName).text = p.name
            findViewById<TextView>(R.id.tvAddress).text = p.address
            findViewById<TextView>(R.id.tvMetro).text = getString(R.string.format_metro, p.metro)
            findViewById<TextView>(R.id.tvHours).text = p.hours
            findViewById<TextView>(R.id.tvPhone).text = getString(R.string.format_phone, p.phone)
            findViewById<TextView>(R.id.tvDescription).text = p.description

            // залы списком в одну строку на каждый
            val halls = repository.getHalls(placeId)
            val sb = StringBuilder()
            for (hall in halls) {
                sb.append(getString(R.string.format_hall_line, hall.name, hall.floor)).append("\n")
            }
            findViewById<TextView>(R.id.tvHalls).text =
                if (halls.isEmpty()) getString(R.string.no_halls) else sb.toString().trim()

            val tours = repository.getToursByPlace(placeId)
            adapter.setItems(tours)
            findViewById<TextView>(R.id.tvNoTours).visibility = if (tours.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // открываем адрес в приложении карт (Яндекс, Google - что установлено)
    private fun openMap() {
        val p = place ?: return
        val uri = Uri.parse("geo:${p.latitude},${p.longitude}?q=" + Uri.encode(p.address))
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_no_map_app, Toast.LENGTH_SHORT).show()
        }
    }

    // открываем звонилку с номером (сам звонок пользователь нажимает сам)
    private fun callPhone() {
        val p = place ?: return
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + p.phone)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_no_dial_app, Toast.LENGTH_SHORT).show()
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
