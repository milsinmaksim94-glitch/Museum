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
import com.museum.app.adapters.ExhibitAdapter
import com.museum.app.data.MuseumRepository
import kotlinx.coroutines.launch

// Одна страница на главной - список экспонатов одного зала
class ExhibitListFragment : Fragment() {

    companion object {
        private const val ARG_HALL_ID = "hall_id"

        // так правильно передавать параметры во фрагмент (через arguments),
        // иначе при повороте экрана они потеряются
        fun newInstance(hallId: Long): ExhibitListFragment {
            val fragment = ExhibitListFragment()
            val args = Bundle()
            args.putLong(ARG_HALL_ID, hallId)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var adapter: ExhibitAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var repository: MuseumRepository
    private var hallId: Long = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.layout_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hallId = arguments?.getLong(ARG_HALL_ID) ?: 0L
        repository = MuseumRepository(requireContext())
        tvEmpty = view.findViewById(R.id.tvEmpty)

        val rv = view.findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = ExhibitAdapter(emptyList(), { exhibit ->
            // открываем подробности экспоната
            val intent = Intent(requireContext(), ExhibitDetailActivity::class.java)
            intent.putExtra(ExhibitDetailActivity.EXTRA_ID, exhibit.id)
            startActivity(intent)
        })
        rv.adapter = adapter
    }

    // грузим в onResume, чтобы после возврата с экрана деталей обновился рейтинг
    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = repository.getExhibits(hallId)
            adapter.setItems(list)
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
