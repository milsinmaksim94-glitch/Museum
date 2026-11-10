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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.museum.app.R
import com.museum.app.adapters.ExhibitAdapter
import com.museum.app.data.MuseumRepository
import kotlinx.coroutines.launch

// Админка: управление экспонатами (добавить / изменить / удалить)
class AdminExhibitsFragment : Fragment() {

    private lateinit var repository: MuseumRepository
    private lateinit var adapter: ExhibitAdapter
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
        adapter = ExhibitAdapter(
            emptyList(),
            { exhibit ->
                // нажатие - редактирование
                val intent = Intent(requireContext(), ExhibitEditActivity::class.java)
                intent.putExtra(ExhibitEditActivity.EXTRA_ID, exhibit.id)
                startActivity(intent)
            },
            { exhibit ->
                // кнопка корзины - удаление
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deleteExhibit(exhibit.id)
                    loadData()
                    Snackbar.make(
                        view,
                        getString(R.string.exhibit_deleted, exhibit.title),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        )
        rv.adapter = adapter

        // кнопка "+" для добавления
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAdd)
        fab.visibility = View.VISIBLE
        fab.setOnClickListener {
            startActivity(Intent(requireContext(), ExhibitEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = repository.getExhibits(0)
            adapter.setItems(list)
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
