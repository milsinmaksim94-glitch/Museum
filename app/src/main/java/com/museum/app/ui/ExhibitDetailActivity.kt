package com.museum.app.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.museum.app.R
import com.museum.app.adapters.CommentAdapter
import com.museum.app.data.Comment
import com.museum.app.data.MuseumRepository
import com.museum.app.utils.SessionManager
import kotlinx.coroutines.launch

// Экран подробностей экспоната: описание, избранное, оценка и отзывы
class ExhibitDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "exhibit_id"
    }

    private lateinit var repository: MuseumRepository
    private lateinit var session: SessionManager
    private lateinit var commentAdapter: CommentAdapter

    private lateinit var tvTitle: TextView
    private lateinit var tvAuthor: TextView
    private lateinit var tvYear: TextView
    private lateinit var tvHall: TextView
    private lateinit var tvDescription: TextView
    private lateinit var ratingAverage: RatingBar
    private lateinit var tvRatingValue: TextView
    private lateinit var btnFavorite: ImageButton
    private lateinit var seekRating: SeekBar
    private lateinit var tvSeekValue: TextView
    private lateinit var etComment: EditText
    private lateinit var tvNoComments: TextView

    private var exhibitId: Long = -1
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exhibit_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = MuseumRepository(this)
        session = SessionManager(this)
        exhibitId = intent.getLongExtra(EXTRA_ID, -1)

        tvTitle = findViewById(R.id.tvTitle)
        tvAuthor = findViewById(R.id.tvAuthor)
        tvYear = findViewById(R.id.tvYear)
        tvHall = findViewById(R.id.tvHall)
        tvDescription = findViewById(R.id.tvDescription)
        ratingAverage = findViewById(R.id.ratingAverage)
        tvRatingValue = findViewById(R.id.tvRatingValue)
        btnFavorite = findViewById(R.id.btnFavorite)
        seekRating = findViewById(R.id.seekRating)
        tvSeekValue = findViewById(R.id.tvSeekValue)
        etComment = findViewById(R.id.etComment)
        tvNoComments = findViewById(R.id.tvNoComments)

        val rvComments = findViewById<RecyclerView>(R.id.rvComments)
        rvComments.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter(emptyList()) { comment -> askDeleteComment(comment) }
        rvComments.adapter = commentAdapter

        // SeekBar идёт от 0 до 4, а оценка от 1 до 5, поэтому +1
        tvSeekValue.text = (seekRating.progress + 1).toString()
        seekRating.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvSeekValue.text = (progress + 1).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnFavorite.setOnClickListener { toggleFavorite() }
        findViewById<Button>(R.id.btnSend).setOnClickListener { sendComment() }

        loadExhibit()
        loadComments()
    }

    // после поворота SeekBar сам восстанавливает значение, а текст рядом - нет, обновим его
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        tvSeekValue.text = (seekRating.progress + 1).toString()
    }

    private fun loadExhibit() {
        lifecycleScope.launch {
            val exhibit = repository.getExhibit(exhibitId)
            if (exhibit == null) {
                // экспонат могли удалить, пока экран был открыт
                Toast.makeText(this@ExhibitDetailActivity, R.string.error_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            title = exhibit.title
            tvTitle.text = exhibit.title
            tvAuthor.text = getString(R.string.format_author, exhibit.author)
            tvYear.text = getString(R.string.format_year, exhibit.year)
            tvHall.text = getString(R.string.format_hall, exhibit.hallName)
            tvDescription.text = exhibit.description

            ratingAverage.rating = exhibit.rating
            if (exhibit.rating > 0) {
                tvRatingValue.text = getString(R.string.format_rating_value, exhibit.rating)
            } else {
                tvRatingValue.text = getString(R.string.no_rating)
            }

            isFavorite = repository.isFavorite(session.getUserId(), exhibitId)
            updateFavoriteIcon()
        }
    }

    private fun loadComments() {
        lifecycleScope.launch {
            val comments = repository.getComments(exhibitId)
            commentAdapter.setItems(comments)
            tvNoComments.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun updateFavoriteIcon() {
        // красное сердце если в избранном, серое если нет
        val color = if (isFavorite) R.color.status_cancelled else R.color.text_secondary
        btnFavorite.setColorFilter(ContextCompat.getColor(this, color))
    }

    private fun toggleFavorite() {
        lifecycleScope.launch {
            if (isFavorite) {
                repository.removeFavorite(session.getUserId(), exhibitId)
                isFavorite = false
                Toast.makeText(this@ExhibitDetailActivity, R.string.removed_from_favorites, Toast.LENGTH_SHORT).show()
            } else {
                repository.addFavorite(session.getUserId(), exhibitId)
                isFavorite = true
                Toast.makeText(this@ExhibitDetailActivity, R.string.added_to_favorites, Toast.LENGTH_SHORT).show()
            }
            updateFavoriteIcon()
        }
    }

    private fun sendComment() {
        val text = etComment.text.toString().trim()
        if (text.isEmpty()) {
            etComment.error = getString(R.string.error_empty_comment)
            return
        }
        val rating = seekRating.progress + 1

        lifecycleScope.launch {
            repository.addComment(exhibitId, session.getUserId(), text, rating)
            etComment.setText("")
            Toast.makeText(this@ExhibitDetailActivity, R.string.comment_added, Toast.LENGTH_SHORT).show()
            loadComments()
            loadExhibit() // чтобы обновилась средняя оценка
        }
    }

    // удалить можно только свой отзыв, админ может удалить любой
    private fun askDeleteComment(comment: Comment) {
        if (comment.userId != session.getUserId() && !session.isAdmin()) {
            Toast.makeText(this, R.string.error_not_your_comment, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setMessage(R.string.delete_comment_question)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                lifecycleScope.launch {
                    repository.deleteComment(comment.id)
                    loadComments()
                    loadExhibit()
                    Snackbar.make(findViewById(R.id.rootLayout), R.string.comment_deleted, Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // стрелка назад в верхней панели
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
