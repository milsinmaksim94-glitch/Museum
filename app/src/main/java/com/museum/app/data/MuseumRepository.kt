package com.museum.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Репозиторий - тут все SQL запросы.
// Экраны (Activity/Fragment) сами в базу не лезут, только через этот класс.
// Все функции suspend и выполняются в Dispatchers.IO, то есть в фоновом потоке,
// чтобы интерфейс не зависал
class MuseumRepository(context: Context) {

    private val dbHelper = DBHelper.getInstance(context)

    // общий запрос для экспонатов, сразу с названием зала и средней оценкой
    private val exhibitSelect =
        "SELECT e.id, e.hall_id, h.name AS hall_name, e.title, e.author, e.year, e.description, " +
                "(SELECT AVG(c.rating) FROM ${DBHelper.TABLE_COMMENTS} c WHERE c.exhibit_id = e.id) AS avg_rating " +
                "FROM ${DBHelper.TABLE_EXHIBITS} e " +
                "JOIN ${DBHelper.TABLE_HALLS} h ON e.hall_id = h.id "

    // ===================== ПОЛЬЗОВАТЕЛИ =====================

    // возвращает id нового юзера или -1 если такой логин уже есть
    suspend fun register(login: String, password: String, name: String): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cursor = db.rawQuery(
            "SELECT id FROM ${DBHelper.TABLE_USERS} WHERE login = ?", arrayOf(login)
        )
        val exists = cursor.count > 0
        cursor.close()
        if (exists) {
            return@withContext -1L
        }
        val cv = ContentValues()
        cv.put("login", login)
        cv.put("password", PasswordUtils.hash(password))
        cv.put("name", name)
        cv.put("role", DBHelper.ROLE_USER)
        db.insert(DBHelper.TABLE_USERS, null, cv)
    }

    // если логин и пароль верные - вернёт пользователя, иначе null
    suspend fun login(login: String, password: String): User? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, login, name, role FROM ${DBHelper.TABLE_USERS} WHERE login = ? AND password = ?",
            arrayOf(login, PasswordUtils.hash(password))
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor)
        }
        cursor.close()
        user
    }

    suspend fun getUser(id: Long): User? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, login, name, role FROM ${DBHelper.TABLE_USERS} WHERE id = ?",
            arrayOf(id.toString())
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor)
        }
        cursor.close()
        user
    }

    suspend fun updateUserName(id: Long, name: String) = withContext(Dispatchers.IO) {
        val cv = ContentValues()
        cv.put("name", name)
        dbHelper.writableDatabase.update(DBHelper.TABLE_USERS, cv, "id = ?", arrayOf(id.toString()))
    }

    suspend fun updatePassword(id: Long, newPassword: String) = withContext(Dispatchers.IO) {
        val cv = ContentValues()
        cv.put("password", PasswordUtils.hash(newPassword))
        dbHelper.writableDatabase.update(DBHelper.TABLE_USERS, cv, "id = ?", arrayOf(id.toString()))
    }

    // ===================== ЗАЛЫ =====================

    suspend fun getHalls(): List<Hall> = withContext(Dispatchers.IO) {
        val list = ArrayList<Hall>()
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT id, name, floor, description FROM ${DBHelper.TABLE_HALLS} ORDER BY id", null
        )
        while (cursor.moveToNext()) {
            list.add(
                Hall(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("floor")),
                    cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: ""
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun addHall(name: String, floor: Int, description: String): Long = withContext(Dispatchers.IO) {
        val cv = ContentValues()
        cv.put("name", name)
        cv.put("floor", floor)
        cv.put("description", description)
        dbHelper.writableDatabase.insert(DBHelper.TABLE_HALLS, null, cv)
    }

    suspend fun deleteHall(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBHelper.TABLE_HALLS, "id = ?", arrayOf(id.toString()))
    }

    // ===================== ЭКСПОНАТЫ =====================

    // hallId = 0 значит "все залы"
    suspend fun getExhibits(hallId: Long): List<Exhibit> = withContext(Dispatchers.IO) {
        val cursor = if (hallId == 0L) {
            dbHelper.readableDatabase.rawQuery("$exhibitSelect ORDER BY e.title", null)
        } else {
            dbHelper.readableDatabase.rawQuery(
                "$exhibitSelect WHERE e.hall_id = ? ORDER BY e.title", arrayOf(hallId.toString())
            )
        }
        val list = ArrayList<Exhibit>()
        while (cursor.moveToNext()) {
            list.add(cursorToExhibit(cursor))
        }
        cursor.close()
        list
    }

    // поиск по названию и автору, можно ещё ограничить залом
    suspend fun searchExhibits(text: String, hallId: Long): List<Exhibit> = withContext(Dispatchers.IO) {
        val like = "%$text%"
        val cursor = if (hallId == 0L) {
            dbHelper.readableDatabase.rawQuery(
                "$exhibitSelect WHERE (e.title LIKE ? OR e.author LIKE ?) ORDER BY e.title",
                arrayOf(like, like)
            )
        } else {
            dbHelper.readableDatabase.rawQuery(
                "$exhibitSelect WHERE (e.title LIKE ? OR e.author LIKE ?) AND e.hall_id = ? ORDER BY e.title",
                arrayOf(like, like, hallId.toString())
            )
        }
        val list = ArrayList<Exhibit>()
        while (cursor.moveToNext()) {
            list.add(cursorToExhibit(cursor))
        }
        cursor.close()
        list
    }

    suspend fun getExhibit(id: Long): Exhibit? = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "$exhibitSelect WHERE e.id = ?", arrayOf(id.toString())
        )
        var exhibit: Exhibit? = null
        if (cursor.moveToFirst()) {
            exhibit = cursorToExhibit(cursor)
        }
        cursor.close()
        exhibit
    }

    suspend fun addExhibit(hallId: Long, title: String, author: String, year: String, description: String): Long =
        withContext(Dispatchers.IO) {
            val cv = ContentValues()
            cv.put("hall_id", hallId)
            cv.put("title", title)
            cv.put("author", author)
            cv.put("year", year)
            cv.put("description", description)
            dbHelper.writableDatabase.insert(DBHelper.TABLE_EXHIBITS, null, cv)
        }

    suspend fun updateExhibit(id: Long, hallId: Long, title: String, author: String, year: String, description: String) =
        withContext(Dispatchers.IO) {
            val cv = ContentValues()
            cv.put("hall_id", hallId)
            cv.put("title", title)
            cv.put("author", author)
            cv.put("year", year)
            cv.put("description", description)
            dbHelper.writableDatabase.update(DBHelper.TABLE_EXHIBITS, cv, "id = ?", arrayOf(id.toString()))
        }

    suspend fun deleteExhibit(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBHelper.TABLE_EXHIBITS, "id = ?", arrayOf(id.toString()))
    }

    // ===================== КОММЕНТАРИИ =====================

    suspend fun getComments(exhibitId: Long): List<Comment> = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT c.id, c.exhibit_id, c.user_id, u.name AS user_name, c.text, c.rating, c.date " +
                    "FROM ${DBHelper.TABLE_COMMENTS} c " +
                    "JOIN ${DBHelper.TABLE_USERS} u ON c.user_id = u.id " +
                    "WHERE c.exhibit_id = ? ORDER BY c.id DESC",
            arrayOf(exhibitId.toString())
        )
        val list = ArrayList<Comment>()
        while (cursor.moveToNext()) {
            list.add(
                Comment(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("exhibit_id")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("user_name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("text")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("rating")),
                    cursor.getString(cursor.getColumnIndexOrThrow("date"))
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun addComment(exhibitId: Long, userId: Long, text: String, rating: Int): Long = withContext(Dispatchers.IO) {
        val cv = ContentValues()
        cv.put("exhibit_id", exhibitId)
        cv.put("user_id", userId)
        cv.put("text", text)
        cv.put("rating", rating)
        cv.put("date", currentDate())
        dbHelper.writableDatabase.insert(DBHelper.TABLE_COMMENTS, null, cv)
    }

    suspend fun deleteComment(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBHelper.TABLE_COMMENTS, "id = ?", arrayOf(id.toString()))
    }

    // ===================== ИЗБРАННОЕ =====================

    suspend fun isFavorite(userId: Long, exhibitId: Long): Boolean = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT 1 FROM ${DBHelper.TABLE_FAVORITES} WHERE user_id = ? AND exhibit_id = ?",
            arrayOf(userId.toString(), exhibitId.toString())
        )
        val result = cursor.count > 0
        cursor.close()
        result
    }

    suspend fun addFavorite(userId: Long, exhibitId: Long) = withContext(Dispatchers.IO) {
        val cv = ContentValues()
        cv.put("user_id", userId)
        cv.put("exhibit_id", exhibitId)
        dbHelper.writableDatabase.insert(DBHelper.TABLE_FAVORITES, null, cv)
    }

    suspend fun removeFavorite(userId: Long, exhibitId: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(
            DBHelper.TABLE_FAVORITES, "user_id = ? AND exhibit_id = ?",
            arrayOf(userId.toString(), exhibitId.toString())
        )
    }

    suspend fun getFavorites(userId: Long): List<Exhibit> = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "$exhibitSelect JOIN ${DBHelper.TABLE_FAVORITES} f ON f.exhibit_id = e.id " +
                    "WHERE f.user_id = ? ORDER BY e.title",
            arrayOf(userId.toString())
        )
        val list = ArrayList<Exhibit>()
        while (cursor.moveToNext()) {
            list.add(cursorToExhibit(cursor))
        }
        cursor.close()
        list
    }

    // ===================== ЭКСКУРСИИ =====================

    suspend fun getTours(): List<Tour> = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT t.*, h.name AS hall_name FROM ${DBHelper.TABLE_TOURS} t " +
                    "JOIN ${DBHelper.TABLE_HALLS} h ON t.hall_id = h.id ORDER BY t.title",
            null
        )
        val list = ArrayList<Tour>()
        while (cursor.moveToNext()) {
            list.add(cursorToTour(cursor))
        }
        cursor.close()
        list
    }

    suspend fun getTour(id: Long): Tour? = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT t.*, h.name AS hall_name FROM ${DBHelper.TABLE_TOURS} t " +
                    "JOIN ${DBHelper.TABLE_HALLS} h ON t.hall_id = h.id WHERE t.id = ?",
            arrayOf(id.toString())
        )
        var tour: Tour? = null
        if (cursor.moveToFirst()) {
            tour = cursorToTour(cursor)
        }
        cursor.close()
        tour
    }

    // ===================== БРОНИРОВАНИЯ =====================

    suspend fun addBooking(
        userId: Long, tourId: Long, date: String, time: String,
        adults: Int, children: Int, audioGuide: Boolean, total: Int
    ): Long = withContext(Dispatchers.IO) {
        val cv = ContentValues()
        cv.put("user_id", userId)
        cv.put("tour_id", tourId)
        cv.put("date", date)
        cv.put("time", time)
        cv.put("adults", adults)
        cv.put("children", children)
        cv.put("audio_guide", if (audioGuide) 1 else 0)
        cv.put("total", total)
        cv.put("status", DBHelper.STATUS_NEW)
        dbHelper.writableDatabase.insert(DBHelper.TABLE_BOOKINGS, null, cv)
    }

    // userId = 0 значит все брони (для админа)
    suspend fun getBookings(userId: Long): List<Booking> = withContext(Dispatchers.IO) {
        var sql = "SELECT b.*, t.title AS tour_title, u.login AS user_login " +
                "FROM ${DBHelper.TABLE_BOOKINGS} b " +
                "JOIN ${DBHelper.TABLE_TOURS} t ON b.tour_id = t.id " +
                "JOIN ${DBHelper.TABLE_USERS} u ON b.user_id = u.id "
        val cursor = if (userId == 0L) {
            sql += "ORDER BY b.id DESC"
            dbHelper.readableDatabase.rawQuery(sql, null)
        } else {
            sql += "WHERE b.user_id = ? ORDER BY b.id DESC"
            dbHelper.readableDatabase.rawQuery(sql, arrayOf(userId.toString()))
        }
        val list = ArrayList<Booking>()
        while (cursor.moveToNext()) {
            list.add(
                Booking(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("user_login")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("tour_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("tour_title")),
                    cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    cursor.getString(cursor.getColumnIndexOrThrow("time")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("adults")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("children")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("audio_guide")) == 1,
                    cursor.getInt(cursor.getColumnIndexOrThrow("total")),
                    cursor.getString(cursor.getColumnIndexOrThrow("status"))
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun updateBookingStatus(id: Long, status: String) = withContext(Dispatchers.IO) {
        val cv = ContentValues()
        cv.put("status", status)
        dbHelper.writableDatabase.update(DBHelper.TABLE_BOOKINGS, cv, "id = ?", arrayOf(id.toString()))
    }

    suspend fun deleteBooking(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBHelper.TABLE_BOOKINGS, "id = ?", arrayOf(id.toString()))
    }

    // ===================== ВСПОМОГАТЕЛЬНОЕ =====================

    private fun cursorToUser(cursor: Cursor): User {
        return User(
            cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            cursor.getString(cursor.getColumnIndexOrThrow("login")),
            cursor.getString(cursor.getColumnIndexOrThrow("name")),
            cursor.getString(cursor.getColumnIndexOrThrow("role"))
        )
    }

    private fun cursorToExhibit(cursor: Cursor): Exhibit {
        return Exhibit(
            cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            cursor.getLong(cursor.getColumnIndexOrThrow("hall_id")),
            cursor.getString(cursor.getColumnIndexOrThrow("hall_name")),
            cursor.getString(cursor.getColumnIndexOrThrow("title")),
            cursor.getString(cursor.getColumnIndexOrThrow("author")) ?: "",
            cursor.getString(cursor.getColumnIndexOrThrow("year")) ?: "",
            cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: "",
            cursor.getFloat(cursor.getColumnIndexOrThrow("avg_rating")) // если комментов нет, будет 0
        )
    }

    private fun cursorToTour(cursor: Cursor): Tour {
        return Tour(
            cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            cursor.getLong(cursor.getColumnIndexOrThrow("hall_id")),
            cursor.getString(cursor.getColumnIndexOrThrow("hall_name")),
            cursor.getString(cursor.getColumnIndexOrThrow("title")),
            cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: "",
            cursor.getInt(cursor.getColumnIndexOrThrow("price")),
            cursor.getInt(cursor.getColumnIndexOrThrow("duration"))
        )
    }

    private fun currentDate(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}
