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

// Добавление и редактирование площадки (для админа)
class PlaceEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "place_id"
    }

    private lateinit var repository: MuseumRepository
    private lateinit var etName: EditText
    private lateinit var etAddress: EditText
    private lateinit var etMetro: EditText
    private lateinit var etHours: EditText
    private lateinit var etPhone: EditText
    private lateinit var etLat: EditText
    private lateinit var etLon: EditText
    private lateinit var etDescription: EditText
    private lateinit var ivPhoto: ImageView

    private var placeId: Long = -1
    private var image = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) { ImageHelper.uriToBase64(this@PlaceEditActivity, uri) }
                if (result == null) {
                    Toast.makeText(this@PlaceEditActivity, R.string.error_photo, Toast.LENGTH_SHORT).show()
                } else {
                    image = result
                    ImageHelper.show(ivPhoto, image, R.drawable.place_placeholder)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = MuseumRepository(this)
        placeId = intent.getLongExtra(EXTRA_ID, -1)
        setTitle(if (placeId == -1L) R.string.title_place_add else R.string.title_place_edit)

        etName = findViewById(R.id.etName)
        etAddress = findViewById(R.id.etAddress)
        etMetro = findViewById(R.id.etMetro)
        etHours = findViewById(R.id.etHours)
        etPhone = findViewById(R.id.etPhone)
        etLat = findViewById(R.id.etLat)
        etLon = findViewById(R.id.etLon)
        etDescription = findViewById(R.id.etDescription)
        ivPhoto = findViewById(R.id.ivPhoto)

        findViewById<Button>(R.id.btnPhoto).setOnClickListener { pickImage.launch("image/*") }
        findViewById<Button>(R.id.btnSave).setOnClickListener { save() }

        if (savedInstanceState != null) {
            image = savedInstanceState.getString("image", "")
            ImageHelper.show(ivPhoto, image, R.drawable.place_placeholder)
        } else if (placeId != -1L) {
            lifecycleScope.launch {
                val place = repository.getPlace(placeId) ?: return@launch
                etName.setText(place.name)
                etAddress.setText(place.address)
                etMetro.setText(place.metro)
                etHours.setText(place.hours)
                etPhone.setText(place.phone)
                etLat.setText(place.latitude.toString())
                etLon.setText(place.longitude.toString())
                etDescription.setText(place.description)
                image = place.image
                ImageHelper.show(ivPhoto, image, R.drawable.place_placeholder)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("image", image)
    }

    private fun save() {
        val name = etName.text.toString().trim()
        val address = etAddress.text.toString().trim()
        // координаты необязательные, если пусто - будет 0
        val latText = etLat.text.toString().trim()
        val lonText = etLon.text.toString().trim()
        val lat = if (latText.isEmpty()) 0.0 else latText.toDoubleOrNull()
        val lon = if (lonText.isEmpty()) 0.0 else lonText.toDoubleOrNull()

        var ok = true
        if (name.isEmpty()) {
            FormUtils.showError(etName, getString(R.string.error_empty_field))
            ok = false
        }
        if (address.isEmpty()) {
            FormUtils.showError(etAddress, getString(R.string.error_empty_field))
            ok = false
        }
        if (lat == null || lat < -90 || lat > 90) {
            FormUtils.showError(etLat, getString(R.string.error_wrong_number))
            ok = false
        }
        if (lon == null || lon < -180 || lon > 180) {
            FormUtils.showError(etLon, getString(R.string.error_wrong_number))
            ok = false
        }
        if (!ok) return

        lifecycleScope.launch {
            repository.savePlace(
                placeId, name, address,
                etMetro.text.toString().trim(),
                etHours.text.toString().trim(),
                etPhone.text.toString().trim(),
                etDescription.text.toString().trim(),
                lat!!, lon!!, image
            )
            Toast.makeText(this@PlaceEditActivity, R.string.saved, Toast.LENGTH_SHORT).show()
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
