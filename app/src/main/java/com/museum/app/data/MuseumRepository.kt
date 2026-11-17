package com.museum.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.museum.app.utils.IdUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Репозиторий - тут все SQL запросы.
// Экраны (Activity/Fragment) сами в базу не лезут, только через этот класс.
// Все функции suspend и выполняются в Dispatchers.IO, то есть в фоновом потоке,
// чтобы интерфейс не зависал.
// После каждого изменения вызываем CloudSync, чтобы изменение ушло в общую базу Firebase
class MuseumRepository(context: Context) {

    private val dbHelper = DBHelper.getInstance(context)

    // общий запрос для экспонатов, сразу с названием зала, площадки и средней оценкой
    private val exhibitSelect =
        "SELECT e.*, h.name AS hall_name, p.name AS place_name, " +
                "(SELECT AVG(c.rating) FROM ${DBHelper.TABLE_COMMENTS} c WHERE c.exhibit_id = e.id) AS avg_rating " +
                "FROM ${DBHelper.TABLE_EXHIBITS} e " +
                "JOIN ${DBHelper.TABLE_HALLS} h ON e.hall_id = h.id " +
                "JOIN ${DBHelper.TABLE_PLACES} p ON h.place_id = p.id "

    // общий запрос для экскурсий: + зал, площадка и экскурсовод
    private val tourSelect =
        "SELECT t.*, h.name AS hall_name, p.name AS place_name, g.name AS guide_name " +
                "FROM ${DBHelper.TABLE_TOURS} t " +
                "JOIN ${DBHelper.TABLE_HALLS} h ON t.hall_id = h.id " +
                "JOIN ${DBHelper.TABLE_PLACES} p ON h.place_id = p.id " +
                "JOIN ${DBHelper.TABLE_GUIDES} g ON t.guide_id = g.id "

    // синхронизация

    // скачать свежие данные из облака. true - получилось
    suspend fun syncFromCloud(): Boolean = CloudSync.downloadAll(dbHelper)

