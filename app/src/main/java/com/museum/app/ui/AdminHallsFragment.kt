package com.museum.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.museum.app.R
import com.museum.app.adapters.HallAdapter
import com.museum.app.data.Hall
import com.museum.app.data.MuseumRepository
import kotlinx.coroutines.launch

// Админка: справочник залов
class AdminHallsFragment : Fragment() {

    private lateinit var repository: MuseumRepository
    private lateinit var adapter: HallAdapter
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
        adapter = HallAdapter(emptyList()) { hall -> askDelete(hall) }
        rv.adapter = adapter

        val fab = view.findViewById<FloatingActionButton>(R.id.fabAdd)
        fab.visibility = View.VISIBLE
        fab.setOnClickListener { showAddDialog() }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = repository.getHalls()
            adapter.setItems(list)
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // диалог с полями для нового зала
    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_hall, null)
        val etName = dialogView.findViewById<EditText>(R.id.etHallName)
        val etFloor = dialogView.findViewById<EditText>(R.id.etHallFloor)
        val etDescription = dialogView.findViewById<EditText>(R.id.etHallDescription)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_hall_title)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_save, null) // обработчик ставим ниже, чтобы диалог не закрывался при ошибке
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = etName.text.toString().trim()
                val floorText = etFloor.text.toString().trim()

                if (name.isEmpty()) {
                    etName.error = getString(R.string.error_empty_field)
                    return@setOnClickListener
                }
                // toIntOrNull вернёт null если там не число
                val floor = floorText.toIntOrNull()
                if (floor == null || floor < 1) {
                    etFloor.error = getString(R.string.error_wrong_floor)
                    return@setOnClickListener
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    repository.addHall(name, floor, etDescription.text.toString().trim())
                    loadData()
                    Toast.makeText(requireContext(), R.string.hall_added, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun askDelete(hall: Hall) {
        // предупреждаем, что удалятся и экспонаты зала (каскадное удаление в БД)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_hall_title)
            .setMessage(getString(R.string.delete_hall_question, hall.name))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deleteHall(hall.id)
                    loadData()
                    Snackbar.make(requireView(), getString(R.string.hall_deleted, hall.name), Snackbar.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
