package com.museum.app.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.museum.app.R
import com.museum.app.data.Hall
import com.museum.app.data.MuseumRepository
import com.museum.app.utils.ImageHelper
import kotlinx.coroutines.Dispatchers
import com.museum.app.utils.FormUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private lateinit var etMaterial: EditText
    private lateinit var etInvNumber: EditText
    private lateinit var etDescription: EditText
    private lateinit var ivPhoto: ImageView

    private var halls: List<Hall> = emptyList()
    private var exhibitId: Long = -1
    private var image = "" // текущее фото (res:... или base64)

    // выбор картинки из галереи телефона
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                // сжатие картинки долгое, делаем в фоне
                val result = withContext(Dispatchers.IO) { ImageHelper.uriToBase64(this@ExhibitEditActivity, uri) }
                if (result == null) {
                    Toast.makeText(this@ExhibitEditActivity, R.string.error_photo, Toast.LENGTH_SHORT).show()
                } else {
                    image = result
                    ImageHelper.show(ivPhoto, image, R.drawable.ic_museum_large)
                }
            }
        }
    }

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
        etMaterial = findViewById(R.id.etMaterial)
        etInvNumber = findViewById(R.id.etInvNumber)
        etDescription = findViewById(R.id.etDescription)
        ivPhoto = findViewById(R.id.ivPhoto)

        if (exhibitId == -1L) {
            setTitle(R.string.title_exhibit_add)
        }

        findViewById<Button>(R.id.btnPhoto).setOnClickListener { pickImage.launch("image/*") }
        findViewById<Button>(R.id.btnSave).setOnClickListener { save() }

        // если экран пересоздан после поворота, поля уже заполнены - из базы не грузим
        val restoredHallPos = savedInstanceState?.getInt("hall_pos", -1) ?: -1
        if (savedInstanceState != null) {
            image = savedInstanceState.getString("image", "")
            ImageHelper.show(ivPhoto, image, R.drawable.ic_museum_large)
        }
        loadData(savedInstanceState == null, restoredHallPos)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("hall_pos", spinnerHall.selectedItemPosition)
        outState.putString("image", image)
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
                names.add(getString(R.string.format_hall_in_place, hall.name, hall.placeName))
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
                    etMaterial.setText(exhibit.material)
                    etInvNumber.setText(exhibit.invNumber)
                    etDescription.setText(exhibit.description)
                    image = exhibit.image
                    ImageHelper.show(ivPhoto, image, R.drawable.ic_museum_large)
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
        val description = etDescription.text.toString().trim()

        // обязательные поля: название и описание
        var ok = true
        if (title.isEmpty()) {
            FormUtils.showError(etTitle, getString(R.string.error_empty_field))
            ok = false
        }
        if (description.isEmpty()) {
            FormUtils.showError(etDescription, getString(R.string.error_empty_field))
            ok = false
        }
        val pos = spinnerHall.selectedItemPosition
        if (pos < 0 || pos >= halls.size) {
            Toast.makeText(this, R.string.error_choose_hall, Toast.LENGTH_SHORT).show()
            ok = false
        }
        if (!ok) return

        lifecycleScope.launch {
            repository.saveExhibit(
                exhibitId, halls[pos].id, title,
                etAuthor.text.toString().trim(),
                etYear.text.toString().trim(),
                description,
                etInvNumber.text.toString().trim(),
                etMaterial.text.toString().trim(),
                image
            )
            val msg = if (exhibitId == -1L) R.string.exhibit_added else R.string.exhibit_updated
            Toast.makeText(this@ExhibitEditActivity, msg, Toast.LENGTH_SHORT).show()
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
