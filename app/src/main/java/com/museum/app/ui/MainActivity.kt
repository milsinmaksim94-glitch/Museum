package com.museum.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.museum.app.R
import com.museum.app.data.MuseumRepository
import com.museum.app.utils.SessionManager
import kotlinx.coroutines.launch

// Главный экран с нижним меню. Переключает фрагменты
// и при запуске подтягивает свежие данные из облака
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // на всякий случай: если сессии нет, отправляем на вход
        if (!SessionManager(this).isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            openTab(item.itemId)
            true
        }

        // при повороте экрана фрагмент восстановится сам, поэтому
        // открываем главную только при первом запуске
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
            syncData(false)
        } else {
            title = savedInstanceState.getString("title", getString(R.string.app_name))
        }
    }

    private fun openTab(itemId: Int) {
        when (itemId) {
            R.id.nav_home -> openFragment(HomeFragment(), R.string.nav_home)
            R.id.nav_search -> openFragment(SearchFragment(), R.string.nav_search)
            R.id.nav_tours -> openFragment(ToursFragment(), R.string.nav_tours)
            R.id.nav_profile -> openFragment(ProfileFragment(), R.string.nav_profile)
        }
    }

    private fun openFragment(fragment: Fragment, titleRes: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        setTitle(titleRes)
    }

    // скачиваем общие данные из Firebase. showMessage - показывать ли тост с результатом
    private fun syncData(showMessage: Boolean) {
        lifecycleScope.launch {
            val repository = MuseumRepository(this@MainActivity)
            val ok = repository.syncFromCloud()

            // если пользователя больше нет в базе (например, после обновления приложения
            // база пересоздалась) - просим войти заново
            val session = SessionManager(this@MainActivity)
            if (repository.getUser(session.getUserId()) == null) {
                session.logout()
                Toast.makeText(this@MainActivity, R.string.error_login_again, Toast.LENGTH_LONG).show()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            if (ok) {
                // пересоздаём текущую вкладку, чтобы списки показали новые данные
                // (если экран уже свёрнут - не трогаем, иначе будет ошибка)
                if (!supportFragmentManager.isStateSaved) {
                    openTab(bottomNav.selectedItemId)
                }
                if (showMessage) {
                    Toast.makeText(this@MainActivity, R.string.sync_ok, Toast.LENGTH_SHORT).show()
                }
            } else if (showMessage) {
                Toast.makeText(this@MainActivity, R.string.sync_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> {
                Toast.makeText(this, R.string.sync_started, Toast.LENGTH_SHORT).show()
                syncData(true)
                return true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("title", title.toString())
    }
}
