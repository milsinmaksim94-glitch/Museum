package com.museum.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.museum.app.R
import com.museum.app.adapters.ExhibitAdapter
import com.museum.app.data.Hall
import com.museum.app.data.MuseumRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Поиск экспонатов в реальном времени + фильтр по залу
class SearchFragment : Fragment() {

    private lateinit var repository: MuseumRepository
    private lateinit var adapter: ExhibitAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var spinner: Spinner

    private var halls: List<Hall> = emptyList()
    private var query = ""
    private var searchJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = MuseumRepository(requireContext())
        tvEmpty = view.findViewById(R.id.tvEmpty)
        spinner = view.findViewById(R.id.spinnerHall)
        val searchView = view.findViewById<SearchView>(R.id.searchView)

        val rv = view.findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = ExhibitAdapter(emptyList(), { exhibit ->
            val intent = Intent(requireContext(), ExhibitDetailActivity::class.java)
            intent.putExtra(ExhibitDetailActivity.EXTRA_ID, exhibit.id)
            startActivity(intent)
        })
        rv.adapter = adapter

        // ищем при каждом изменении текста, кнопку "найти" жать не надо
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(text: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(text: String?): Boolean {
                query = text ?: ""
                search()
                return true
            }
        })

        loadHalls(savedInstanceState?.getInt("hall_pos", 0) ?: 0)
    }

    // заполняем выпадающий список залами
    private fun loadHalls(selectedPos: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            halls = repository.getHalls()

            val names = ArrayList<String>()
            names.add(getString(R.string.all_halls))
            for (hall in halls) {
                names.add(hall.name)
            }

            val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = spinnerAdapter
            if (selectedPos < names.size) {
                spinner.setSelection(selectedPos)
            }

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    search()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            search()
        }
    }

    private fun search() {
        // 0 - "все залы", остальные позиции сдвинуты на 1
        val pos = spinner.selectedItemPosition
        val hallId = if (pos <= 0 || pos > halls.size) 0L else halls[pos - 1].id

        // если пользователь быстро печатает, отменяем прошлый поиск
        searchJob?.cancel()
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.searchExhibits(query.trim(), hallId)
            adapter.setItems(result)
            tvEmpty.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // обновляем результаты, вдруг что-то поменялось
        if (halls.isNotEmpty()) search()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // запоминаем выбранный зал, текст в SearchView сохраняется сам
        if (::spinner.isInitialized) {
            outState.putInt("hall_pos", spinner.selectedItemPosition)
        }
    }
}
