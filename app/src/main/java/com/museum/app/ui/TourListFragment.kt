package com.museum.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.museum.app.R
import com.museum.app.adapters.TourAdapter
import com.museum.app.data.MuseumRepository
import kotlinx.coroutines.launch

// Список экскурсий, отсюда переходим к бронированию
class TourListFragment : Fragment() {

    private lateinit var adapter: TourAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.layout_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvEmpty = view.findViewById(R.id.tvEmpty)
        val rv = view.findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = TourAdapter(emptyList()) { tour ->
            val intent = Intent(requireContext(), BookingActivity::class.java)
            intent.putExtra(BookingActivity.EXTRA_TOUR_ID, tour.id)
            startActivity(intent)
        }
        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            val tours = MuseumRepository(requireContext()).getTours()
            adapter.setItems(tours)
            tvEmpty.visibility = if (tours.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