    // пользователи

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
        val id = IdUtils.newId()
        val cv = ContentValues()
        cv.put("id", id)
        cv.put("login", login)
        cv.put("password", PasswordUtils.hash(password))
        cv.put("name", name)
        cv.put("role", DBHelper.ROLE_USER)
        db.insert(DBHelper.TABLE_USERS, null, cv)
        CloudSync.push(db, DBHelper.TABLE_USERS, id)
        id
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
        val db = dbHelper.writableDatabase
        db.update(DBHelper.TABLE_USERS, cv, "id = ?", arrayOf(id.toString()))
        CloudSync.push(db, DBHelper.TABLE_USERS, id)
    }

    suspend fun updatePassword(id: Long, newPassword: String) = withContext(Dispatchers.IO) {
        val cv = ContentValues()
        cv.put("password", PasswordUtils.hash(newPassword))
        val db = dbHelper.writableDatabase
        db.update(DBHelper.TABLE_USERS, cv, "id = ?", arrayOf(id.toString()))
        CloudSync.push(db, DBHelper.TABLE_USERS, id)
    }

    // площадки

    // если у площадки нет своего фото, на обложку берём фото первого экспоната из её залов
    private val placeSelect =
        "SELECT p.*, COALESCE(NULLIF(p.image, ''), " +
                "(SELECT e.image FROM ${DBHelper.TABLE_EXHIBITS} e " +
                "JOIN ${DBHelper.TABLE_HALLS} h ON e.hall_id = h.id " +
                "WHERE h.place_id = p.id AND e.image != '' ORDER BY e.id LIMIT 1), '') AS cover " +
                "FROM ${DBHelper.TABLE_PLACES} p "

    suspend fun getPlaces(): List<Place> = withContext(Dispatchers.IO) {
        val list = ArrayList<Place>()
        val cursor = dbHelper.readableDatabase.rawQuery(
            "$placeSelect ORDER BY p.id", null
        )
        while (cursor.moveToNext()) {
            list.add(cursorToPlace(cursor))
        }
        cursor.close()
        list
    }

    suspend fun getPlace(id: Long): Place? = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "$placeSelect WHERE p.id = ?", arrayOf(id.toString())
        )
        var place: Place? = null
        if (cursor.moveToFirst()) {
            place = cursorToPlace(cursor)
        }
        cursor.close()
        place
    }

    // id = -1 значит новая площадка, иначе изменяем существующую
    suspend fun savePlace(
        id: Long, name: String, address: String, metro: String, hours: String,
        phone: String, description: String, latitude: Double, longitude: Double, image: String
    ): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues()
        cv.put("name", name)
        cv.put("address", address)
        cv.put("metro", metro)
        cv.put("hours", hours)
        cv.put("phone", phone)
        cv.put("description", description)
        cv.put("latitude", latitude)
        cv.put("longitude", longitude)
        cv.put("image", image)
        var placeId = id
        if (id == -1L) {
            placeId = IdUtils.newId()
            cv.put("id", placeId)
            db.insert(DBHelper.TABLE_PLACES, null, cv)
        } else {
            db.update(DBHelper.TABLE_PLACES, cv, "id = ?", arrayOf(id.toString()))
        }
        CloudSync.push(db, DBHelper.TABLE_PLACES, placeId)
        placeId
    }

    suspend fun deletePlace(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBHelper.TABLE_PLACES, "id = ?", arrayOf(id.toString()))
        CloudSync.pushDelete(DBHelper.TABLE_PLACES, id.toString())
    }

    // залы

    // placeId = 0 значит все площадки
    suspend fun getHalls(placeId: Long = 0): List<Hall> = withContext(Dispatchers.IO) {
        val list = ArrayList<Hall>()
        var sql = "SELECT h.*, p.name AS place_name FROM ${DBHelper.TABLE_HALLS} h " +
                "JOIN ${DBHelper.TABLE_PLACES} p ON h.place_id = p.id "
        val cursor = if (placeId == 0L) {
            sql += "ORDER BY h.id"
            dbHelper.readableDatabase.rawQuery(sql, null)
        } else {
            sql += "WHERE h.place_id = ? ORDER BY h.floor, h.id"
            dbHelper.readableDatabase.rawQuery(sql, arrayOf(placeId.toString()))
        }
        while (cursor.moveToNext()) {
            list.add(
                Hall(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("place_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("place_name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("floor")),
                    cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: ""
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun addHall(placeId: Long, name: String, floor: Int, description: String): Long = withContext(Dispatchers.IO) {
        val id = IdUtils.newId()
        val cv = ContentValues()
        cv.put("id", id)
        cv.put("place_id", placeId)
        cv.put("name", name)
        cv.put("floor", floor)
        cv.put("description", description)
        val db = dbHelper.writableDatabase
        db.insert(DBHelper.TABLE_HALLS, null, cv)
        CloudSync.push(db, DBHelper.TABLE_HALLS, id)
        id
    }

    suspend fun deleteHall(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBHelper.TABLE_HALLS, "id = ?", arrayOf(id.toString()))
        CloudSync.pushDelete(DBHelper.TABLE_HALLS, id.toString())
    }

    // экспонаты

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

    // поиск по названию, автору и инвентарному номеру, можно ещё ограничить залом
    suspend fun searchExhibits(text: String, hallId: Long): List<Exhibit> = withContext(Dispatchers.IO) {
        val like = "%$text%"
        val where = "WHERE (e.title LIKE ? OR e.author LIKE ? OR e.inv_number LIKE ?)"
        val cursor = if (hallId == 0L) {
            dbHelper.readableDatabase.rawQuery(
                "$exhibitSelect $where ORDER BY e.title", arrayOf(like, like, like)
            )
        } else {
            dbHelper.readableDatabase.rawQuery(
                "$exhibitSelect $where AND e.hall_id = ? ORDER BY e.title",
                arrayOf(like, like, like, hallId.toString())
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

    // id = -1 значит новый экспонат, иначе изменяем существующий
    suspend fun saveExhibit(
        id: Long, hallId: Long, title: String, author: String, year: String,
        description: String, invNumber: String, material: String, image: String
    ): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues()
        cv.put("hall_id", hallId)
        cv.put("title", title)
        cv.put("author", author)
        cv.put("year", year)
        cv.put("description", description)
        cv.put("inv_number", invNumber)
        cv.put("material", material)
        cv.put("image", image)
        var exhibitId = id
        if (id == -1L) {
            exhibitId = IdUtils.newId()
            cv.put("id", exhibitId)
            db.insert(DBHelper.TABLE_EXHIBITS, null, cv)
        } else {
            db.update(DBHelper.TABLE_EXHIBITS, cv, "id = ?", arrayOf(id.toString()))
        }
        CloudSync.push(db, DBHelper.TABLE_EXHIBITS, exhibitId)
        exhibitId
    }

    suspend fun deleteExhibit(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBHelper.TABLE_EXHIBITS, "id = ?", arrayOf(id.toString()))
        CloudSync.pushDelete(DBHelper.TABLE_EXHIBITS, id.toString())
    }

    // комментарии

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
        val id = IdUtils.newId()
        val cv = ContentValues()
        cv.put("id", id)
        cv.put("exhibit_id", exhibitId)
        cv.put("user_id", userId)
        cv.put("text", text)
        cv.put("rating", rating)
        cv.put("date", currentDate())
        val db = dbHelper.writableDatabase
        db.insert(DBHelper.TABLE_COMMENTS, null, cv)
        CloudSync.push(db, DBHelper.TABLE_COMMENTS, id)
        id
    }

    suspend fun deleteComment(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBHelper.TABLE_COMMENTS, "id = ?", arrayOf(id.toString()))
        CloudSync.pushDelete(DBHelper.TABLE_COMMENTS, id.toString())
    }

    // избранное

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
        CloudSync.pushFavorite(userId, exhibitId)
    }

    suspend fun removeFavorite(userId: Long, exhibitId: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(
            DBHelper.TABLE_FAVORITES, "user_id = ? AND exhibit_id = ?",
            arrayOf(userId.toString(), exhibitId.toString())
        )
        CloudSync.pushDelete(DBHelper.TABLE_FAVORITES, "${userId}_$exhibitId")
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

    // экскурсоводы

    suspend fun getGuides(): List<Guide> = withContext(Dispatchers.IO) {
        val list = ArrayList<Guide>()
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT * FROM ${DBHelper.TABLE_GUIDES} ORDER BY name", null
        )
        while (cursor.moveToNext()) {
            list.add(cursorToGuide(cursor))
        }
        cursor.close()
        list
    }

    suspend fun getGuide(id: Long): Guide? = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT * FROM ${DBHelper.TABLE_GUIDES} WHERE id = ?", arrayOf(id.toString())
        )
        var guide: Guide? = null
        if (cursor.moveToFirst()) {
            guide = cursorToGuide(cursor)
        }
        cursor.close()
        guide
    }

    // id = -1 значит новый экскурсовод
    suspend fun saveGuide(
        id: Long, name: String, position: String, experience: Int,
        languages: String, bio: String, image: String
    ): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues()
        cv.put("name", name)
        cv.put("position", position)
        cv.put("experience", experience)
        cv.put("languages", languages)
        cv.put("bio", bio)
        cv.put("image", image)
        var guideId = id
        if (id == -1L) {
            guideId = IdUtils.newId()
            cv.put("id", guideId)
            db.insert(DBHelper.TABLE_GUIDES, null, cv)
        } else {
            db.update(DBHelper.TABLE_GUIDES, cv, "id = ?", arrayOf(id.toString()))
        }
        CloudSync.push(db, DBHelper.TABLE_GUIDES, guideId)
        guideId
    }

    suspend fun deleteGuide(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBHelper.TABLE_GUIDES, "id = ?", arrayOf(id.toString()))
        CloudSync.pushDelete(DBHelper.TABLE_GUIDES, id.toString())
    }

    // экскурсии

    suspend fun getTours(): List<Tour> = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.rawQuery("$tourSelect ORDER BY t.title", null)
        val list = ArrayList<Tour>()
        while (cursor.moveToNext()) {
            list.add(cursorToTour(cursor))
        }
        cursor.close()
        list
    }

    // экскурсии одного экскурсовода
    suspend fun getToursByGuide(guideId: Long): List<Tour> = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "$tourSelect WHERE t.guide_id = ? ORDER BY t.title", arrayOf(guideId.toString())
        )
        val list = ArrayList<Tour>()
        while (cursor.moveToNext()) {
            list.add(cursorToTour(cursor))
        }
        cursor.close()
        list
    }

    // экскурсии на одной площадке
    suspend fun getToursByPlace(placeId: Long): List<Tour> = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "$tourSelect WHERE h.place_id = ? ORDER BY t.title", arrayOf(placeId.toString())
        )
        val list = ArrayList<Tour>()
        while (cursor.moveToNext()) {
            list.add(cursorToTour(cursor))
        }
        cursor.close()
        list
    }

    suspend fun getTour(id: Long): Tour? = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.rawQuery("$tourSelect WHERE t.id = ?", arrayOf(id.toString()))
        var tour: Tour? = null
        if (cursor.moveToFirst()) {
            tour = cursorToTour(cursor)
        }
        cursor.close()
        tour
    }

    // бронирования

    // сколько человек уже записано на этот сеанс (отменённые не считаем)
    suspend fun getBookedPeople(tourId: Long, date: String, time: String): Int = withContext(Dispatchers.IO) {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT SUM(adults + children) FROM ${DBHelper.TABLE_BOOKINGS} " +
                    "WHERE tour_id = ? AND date = ? AND time = ? AND status != ?",
            arrayOf(tourId.toString(), date, time, DBHelper.STATUS_CANCELLED)
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0) // если записей нет, SUM вернёт NULL, а getInt даст 0
        }
        cursor.close()
        count
    }

    suspend fun addBooking(
        userId: Long, tourId: Long, date: String, time: String,
        adults: Int, children: Int, audioGuide: Boolean, total: Int
    ): Long = withContext(Dispatchers.IO) {
        val id = IdUtils.newId()
        val cv = ContentValues()
        cv.put("id", id)
        cv.put("user_id", userId)
        cv.put("tour_id", tourId)
        cv.put("date", date)
        cv.put("time", time)
        cv.put("adults", adults)
        cv.put("children", children)
        cv.put("audio_guide", if (audioGuide) 1 else 0)
        cv.put("total", total)
        cv.put("status", DBHelper.STATUS_NEW)
        val db = dbHelper.writableDatabase
        db.insert(DBHelper.TABLE_BOOKINGS, null, cv)
        CloudSync.push(db, DBHelper.TABLE_BOOKINGS, id)
        id
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
        val db = dbHelper.writableDatabase
        db.update(DBHelper.TABLE_BOOKINGS, cv, "id = ?", arrayOf(id.toString()))
        CloudSync.push(db, DBHelper.TABLE_BOOKINGS, id)
    }

    suspend fun deleteBooking(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBHelper.TABLE_BOOKINGS, "id = ?", arrayOf(id.toString()))
        CloudSync.pushDelete(DBHelper.TABLE_BOOKINGS, id.toString())
    }

    // вспомогательное

    private fun str(cursor: Cursor, column: String): String {
        return cursor.getString(cursor.getColumnIndexOrThrow(column)) ?: ""
    }

    private fun cursorToUser(cursor: Cursor): User {
        return User(
            cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            str(cursor, "login"),
            str(cursor, "name"),
            str(cursor, "role")
        )
    }

    private fun cursorToPlace(cursor: Cursor): Place {
        return Place(
            cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            str(cursor, "name"),
            str(cursor, "address"),
            str(cursor, "metro"),
            str(cursor, "hours"),
            str(cursor, "phone"),
            str(cursor, "description"),
            cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
            cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
            str(cursor, "image"),
            str(cursor, "cover")
        )
    }

    private fun cursorToExhibit(cursor: Cursor): Exhibit {
        return Exhibit(
            cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            cursor.getLong(cursor.getColumnIndexOrThrow("hall_id")),
            str(cursor, "hall_name"),
            str(cursor, "place_name"),
            str(cursor, "title"),
            str(cursor, "author"),
            str(cursor, "year"),
            str(cursor, "description"),
            str(cursor, "inv_number"),
            str(cursor, "material"),
            str(cursor, "image"),
            cursor.getFloat(cursor.getColumnIndexOrThrow("avg_rating")) // если комментов нет, будет 0
        )
    }

    private fun cursorToGuide(cursor: Cursor): Guide {
        return Guide(
            cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            str(cursor, "name"),
            str(cursor, "position"),
            cursor.getInt(cursor.getColumnIndexOrThrow("experience")),
            str(cursor, "languages"),
            str(cursor, "bio"),
            str(cursor, "image")
        )
    }

    private fun cursorToTour(cursor: Cursor): Tour {
        return Tour(
            cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            cursor.getLong(cursor.getColumnIndexOrThrow("hall_id")),
            str(cursor, "hall_name"),
            str(cursor, "place_name"),
            cursor.getLong(cursor.getColumnIndexOrThrow("guide_id")),
            str(cursor, "guide_name"),
            str(cursor, "title"),
            str(cursor, "description"),
            str(cursor, "format"),
            cursor.getInt(cursor.getColumnIndexOrThrow("price")),
            cursor.getInt(cursor.getColumnIndexOrThrow("duration")),
            cursor.getInt(cursor.getColumnIndexOrThrow("max_people")),
            str(cursor, "sessions")
        )
    }

    private fun currentDate(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}
