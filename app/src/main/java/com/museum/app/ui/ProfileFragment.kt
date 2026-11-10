package com.museum.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import com.museum.app.R
import com.museum.app.data.MuseumRepository
import com.museum.app.utils.SessionManager
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var session: SessionManager
    private lateinit var repository: MuseumRepository

    private lateinit var tvLogin: TextView
    private lateinit var tvRole: TextView
    private lateinit var etName: EditText
    private lateinit var etNewPassword: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        repository = MuseumRepository(requireContext())

        tvLogin = view.findViewById(R.id.tvLogin)
        tvRole = view.findViewById(R.id.tvRole)
        etName = view.findViewById(R.id.etName)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        val btnAdmin = view.findViewById<Button>(R.id.btnAdmin)
        val switchTheme = view.findViewById<SwitchMaterial>(R.id.switchTheme)

        // админку показываем только админу
        btnAdmin.visibility = if (session.isAdmin()) View.VISIBLE else View.GONE

        // если экран пересоздан (поворот), то в поле уже может быть текст - не затираем его
        loadUser(savedInstanceState == null)

        view.findViewById<Button>(R.id.btnSaveName).setOnClickListener { saveName() }
        view.findViewById<Button>(R.id.btnSavePassword).setOnClickListener { savePassword() }

        view.findViewById<Button>(R.id.btnFavorites).setOnClickListener {
            startActivity(Intent(requireContext(), FavoritesActivity::class.java))
        }
        view.findViewById<Button>(R.id.btnMyBookings).setOnClickListener {
            startActivity(Intent(requireContext(), MyBookingsActivity::class.java))
        }
        btnAdmin.setOnClickListener {
            startActivity(Intent(requireContext(), AdminActivity::class.java))
        }

        // переключатель темы
        switchTheme.isChecked = session.isDarkTheme()
        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            session.setDarkTheme(isChecked)
        }

        view.findViewById<Button>(R.id.btnLogout).setOnClickListener { askLogout() }
    }

    private fun loadUser(fillName: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = repository.getUser(session.getUserId()) ?: return@launch
            tvLogin.text = user.login
            tvRole.text = if (user.isAdmin()) getString(R.string.role_admin) else getString(R.string.role_user)
            if (fillName) {
                etName.setText(user.name)
            }
        }
    }

    private fun saveName() {
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            etName.error = getString(R.string.error_empty_field)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repository.updateUserName(session.getUserId(), name)
            Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePassword() {
        val password = etNewPassword.text.toString()
        if (password.length < 6) {
            etNewPassword.error = getString(R.string.error_password_short)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repository.updatePassword(session.getUserId(), password)
            etNewPassword.setText("")
            Toast.makeText(requireContext(), R.string.password_changed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun askLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.btn_logout)
            .setMessage(R.string.logout_question)
            .setPositiveButton(R.string.yes) { _, _ ->
                session.logout()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
}
