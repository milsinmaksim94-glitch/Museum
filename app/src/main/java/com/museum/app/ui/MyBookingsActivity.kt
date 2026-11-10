package com.museum.app.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.museum.app.R
import com.museum.app.adapters.BookingAdapter
import com.museum.app.data.Booking
import com.museum.app.data.DBHelper
import com.museum.app.data.MuseumRepository
import com.museum.app.utils.SessionManager
import kotlinx.coroutines.launch

// Мои записи на экскурсии. Новую бронь можно отменить нажатием
class MyBookingsActivity : AppCompatActivity() {

    private lateinit var repository: MuseumRepository
    private lateinit var adapter: BookingAdapter
    private lateinit var tvEmpty: TextView
    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = MuseumRepository(this)
        userId = SessionManager(this).getUserId()
        tvEmpty = findViewById(R.id.tvEmpty)
        tvEmpty.setText(R.string.no_bookings)

        val rv = findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = BookingAdapter(emptyList(), false) { booking -> onBookingClick(booking) }
        rv.adapter = adapter

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val list = repository.getBookings(userId)
            adapter.setItems(list)
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun onBookingClick(booking: Booking) {
        // отменить можно только новую бронь, подтверждённую уже нельзя
        if (booking.status != DBHelper.STATUS_NEW) {
            Toast.makeText(this, R.string.error_cant_cancel, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.cancel_booking_title)
            .setMessage(getString(R.string.cancel_booking_question, booking.tourTitle, booking.date))
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    repository.updateBookingStatus(booking.id, DBHelper.STATUS_CANCELLED)
                    loadData()
                    Snackbar.make(findViewById(R.id.rootLayout), R.string.booking_cancelled, Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
