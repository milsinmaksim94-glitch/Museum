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
import com.museum.app.adapters.GuideAdapter
import com.museum.app.data.Guide
import com.museum.app.data.MuseumRepository
import kotlinx.coroutines.launch

// Админка: экскурсоводы (добавить / изменить / удалить)
class AdminGuidesFragment : Fragment() {

    private lateinit var repository: MuseumRepository
    private lateinit var adapter: GuideAdapter
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
        adapter = GuideAdapter(
            emptyList(),
            { guide ->
                val intent = Intent(requireContext(), GuideEditActivity::class.java)
                intent.putExtra(GuideEditActivity.EXTRA_ID, guide.id)
                startActivity(intent)
            },
            { guide -> askDelete(guide) }
        )
        rv.adapter = adapter

        val fab = view.findViewById<FloatingActionButton>(R.id.fabAdd)
        fab.visibility = View.VISIBLE
        fab.setOnClickListener {
            startActivity(Intent(requireContext(), GuideEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = repository.getGuides()
            adapter.setItems(list)
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // у экскурсовода могут быть экскурсии, они удалятся вместе с ним - предупреждаем
    private fun askDelete(guide: Guide) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_guide_title)
            .setMessage(getString(R.string.delete_guide_question, guide.name))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deleteGuide(guide.id)
                    loadData()
                    Snackbar.make(requireView(), getString(R.string.guide_deleted, guide.name), Snackbar.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
