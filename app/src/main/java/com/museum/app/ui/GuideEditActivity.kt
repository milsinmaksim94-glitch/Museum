package com.museum.app.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.museum.app.R
import com.museum.app.data.MuseumRepository
import com.museum.app.utils.ImageHelper
import kotlinx.coroutines.Dispatchers
import com.museum.app.utils.FormUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Добавление и редактирование экскурсовода (для админа)
class GuideEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "guide_id"
    }

    private lateinit var repository: MuseumRepository
    private lateinit var etName: EditText
    private lateinit var etPosition: EditText
    private lateinit var etExperience: EditText
    private lateinit var etLanguages: EditText
    private lateinit var etBio: EditText
    private lateinit var ivPhoto: ImageView

    private var guideId: Long = -1
    private var image = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) { ImageHelper.uriToBase64(this@GuideEditActivity, uri) }
                if (result == null) {
                    Toast.makeText(this@GuideEditActivity, R.string.error_photo, Toast.LENGTH_SHORT).show()
                } else {
                    image = result
                    ImageHelper.show(ivPhoto, image, R.drawable.ic_person)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = MuseumRepository(this)
        guideId = intent.getLongExtra(EXTRA_ID, -1)
        setTitle(if (guideId == -1L) R.string.title_guide_add else R.string.title_guide_edit)

        etName = findViewById(R.id.etName)
        etPosition = findViewById(R.id.etPosition)
        etExperience = findViewById(R.id.etExperience)
        etLanguages = findViewById(R.id.etLanguages)
        etBio = findViewById(R.id.etBio)
        ivPhoto = findViewById(R.id.ivPhoto)

        findViewById<Button>(R.id.btnPhoto).setOnClickListener { pickImage.launch("image/*") }
        findViewById<Button>(R.id.btnSave).setOnClickListener { save() }

        if (savedInstanceState != null) {
            // после поворота текст в полях восстановится сам, а фото берём из bundle
            image = savedInstanceState.getString("image", "")
            ImageHelper.show(ivPhoto, image, R.drawable.ic_person)
        } else if (guideId != -1L) {
            lifecycleScope.launch {
                val guide = repository.getGuide(guideId) ?: return@launch
                etName.setText(guide.name)
                etPosition.setText(guide.position)
                etExperience.setText(guide.experience.toString())
                etLanguages.setText(guide.languages)
                etBio.setText(guide.bio)
                image = guide.image
                ImageHelper.show(ivPhoto, image, R.drawable.ic_person)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("image", image)
    }

    private fun save() {
        val name = etName.text.toString().trim()
        val experience = etExperience.text.toString().trim().toIntOrNull()

        var ok = true
        if (name.isEmpty()) {
            FormUtils.showError(etName, getString(R.string.error_empty_field))
            ok = false
        }
        if (experience == null || experience < 0) {
            FormUtils.showError(etExperience, getString(R.string.error_wrong_number))
            ok = false
        }
        if (!ok) return

        lifecycleScope.launch {
            repository.saveGuide(
                guideId, name, etPosition.text.toString().trim(), experience!!,
                etLanguages.text.toString().trim(), etBio.text.toString().trim(), image
            )
            Toast.makeText(this@GuideEditActivity, R.string.saved, Toast.LENGTH_SHORT).show()
            finish()
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
