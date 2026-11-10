package com.museum.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.museum.app.R
import com.museum.app.adapters.BookingAdapter
import com.museum.app.data.Booking
import com.museum.app.data.MuseumRepository
import com.museum.app.utils.StatusHelper
import kotlinx.coroutines.launch

// Админка: все брони, тут меняем статус
class AdminBookingsFragment : Fragment() {

    private lateinit var repository: MuseumRepository
    private lateinit var adapter: BookingAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.layout_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = MuseumRepository(requireContext())
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvEmpty.setText(R.string.no_bookings)

        val rv = view.findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = BookingAdapter(emptyList(), true) { booking -> showStatusDialog(booking) }
        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = repository.getBookings(0) // 0 = все пользователи
            adapter.setItems(list)
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // диалог со списком статусов, текущий отмечен
    private fun showStatusDialog(booking: Booking) {
        val statuses = StatusHelper.allStatuses
        val names = Array(statuses.size) { i -> StatusHelper.getStatusText(requireContext(), statuses[i]) }
        val current = statuses.indexOf(booking.status)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.change_status_title)
            .setSingleChoiceItems(names, current) { dialog, which ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.updateBookingStatus(booking.id, statuses[which])
                    loadData()
                    Toast.makeText(requireContext(), R.string.status_changed, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
