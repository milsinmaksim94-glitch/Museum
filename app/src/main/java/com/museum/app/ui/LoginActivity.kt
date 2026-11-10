package com.museum.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.museum.app.R
import com.museum.app.data.MuseumRepository
import com.museum.app.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etLogin: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    private lateinit var repository: MuseumRepository
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)

        // если уже входили раньше - сразу на главный экран
        if (session.isLoggedIn()) {
            openMain()
            return
        }

        setContentView(R.layout.activity_login)
        repository = MuseumRepository(this)

        etLogin = findViewById(R.id.etLogin)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)

        btnLogin.setOnClickListener { tryLogin() }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun tryLogin() {
        val login = etLogin.text.toString().trim()
        val password = etPassword.text.toString()

        // проверяем что поля не пустые
        var ok = true
        if (login.isEmpty()) {
            etLogin.error = getString(R.string.error_empty_field)
            ok = false
        }
        if (password.isEmpty()) {
            etPassword.error = getString(R.string.error_empty_field)
            ok = false
        }
        if (!ok) return

        btnLogin.isEnabled = false // чтобы не нажимали 10 раз

        lifecycleScope.launch {
            val user = repository.login(login, password)
            btnLogin.isEnabled = true
            if (user == null) {
                Toast.makeText(this@LoginActivity, R.string.error_wrong_login, Toast.LENGTH_SHORT).show()
            } else {
                session.saveUser(user.id, user.role)
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.welcome_user, user.name),
                    Toast.LENGTH_SHORT
                ).show()
                openMain()
            }
        }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
