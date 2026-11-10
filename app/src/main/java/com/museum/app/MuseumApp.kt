package com.museum.app

import android.app.Application
import com.museum.app.utils.SessionManager

// Класс приложения, нужен чтобы сразу при запуске поставить нужную тему
class MuseumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager(this).applyTheme()
    }
}
