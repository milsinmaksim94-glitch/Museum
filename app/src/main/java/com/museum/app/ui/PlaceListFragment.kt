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
import com.museum.app.adapters.PlaceAdapter
import com.museum.app.data.MuseumRepository
import kotlinx.coroutines.launch

// Список площадок (зданий) музея
class PlaceListFragment : Fragment() {

    private lateinit var adapter: PlaceAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.layout_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvEmpty = view.findViewById(R.id.tvEmpty)
        val rv = view.findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = PlaceAdapter(emptyList(), { place ->
            val intent = Intent(requireContext(), PlaceActivity::class.java)
            intent.putExtra(PlaceActivity.EXTRA_ID, place.id)
            startActivity(intent)
        })
        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            val places = MuseumRepository(requireContext()).getPlaces()
            adapter.setItems(places)
            tvEmpty.visibility = if (places.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
