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

// Площадка музея (у музея несколько зданий, как филиалы)
data class Place(
    val id: Long,
    val name: String,
    val address: String,
    val metro: String,
    val hours: String,
    val phone: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val image: String, // фото: "res:имя_картинки" или base64 из галереи, может быть пустым
    val cover: String = image // что показывать на обложке: своё фото, а если его нет - фото экспоната отсюда
)

// Зал музея (по сути категория экспонатов), находится в какой-то площадке
data class Hall(
    val id: Long,
    val placeId: Long,
    val placeName: String,
    val name: String,
    val floor: Int,
    val description: String
)

data class Exhibit(
    val id: Long,
    val hallId: Long,
    val hallName: String,
    val placeName: String,
    val title: String,
    val author: String,
    val year: String,
    val description: String,
    val invNumber: String, // инвентарный номер, как в настоящем музее
    val material: String,
    val image: String,
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

// Экскурсовод
data class Guide(
    val id: Long,
    val name: String,
    val position: String, // должность, например "искусствовед"
    val experience: Int, // стаж в годах
    val languages: String,
    val bio: String,
    val image: String
)

// Экскурсия, на неё можно записаться
data class Tour(
    val id: Long,
    val hallId: Long,
    val hallName: String,
    val placeName: String,
    val guideId: Long,
    val guideName: String,
    val title: String,
    val description: String,
    val format: String, // экскурсия, спектакль, квест...
    val price: Int, // цена взрослого билета в рублях
    val duration: Int, // длительность в минутах
    val maxPeople: Int, // сколько человек максимум в группе на один сеанс
    val sessions: String // время начала через запятую, например "11:00,15:00"
) {
    // список сеансов в виде списка строк
    fun sessionList(): List<String> {
        val result = ArrayList<String>()
        for (s in sessions.split(",")) {
            if (s.trim().isNotEmpty()) result.add(s.trim())
        }
        return result
    }
}

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
