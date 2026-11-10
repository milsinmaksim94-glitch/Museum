package com.museum.app.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.museum.app.R
import com.museum.app.data.Hall
import com.museum.app.data.MuseumRepository
import kotlinx.coroutines.launch

// Форма добавления и редактирования экспоната (для админа).
// Если передан id - редактируем, если нет - добавляем новый
class ExhibitEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "exhibit_id"
    }

    private lateinit var repository: MuseumRepository
    private lateinit var spinnerHall: Spinner
    private lateinit var etTitle: EditText
    private lateinit var etAuthor: EditText
    private lateinit var etYear: EditText
    private lateinit var etDescription: EditText

    private var halls: List<Hall> = emptyList()
    private var exhibitId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exhibit_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = MuseumRepository(this)
        exhibitId = intent.getLongExtra(EXTRA_ID, -1)

        spinnerHall = findViewById(R.id.spinnerHall)
        etTitle = findViewById(R.id.etTitle)
        etAuthor = findViewById(R.id.etAuthor)
        etYear = findViewById(R.id.etYear)
        etDescription = findViewById(R.id.etDescription)

        if (exhibitId == -1L) {
            setTitle(R.string.title_exhibit_add)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener { save() }

        // если экран пересоздан после поворота, поля уже заполнены - из базы не грузим
        val restoredHallPos = savedInstanceState?.getInt("hall_pos", -1) ?: -1
        loadData(savedInstanceState == null, restoredHallPos)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("hall_pos", spinnerHall.selectedItemPosition)
    }

    private fun loadData(firstStart: Boolean, restoredHallPos: Int) {
        lifecycleScope.launch {
            halls = repository.getHalls()

            if (halls.isEmpty()) {
                // без залов экспонат не добавить, т.к. hall_id обязательный (внешний ключ)
                Toast.makeText(this@ExhibitEditActivity, R.string.error_no_halls, Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            val names = ArrayList<String>()
            for (hall in halls) {
                names.add(hall.name)
            }
            val spinnerAdapter = ArrayAdapter(this@ExhibitEditActivity, android.R.layout.simple_spinner_item, names)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerHall.adapter = spinnerAdapter

            if (!firstStart) {
                // после поворота просто ставим зал, который был выбран
                if (restoredHallPos in halls.indices) spinnerHall.setSelection(restoredHallPos)
                return@launch
            }

            // режим редактирования - заполняем поля
            if (exhibitId != -1L) {
                val exhibit = repository.getExhibit(exhibitId)
                if (exhibit != null) {
                    etTitle.setText(exhibit.title)
                    etAuthor.setText(exhibit.author)
                    etYear.setText(exhibit.year)
                    etDescription.setText(exhibit.description)
                    for (i in halls.indices) {
                        if (halls[i].id == exhibit.hallId) {
                            spinnerHall.setSelection(i)
                            break
                        }
                    }
                }
            }
        }
    }

    private fun save() {
        val title = etTitle.text.toString().trim()
        val author = etAuthor.text.toString().trim()
        val year = etYear.text.toString().trim()
        val description = etDescription.text.toString().trim()

        // обязательные поля: название и описание
        var ok = true
        if (title.isEmpty()) {
            etTitle.error = getString(R.string.error_empty_field)
            ok = false
        }
        if (description.isEmpty()) {
            etDescription.error = getString(R.string.error_empty_field)
            ok = false
        }
        val pos = spinnerHall.selectedItemPosition
        if (pos < 0 || pos >= halls.size) {
            Toast.makeText(this, R.string.error_choose_hall, Toast.LENGTH_SHORT).show()
            ok = false
        }
        if (!ok) return

        val hallId = halls[pos].id

        lifecycleScope.launch {
            if (exhibitId == -1L) {
                repository.addExhibit(hallId, title, author, year, description)
                Toast.makeText(this@ExhibitEditActivity, R.string.exhibit_added, Toast.LENGTH_SHORT).show()
            } else {
                repository.updateExhibit(exhibitId, hallId, title, author, year, description)
                Toast.makeText(this@ExhibitEditActivity, R.string.exhibit_updated, Toast.LENGTH_SHORT).show()
            }
            finish() // экран закрывается, список обновится в onResume
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
