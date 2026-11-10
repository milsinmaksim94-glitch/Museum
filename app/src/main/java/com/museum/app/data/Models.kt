package com.museum.app.data

// Тут все классы-модели, которые мы достаём из базы.
// Сделал их data class, так удобнее

data class User(
    val id: Long,
    val login: String,
    val name: String,
    val role: String // "user" или "admin"
) {
    fun isAdmin(): Boolean = role == DBHelper.ROLE_ADMIN
}

// Зал музея (по сути категория экспонатов)
data class Hall(
    val id: Long,
    val name: String,
    val floor: Int,
    val description: String
)

data class Exhibit(
    val id: Long,
    val hallId: Long,
    val hallName: String,
    val title: String,
    val author: String,
    val year: String,
    val description: String,
    val rating: Float = 0f // средняя оценка по комментам
)

data class Comment(
    val id: Long,
    val exhibitId: Long,
    val userId: Long,
    val userName: String,
    val text: String,
    val rating: Int,
    val date: String
)

// Экскурсия, на неё можно записаться
data class Tour(
    val id: Long,
    val hallId: Long,
    val hallName: String,
    val title: String,
    val description: String,
    val price: Int, // цена взрослого билета в рублях
    val duration: Int // длительность в минутах
)

data class Booking(
    val id: Long,
    val userId: Long,
    val userLogin: String,
    val tourId: Long,
    val tourTitle: String,
    val date: String,
    val time: String,
    val adults: Int,
    val children: Int,
    val audioGuide: Boolean,
    val total: Int,
    val status: String
)
