package com.museum.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.museum.app.R
import com.museum.app.adapters.ExhibitAdapter
import com.museum.app.data.MuseumRepository
import com.museum.app.utils.SessionManager
import kotlinx.coroutines.launch

// Избранные экспонаты пользователя
class FavoritesActivity : AppCompatActivity() {

    private lateinit var repository: MuseumRepository
    private lateinit var adapter: ExhibitAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = MuseumRepository(this)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvEmpty.setText(R.string.no_favorites)

        val rv = findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ExhibitAdapter(emptyList(), { exhibit ->
            val intent = Intent(this, ExhibitDetailActivity::class.java)
            intent.putExtra(ExhibitDetailActivity.EXTRA_ID, exhibit.id)
            startActivity(intent)
        })
        rv.adapter = adapter
    }

    // в onResume, потому что на экране деталей могли убрать из избранного
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val list = repository.getFavorites(SessionManager(this@FavoritesActivity).getUserId())
            adapter.setItems(list)
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
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
