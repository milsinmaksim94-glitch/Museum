package com.museum.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.CheckBox
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.museum.app.R
import com.museum.app.data.MuseumRepository
import com.museum.app.data.Tour
import com.museum.app.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.Calendar

// Запись на экскурсию: выбор даты, сеанса (времени), кол-ва человек.
// Стоимость считается сразу при любом изменении.
// У каждой экскурсии есть сеансы и максимум людей в группе - как на настоящем сайте музея
class BookingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TOUR_ID = "tour_id"

        const val AUDIO_GUIDE_PRICE = 200 // аудиогид на одного человека
    }

    private lateinit var repository: MuseumRepository
    private lateinit var session: SessionManager

    private lateinit var btnDate: Button
    private lateinit var btnTime: Button
    private lateinit var tvAdults: TextView
    private lateinit var tvChildren: TextView
    private lateinit var cbAudioGuide: CheckBox
    private lateinit var tvTotal: TextView
    private lateinit var tvFreeSeats: TextView

    private var tour: Tour? = null
    private var tourId: Long = -1

    // выбранные значения. -1 значит ещё не выбрано
    private var year = -1
    private var month = -1
    private var day = -1
    private var hour = -1
    private var minute = -1
    private var adults = 1
    private var children = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = MuseumRepository(this)
        session = SessionManager(this)
        tourId = intent.getLongExtra(EXTRA_TOUR_ID, -1)

        btnDate = findViewById(R.id.btnDate)
        btnTime = findViewById(R.id.btnTime)
        tvAdults = findViewById(R.id.tvAdults)
        tvChildren = findViewById(R.id.tvChildren)
        cbAudioGuide = findViewById(R.id.cbAudioGuide)
        tvTotal = findViewById(R.id.tvTotal)
        tvFreeSeats = findViewById(R.id.tvFreeSeats)

        // восстанавливаем то, что было выбрано до поворота экрана
        if (savedInstanceState != null) {
            year = savedInstanceState.getInt("year", -1)
            month = savedInstanceState.getInt("month", -1)
            day = savedInstanceState.getInt("day", -1)
            hour = savedInstanceState.getInt("hour", -1)
            minute = savedInstanceState.getInt("minute", -1)
            adults = savedInstanceState.getInt("adults", 1)
            children = savedInstanceState.getInt("children", 0)
        }

        btnDate.setOnClickListener { showDatePicker() }
        btnTime.setOnClickListener { showTimePicker() }

        findViewById<View>(R.id.btnAdultsPlus).setOnClickListener {
            if (canAddPerson()) adults++
            updateUi()
        }
        findViewById<View>(R.id.btnAdultsMinus).setOnClickListener {
            if (adults > 0) adults--
            updateUi()
        }
        findViewById<View>(R.id.btnChildrenPlus).setOnClickListener {
            if (canAddPerson()) children++
            updateUi()
        }
        findViewById<View>(R.id.btnChildrenMinus).setOnClickListener {
            if (children > 0) children--
            updateUi()
        }
        cbAudioGuide.setOnCheckedChangeListener { _, _ -> updateUi() }

        findViewById<Button>(R.id.btnBook).setOnClickListener { saveBooking() }

        loadTour()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("year", year)
        outState.putInt("month", month)
        outState.putInt("day", day)
        outState.putInt("hour", hour)
        outState.putInt("minute", minute)
        outState.putInt("adults", adults)
        outState.putInt("children", children)
        // галочка аудиогида сохраняется сама, у неё есть id
    }

    private fun loadTour() {
        lifecycleScope.launch {
            tour = repository.getTour(tourId)
            val t = tour
            if (t == null) {
                Toast.makeText(this@BookingActivity, R.string.error_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            findViewById<TextView>(R.id.tvTourTitle).text = t.title
            findViewById<TextView>(R.id.tvTourInfo).text = getString(
                R.string.format_tour_full_info, t.placeName, t.hallName, t.guideName, t.duration, t.price, t.maxPeople
            )
            // вместо часов работы показываем сеансы этой экскурсии
            findViewById<TextView>(R.id.tvWorkHours).text =
                getString(R.string.format_sessions, t.sessionList().joinToString(", "))
            updateUi()
        }
    }

    // в группе не больше maxPeople человек
    private fun canAddPerson(): Boolean {
        val max = tour?.maxPeople ?: 0
        if (adults + children >= max) {
            Toast.makeText(this, getString(R.string.error_max_people, max), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // считаем стоимость: взрослый полная цена, детский в 2 раза дешевле
    private fun calculateTotal(): Int {
        val t = tour ?: return 0
        var total = adults * t.price + children * (t.price / 2)
        if (cbAudioGuide.isChecked) {
            total += (adults + children) * AUDIO_GUIDE_PRICE
        }
        return total
    }

    // обновляем все надписи на экране
    private fun updateUi() {
        tvAdults.text = adults.toString()
        tvChildren.text = children.toString()
        tvTotal.text = getString(R.string.format_total, calculateTotal())

        if (day != -1) {
            btnDate.text = formatDate()
        }
        if (hour != -1) {
            btnTime.text = formatTime()
        }
        updateFreeSeats()
    }

    // показываем, сколько мест осталось на выбранный сеанс
    private fun updateFreeSeats() {
        val t = tour
        if (t == null || day == -1 || hour == -1) {
            tvFreeSeats.text = getString(R.string.choose_session_hint)
            tvFreeSeats.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            return
        }
        lifecycleScope.launch {
            val booked = repository.getBookedPeople(t.id, formatDate(), formatTime())
            val free = t.maxPeople - booked
            if (free > 0) {
                tvFreeSeats.text = getString(R.string.format_free_seats, free, t.maxPeople)
                tvFreeSeats.setTextColor(ContextCompat.getColor(this@BookingActivity, R.color.status_confirmed))
            } else {
                tvFreeSeats.text = getString(R.string.no_free_seats)
                tvFreeSeats.setTextColor(ContextCompat.getColor(this@BookingActivity, R.color.status_cancelled))
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        // если дата уже выбрана - открываем на ней
        val y = if (year != -1) year else calendar.get(Calendar.YEAR)
        val m = if (month != -1) month else calendar.get(Calendar.MONTH)
        val d = if (day != -1) day else calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(this, { _, selYear, selMonth, selDay ->
            year = selYear
            month = selMonth
            day = selDay
            updateUi()
        }, y, m, d)
        // нельзя выбрать прошедшую дату
        dialog.datePicker.minDate = calendar.timeInMillis - 1000
        dialog.show()
    }

    private fun showTimePicker() {
        val t = tour ?: return
        val sessions = t.sessionList()
        // по умолчанию открываем на первом сеансе
        var h = 10
        var min = 0
        if (hour != -1) {
            h = hour
            min = minute
        } else if (sessions.isNotEmpty()) {
            val parts = sessions[0].split(":")
            h = parts[0].toInt()
            min = parts[1].toInt()
        }

        TimePickerDialog(this, { _, selHour, selMinute ->
            val selected = String.format("%02d:%02d", selHour, selMinute)
            // экскурсия начинается только в определённое время (сеансы)
            if (!sessions.contains(selected)) {
                Toast.makeText(
                    this, getString(R.string.error_choose_session, sessions.joinToString(", ")), Toast.LENGTH_LONG
                ).show()
            } else {
                hour = selHour
                minute = selMinute
                updateUi()
            }
        }, h, min, true).show()
    }

    private fun formatDate(): String {
        // месяцы в Calendar идут с 0, поэтому +1
        return String.format("%02d.%02d.%04d", day, month + 1, year)
    }

    private fun formatTime(): String {
        return String.format("%02d:%02d", hour, minute)
    }

    // проверяем, что выбранное время ещё не прошло (если бронь на сегодня)
    private fun isInPast(): Boolean {
        val selected = Calendar.getInstance()
        selected.set(year, month, day, hour, minute, 0)
        return selected.before(Calendar.getInstance())
    }

    private fun saveBooking() {
        val t = tour ?: return

        if (day == -1) {
            Toast.makeText(this, R.string.error_choose_date, Toast.LENGTH_SHORT).show()
            return
        }
        if (hour == -1) {
            Toast.makeText(this, R.string.error_choose_time, Toast.LENGTH_SHORT).show()
            return
        }
        if (adults + children == 0) {
            Toast.makeText(this, R.string.error_no_people, Toast.LENGTH_SHORT).show()
            return
        }
        if (children > 0 && adults == 0) {
            // дети без взрослых не допускаются
            Toast.makeText(this, R.string.error_children_alone, Toast.LENGTH_SHORT).show()
            return
        }
        if (isInPast()) {
            Toast.makeText(this, R.string.error_time_in_past, Toast.LENGTH_SHORT).show()
            return
        }

        val btnBook = findViewById<Button>(R.id.btnBook)
        btnBook.isEnabled = false
        lifecycleScope.launch {
            // перед записью берём свежие брони из облака - вдруг кто-то записался с другого телефона
            repository.syncFromCloud()
            val booked = repository.getBookedPeople(t.id, formatDate(), formatTime())
            val free = t.maxPeople - booked
            if (adults + children > free) {
                btnBook.isEnabled = true
                Toast.makeText(
                    this@BookingActivity, getString(R.string.error_not_enough_seats, free), Toast.LENGTH_LONG
                ).show()
                updateFreeSeats()
                return@launch
            }

            repository.addBooking(
                session.getUserId(), t.id, formatDate(), formatTime(),
                adults, children, cbAudioGuide.isChecked, calculateTotal()
            )
            Toast.makeText(this@BookingActivity, R.string.booking_success, Toast.LENGTH_LONG).show()
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
