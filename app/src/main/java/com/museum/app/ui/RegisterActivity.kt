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
import com.museum.app.data.DBHelper
import com.museum.app.data.MuseumRepository
import com.museum.app.utils.SessionManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etLogin: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPassword2: EditText
    private lateinit var btnRegister: Button

    private lateinit var repository: MuseumRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        repository = MuseumRepository(this)

        etName = findViewById(R.id.etName)
        etLogin = findViewById(R.id.etLogin)
        etPassword = findViewById(R.id.etPassword)
        etPassword2 = findViewById(R.id.etPassword2)
        btnRegister = findViewById(R.id.btnRegister)

        btnRegister.setOnClickListener { tryRegister() }

        // вернуться на экран входа
        findViewById<TextView>(R.id.tvGoLogin).setOnClickListener { finish() }
    }

    // проверка всех полей, если что-то не так - пишем ошибку прямо на поле
    private fun validate(): Boolean {
        val name = etName.text.toString().trim()
        val login = etLogin.text.toString().trim()
        val password = etPassword.text.toString()
        val password2 = etPassword2.text.toString()
        var ok = true

        if (name.isEmpty()) {
            etName.error = getString(R.string.error_empty_field)
            ok = false
        }

        if (login.isEmpty()) {
            etLogin.error = getString(R.string.error_empty_field)
            ok = false
        } else if (login.length < 3) {
            etLogin.error = getString(R.string.error_login_short)
            ok = false
        } else if (login.contains(" ")) {
            etLogin.error = getString(R.string.error_login_spaces)
            ok = false
        }

        if (password.isEmpty()) {
            etPassword.error = getString(R.string.error_empty_field)
            ok = false
        } else if (password.length < 6) {
            etPassword.error = getString(R.string.error_password_short)
            ok = false
        }

        if (password2 != password) {
            etPassword2.error = getString(R.string.error_password_mismatch)
            ok = false
        }
        return ok
    }

    private fun tryRegister() {
        if (!validate()) return

        val name = etName.text.toString().trim()
        val login = etLogin.text.toString().trim()
        val password = etPassword.text.toString()

        btnRegister.isEnabled = false
        lifecycleScope.launch {
            val newId = repository.register(login, password, name)
            btnRegister.isEnabled = true

            if (newId == -1L) {
                etLogin.error = getString(R.string.error_login_taken)
            } else {
                Toast.makeText(this@RegisterActivity, R.string.register_success, Toast.LENGTH_SHORT).show()
                // сразу логиним нового пользователя
                SessionManager(this@RegisterActivity).saveUser(newId, DBHelper.ROLE_USER)

                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                // очищаем стек, чтобы кнопкой назад не вернуться на регистрацию
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }
}
