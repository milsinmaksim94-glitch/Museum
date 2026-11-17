package com.museum.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.museum.app.R
import com.museum.app.adapters.PlaceAdapter
import com.museum.app.data.MuseumRepository
import com.museum.app.data.Place
import kotlinx.coroutines.launch

// Админка: площадки музея (добавить / изменить / удалить)
class AdminPlacesFragment : Fragment() {

    private lateinit var repository: MuseumRepository
    private lateinit var adapter: PlaceAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.layout_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = MuseumRepository(requireContext())
        tvEmpty = view.findViewById(R.id.tvEmpty)

        val rv = view.findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = PlaceAdapter(
            emptyList(),
            { place ->
                val intent = Intent(requireContext(), PlaceEditActivity::class.java)
                intent.putExtra(PlaceEditActivity.EXTRA_ID, place.id)
                startActivity(intent)
            },
            { place -> askDelete(place) }
        )
        rv.adapter = adapter

        val fab = view.findViewById<FloatingActionButton>(R.id.fabAdd)
        fab.visibility = View.VISIBLE
        fab.setOnClickListener {
            startActivity(Intent(requireContext(), PlaceEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = repository.getPlaces()
            adapter.setItems(list)
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // при удалении площадки удаляются её залы, экспонаты и экскурсии (каскадно)
    private fun askDelete(place: Place) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_place_title)
            .setMessage(getString(R.string.delete_place_question, place.name))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deletePlace(place.id)
                    loadData()
                    Snackbar.make(requireView(), getString(R.string.place_deleted, place.name), Snackbar.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
