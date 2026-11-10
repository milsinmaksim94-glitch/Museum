package com.museum.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Класс для создания и обновления базы данных
// Сами запросы (выборки, добавление и т.д.) лежат в MuseumRepository
class DBHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "museum.db"
        const val DB_VERSION = 1

        // названия таблиц
        const val TABLE_USERS = "users"
        const val TABLE_HALLS = "halls"
        const val TABLE_EXHIBITS = "exhibits"
        const val TABLE_COMMENTS = "comments"
        const val TABLE_FAVORITES = "favorites"
        const val TABLE_TOURS = "tours"
        const val TABLE_BOOKINGS = "bookings"

        // роли
        const val ROLE_USER = "user"
        const val ROLE_ADMIN = "admin"

        // статусы брони
        const val STATUS_NEW = "new"
        const val STATUS_CONFIRMED = "confirmed"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_DONE = "done"

        // делаем один экземпляр на всё приложение, чтобы не плодить подключения
        @Volatile
        private var instance: DBHelper? = null

        fun getInstance(context: Context): DBHelper {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = DBHelper(context.applicationContext)
                    }
                }
            }
            return instance!!
        }
    }

    // без этого SQLite не проверяет внешние ключи!
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_USERS (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "login TEXT NOT NULL UNIQUE, " +
                    "password TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "role TEXT NOT NULL DEFAULT '$ROLE_USER')"
        )

        db.execSQL(
            "CREATE TABLE $TABLE_HALLS (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "floor INTEGER NOT NULL DEFAULT 1, " +
                    "description TEXT)"
        )

        // если удалить зал - удалятся и его экспонаты (CASCADE)
        db.execSQL(
            "CREATE TABLE $TABLE_EXHIBITS (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "hall_id INTEGER NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "author TEXT, " +
                    "year TEXT, " +
                    "description TEXT, " +
                    "FOREIGN KEY (hall_id) REFERENCES $TABLE_HALLS(id) ON DELETE CASCADE)"
        )

        db.execSQL(
            "CREATE TABLE $TABLE_COMMENTS (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "exhibit_id INTEGER NOT NULL, " +
                    "user_id INTEGER NOT NULL, " +
                    "text TEXT NOT NULL, " +
                    "rating INTEGER NOT NULL, " +
                    "date TEXT NOT NULL, " +
                    "FOREIGN KEY (exhibit_id) REFERENCES $TABLE_EXHIBITS(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (user_id) REFERENCES $TABLE_USERS(id) ON DELETE CASCADE)"
        )

        // составной ключ, чтобы один экспонат нельзя было добавить в избранное 2 раза
        db.execSQL(
            "CREATE TABLE $TABLE_FAVORITES (" +
                    "user_id INTEGER NOT NULL, " +
                    "exhibit_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (user_id, exhibit_id), " +
                    "FOREIGN KEY (user_id) REFERENCES $TABLE_USERS(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (exhibit_id) REFERENCES $TABLE_EXHIBITS(id) ON DELETE CASCADE)"
        )

        db.execSQL(
            "CREATE TABLE $TABLE_TOURS (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "hall_id INTEGER NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT, " +
                    "price INTEGER NOT NULL, " +
                    "duration INTEGER NOT NULL, " +
                    "FOREIGN KEY (hall_id) REFERENCES $TABLE_HALLS(id) ON DELETE CASCADE)"
        )

        db.execSQL(
            "CREATE TABLE $TABLE_BOOKINGS (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL, " +
                    "tour_id INTEGER NOT NULL, " +
                    "date TEXT NOT NULL, " +
                    "time TEXT NOT NULL, " +
                    "adults INTEGER NOT NULL, " +
                    "children INTEGER NOT NULL, " +
                    "audio_guide INTEGER NOT NULL DEFAULT 0, " +
                    "total INTEGER NOT NULL, " +
                    "status TEXT NOT NULL DEFAULT '$STATUS_NEW', " +
                    "FOREIGN KEY (user_id) REFERENCES $TABLE_USERS(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (tour_id) REFERENCES $TABLE_TOURS(id) ON DELETE CASCADE)"
        )

        fillTestData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // при смене версии просто пересоздаём всё заново
        // (для учебного проекта норм, данные не жалко)
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKINGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TOURS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXHIBITS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HALLS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // Заполняем базу начальными данными, чтобы при первом запуске не было пусто
    private fun fillTestData(db: SQLiteDatabase) {
        // админ по умолчанию: логин admin, пароль admin123
        val admin = ContentValues()
        admin.put("login", "admin")
        admin.put("password", PasswordUtils.hash("admin123"))
        admin.put("name", "Администратор")
        admin.put("role", ROLE_ADMIN)
        db.insert(TABLE_USERS, null, admin)

        val hall1 = addHall(db, "Древний мир", 1, "Предметы Древнего Египта, Греции и Рима")
        val hall2 = addHall(db, "Живопись", 2, "Картины русских и европейских художников")
        val hall3 = addHall(db, "Скульптура", 1, "Скульптуры разных эпох")
        val hall4 = addHall(db, "Оружие и доспехи", 3, "Холодное оружие и рыцарские доспехи")

        addExhibit(db, hall1, "Саркофаг жреца", "Неизвестный мастер", "X век до н.э.",
            "Деревянный саркофаг с росписью, найден в Фивах.")
        addExhibit(db, hall1, "Греческая амфора", "Неизвестный мастер", "V век до н.э.",
            "Чернофигурная амфора для вина и масла.")
        addExhibit(db, hall1, "Римская монета", "Монетный двор Рима", "I век н.э.",
            "Серебряный денарий с профилем императора.")
        addExhibit(db, hall2, "Девятый вал", "И. Айвазовский", "1850",
            "Самая известная картина Айвазовского, шторм на море.")
        addExhibit(db, hall2, "Утро в сосновом лесу", "И. Шишкин, К. Савицкий", "1889",
            "Медведи в утреннем лесу.")
        addExhibit(db, hall2, "Звёздная ночь (копия)", "В. Ван Гог", "1889",
            "Учебная копия знаменитой картины.")
        addExhibit(db, hall3, "Дискобол (слепок)", "Мирон", "V век до н.э.",
            "Гипсовый слепок древнегреческой статуи.")
        addExhibit(db, hall3, "Мыслитель (копия)", "О. Роден", "1880",
            "Бронзовая копия скульптуры Родена.")
        addExhibit(db, hall4, "Рыцарский доспех", "Германские мастера", "XV век",
            "Полный латный доспех готического стиля.")
        addExhibit(db, hall4, "Японская катана", "Мастер Масамунэ (школа)", "XIV век",
            "Меч самурая с оригинальной рукоятью.")

        addTour(db, hall1, "Тайны Древнего Египта", "Обзорная экскурсия по залу Древнего мира", 500, 60)
        addTour(db, hall2, "Шедевры живописи", "Экскурсия по главным картинам музея", 700, 90)
        addTour(db, hall3, "Скульптура сквозь века", "От античности до XIX века", 450, 45)
        addTour(db, hall4, "Рыцари и самураи", "Сравниваем оружие Европы и Японии", 600, 60)
    }

    private fun addHall(db: SQLiteDatabase, name: String, floor: Int, desc: String): Long {
        val cv = ContentValues()
        cv.put("name", name)
        cv.put("floor", floor)
        cv.put("description", desc)
        return db.insert(TABLE_HALLS, null, cv)
    }

    private fun addExhibit(
        db: SQLiteDatabase, hallId: Long, title: String,
        author: String, year: String, desc: String
    ) {
        val cv = ContentValues()
        cv.put("hall_id", hallId)
        cv.put("title", title)
        cv.put("author", author)
        cv.put("year", year)
        cv.put("description", desc)
        db.insert(TABLE_EXHIBITS, null, cv)
    }

    private fun addTour(db: SQLiteDatabase, hallId: Long, title: String, desc: String, price: Int, duration: Int) {
        val cv = ContentValues()
        cv.put("hall_id", hallId)
        cv.put("title", title)
        cv.put("description", desc)
        cv.put("price", price)
        cv.put("duration", duration)
        db.insert(TABLE_TOURS, null, cv)
    }
}
