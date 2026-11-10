package com.museum.app.data

import java.security.MessageDigest

// Пароли в открытом виде хранить нельзя, поэтому храним хеш SHA-256
object PasswordUtils {

    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray())
        // переводим байты в строку из hex символов
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
