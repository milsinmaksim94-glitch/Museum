package com.museum.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Класс для создания и обновления локальной базы данных SQLite.
// Сами запросы (выборки, добавление и т.д.) лежат в MuseumRepository.
// Общая база для всех телефонов - в Firebase, а SQLite это локальная копия (см. CloudSync)
class DBHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "museum.db"
        const val DB_VERSION = 2 // версия 2: площадки, экскурсоводы, фото, сеансы

        // названия таблиц
        const val TABLE_USERS = "users"
        const val TABLE_PLACES = "places"
        const val TABLE_HALLS = "halls"
        const val TABLE_EXHIBITS = "exhibits"
        const val TABLE_COMMENTS = "comments"
        const val TABLE_FAVORITES = "favorites"
        const val TABLE_GUIDES = "guides"
        const val TABLE_TOURS = "tours"
        const val TABLE_BOOKINGS = "bookings"

        // порядок важен: сначала "родители", потом те, кто на них ссылается (FOREIGN KEY)
        val ALL_TABLES = arrayOf(
            TABLE_USERS, TABLE_PLACES, TABLE_HALLS, TABLE_GUIDES, TABLE_EXHIBITS,
            TABLE_TOURS, TABLE_COMMENTS, TABLE_FAVORITES, TABLE_BOOKINGS
        )

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
        // id у всех таблиц задаём сами (а не AUTOINCREMENT), потому что записи
        // создаются на разных телефонах и потом сливаются в общей базе Firebase
        db.execSQL(
            "CREATE TABLE $TABLE_USERS (" +
                    "id INTEGER PRIMARY KEY, " +
                    "login TEXT NOT NULL UNIQUE, " +
                    "password TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "role TEXT NOT NULL DEFAULT '$ROLE_USER')"
        )

        db.execSQL(
            "CREATE TABLE $TABLE_PLACES (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "address TEXT NOT NULL, " +
                    "metro TEXT, " +
                    "hours TEXT, " +
                    "phone TEXT, " +
                    "description TEXT, " +
                    "latitude REAL NOT NULL DEFAULT 0, " +
                    "longitude REAL NOT NULL DEFAULT 0, " +
                    "image TEXT)"
        )

        db.execSQL(
            "CREATE TABLE $TABLE_HALLS (" +
                    "id INTEGER PRIMARY KEY, " +
                    "place_id INTEGER NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "floor INTEGER NOT NULL DEFAULT 1, " +
                    "description TEXT, " +
                    "FOREIGN KEY (place_id) REFERENCES $TABLE_PLACES(id) ON DELETE CASCADE)"
        )

        // если удалить зал - удалятся и его экспонаты (CASCADE)
        db.execSQL(
            "CREATE TABLE $TABLE_EXHIBITS (" +
                    "id INTEGER PRIMARY KEY, " +
                    "hall_id INTEGER NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "author TEXT, " +
                    "year TEXT, " +
                    "description TEXT, " +
                    "inv_number TEXT, " +
                    "material TEXT, " +
                    "image TEXT, " +
                    "FOREIGN KEY (hall_id) REFERENCES $TABLE_HALLS(id) ON DELETE CASCADE)"
        )

        db.execSQL(
            "CREATE TABLE $TABLE_COMMENTS (" +
                    "id INTEGER PRIMARY KEY, " +
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
            "CREATE TABLE $TABLE_GUIDES (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "position TEXT, " +
                    "experience INTEGER NOT NULL DEFAULT 0, " +
                    "languages TEXT, " +
                    "bio TEXT, " +
                    "image TEXT)"
        )

        // экскурсия проходит в зале (а зал в площадке) и её ведёт экскурсовод
        db.execSQL(
            "CREATE TABLE $TABLE_TOURS (" +
                    "id INTEGER PRIMARY KEY, " +
                    "hall_id INTEGER NOT NULL, " +
                    "guide_id INTEGER NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT, " +
                    "format TEXT, " +
                    "price INTEGER NOT NULL, " +
                    "duration INTEGER NOT NULL, " +
                    "max_people INTEGER NOT NULL DEFAULT 15, " +
                    "sessions TEXT NOT NULL, " +
                    "FOREIGN KEY (hall_id) REFERENCES $TABLE_HALLS(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (guide_id) REFERENCES $TABLE_GUIDES(id) ON DELETE CASCADE)"
        )

        db.execSQL(
            "CREATE TABLE $TABLE_BOOKINGS (" +
                    "id INTEGER PRIMARY KEY, " +
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
        // при смене версии просто пересоздаём всё заново.
        // данные не пропадут насовсем - они лежат в Firebase и скачаются при синхронизации
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKINGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TOURS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GUIDES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXHIBITS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HALLS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLACES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // Начальные данные. У них маленькие id (1, 2, 3...), поэтому на всех телефонах
    // они одинаковые и в Firebase не задвоятся. Новые записи получают большие id (см. IdUtils)
    private fun fillTestData(db: SQLiteDatabase) {
        // админ по умолчанию: логин admin, пароль admin123
        val admin = ContentValues()
        admin.put("id", 1)
        admin.put("login", "admin")
        admin.put("password", PasswordUtils.hash("admin123"))
        admin.put("name", "Администратор")
        admin.put("role", ROLE_ADMIN)
        db.insert(TABLE_USERS, null, admin)

        // площадки
        addPlace(
            db, 1, "Главное здание", "Санкт-Петербург, Музейная ул., 1",
            "Владимирская - 7 минут пешком", "Ежедневно 11:00-19:30, вход по записи",
            "+7 (812) 000-00-01",
            "Особняк начала XX века. Здесь залы Древнего мира, европейского искусства и скульптуры, " +
                    "касса и гардероб.",
            59.9275, 30.3475
        )
        addPlace(
            db, 2, "Дом-мастерская художника", "Санкт-Петербург, Художественный пер., 5",
            "Достоевская - 10 минут пешком", "Ср-Вс 12:00-20:00",
            "+7 (812) 000-00-02",
            "Бывшая квартира-мастерская. Два этажа русской живописи XIX века: реализм, пейзаж, " +
                    "былинные сюжеты.",
            59.9286, 30.3431
        )
        addPlace(
            db, 3, "Выставочный зал «Набережная»", "Санкт-Петербург, наб. Музейного канала, 12",
            "Гостиный двор - 12 минут пешком", "Вт-Вс 10:00-18:00",
            "+7 (812) 000-00-03",
            "Зал временных выставок. Сейчас здесь экспозиция «Рыцари и самураи».",
            59.9330, 30.3330
        )

        // залы
        addHall(db, 1, 1, "Древний мир", 1, "Египет, Греция и Рим: саркофаги, амфоры, монеты")
        addHall(db, 2, 2, "Русская живопись", 1, "Реализм и былинные сюжеты XIX века")
        addHall(db, 3, 1, "Европейское искусство", 2, "Живопись Европы конца XIX века")
        addHall(db, 4, 1, "Скульптура", 1, "Слепки и копии скульптур разных эпох")
        addHall(db, 5, 3, "Оружие и доспехи", 1, "Европейские доспехи и японские мечи")
        addHall(db, 6, 2, "Пейзаж", 2, "Русский пейзаж: свет, вода, осень")

        // экспонаты
        addExhibit(
            db, 1, 1, "Саркофаг жреца Иотефамуна", "Мастера Древнего Египта", "I тыс. до н.э.",
            "Внешний деревянный саркофаг жреца с росписью: богини-покровительницы, " +
                    "иероглифические молитвы и сцены суда Осириса.",
            "ЭК-001", "Дерево, левкас, роспись (копия)", "res:photo_coffin"
        )
        addExhibit(
            db, 2, 1, "Панафинейская амфора", "Мастерская Афин", "VI век до н.э.",
            "Такие амфоры с оливковым маслом вручали победителям Панафинейских игр. " +
                    "На одной стороне - богиня Афина, на другой - состязание атлетов.",
            "ЭК-002", "Терракота, чернофигурная роспись (копия)", "res:photo_amphora"
        )
        addExhibit(
            db, 3, 1, "Денарий Веспасиана", "Монетный двор Рима", "69-79 гг. н.э.",
            "Серебряная монета с профилем императора Веспасиана - того самого, " +
                    "при котором начали строить Колизей.",
            "ЭК-003", "Серебро", "res:photo_denarius"
        )
        addExhibit(
            db, 4, 2, "Девятый вал", "И. К. Айвазовский", "1850",
            "Самая известная картина мариниста: моряки после ночного шторма держатся за обломок мачты, " +
                    "а сквозь волны пробивается солнце.",
            "ЭК-004", "Холст, масло (музейная копия)", "res:photo_aivazovsky"
        )
        addExhibit(
            db, 5, 2, "Утро в сосновом лесу", "И. И. Шишкин, К. А. Савицкий", "1889",
            "Лес написал Шишкин, а медведей - Савицкий. Его подпись потом стёр Третьяков, " +
                    "и картину долго считали работой одного Шишкина.",
            "ЭК-005", "Холст, масло (музейная копия)", "res:photo_shishkin"
        )
        addExhibit(
            db, 6, 2, "Бурлаки на Волге", "И. Е. Репин", "1870-1873",
            "Одиннадцать бурлаков тянут баржу против течения. Репин несколько лет ездил на Волгу " +
                    "и писал этюды с настоящих бурлаков.",
            "ЭК-006", "Холст, масло (музейная копия)", "res:photo_repin"
        )
        addExhibit(
            db, 7, 2, "Богатыри", "В. М. Васнецов", "1881-1898",
            "Илья Муромец, Добрыня Никитич и Алёша Попович на заставе. " +
                    "Васнецов работал над картиной почти двадцать лет.",
            "ЭК-007", "Холст, масло (музейная копия)", "res:photo_vasnetsov"
        )
        addExhibit(
            db, 8, 6, "Лунная ночь на Днепре", "А. И. Куинджи", "1880",
            "Когда картину впервые показали, зрители заглядывали за холст - искали спрятанную лампу. " +
                    "Так светится луна.",
            "ЭК-008", "Холст, масло (музейная копия)", "res:photo_kuindzhi"
        )
        addExhibit(
            db, 9, 6, "Золотая осень", "И. И. Левитан", "1895",
            "Берёзовая роща и речка в солнечный осенний день - одна из самых ярких картин Левитана.",
            "ЭК-009", "Холст, масло (музейная копия)", "res:photo_levitan"
        )
        addExhibit(
            db, 10, 3, "Звёздная ночь", "В. ван Гог", "1889",
            "Вид из окна лечебницы в Сен-Реми: кипарис, деревня и закрученное в вихри ночное небо.",
            "ЭК-010", "Холст, масло (учебная копия)", "res:photo_vangogh"
        )
        addExhibit(
            db, 11, 4, "Дискобол", "Мирон (римская копия)", "V век до н.э.",
            "Атлет за мгновение до броска диска. Греческий бронзовый оригинал утерян, " +
                    "известен по римским мраморным копиям.",
            "ЭК-011", "Гипс (слепок)", "res:photo_discobolus"
        )
        addExhibit(
            db, 12, 4, "Мыслитель", "О. Роден", "1880",
            "Задумывался как часть «Врат ада» - фигура Данте над вратами. " +
                    "Потом стал отдельной и самой известной скульптурой Родена.",
            "ЭК-012", "Бронза (копия)", "res:photo_thinker"
        )
        addExhibit(
            db, 13, 5, "Рыцарский доспех", "Мастера Германии", "XVI век",
            "Полный латный доспех весом около 25 кг. В нём можно было бегать, " +
                    "садиться на коня и даже подниматься по лестнице.",
            "ЭК-013", "Сталь, кожа", "res:photo_armor"
        )
        addExhibit(
            db, 14, 5, "Японский меч тати", "Японские оружейники", "XIV-XVI века",
            "Длинный меч, который носили подвешенным к поясу лезвием вниз. " +
                    "Предок более известной катаны.",
            "ЭК-014", "Сталь, дерево, лак, шёлк", "res:photo_tachi"
        )

        // экскурсоводы
        addGuide(
            db, 1, "Анна Соколова", "Старший научный сотрудник", 12, "русский, английский",
            "Искусствовед, специалист по русской живописи XIX века. Ведёт экскурсии по " +
                    "Дому-мастерской и рассказывает истории картин, которых нет в учебниках."
        )
        addGuide(
            db, 2, "Михаил Орлов", "Историк-археолог", 8, "русский, немецкий",
            "Участвовал в раскопках в Крыму и Египте. Отвечает за зал Древнего мира."
        )
        addGuide(
            db, 3, "Екатерина Белова", "Искусствовед", 6, "русский, французский",
            "Занимается европейским искусством и скульптурой. Ведёт обзорную экскурсию."
        )
        addGuide(
            db, 4, "Дмитрий Волков", "Хранитель коллекции оружия", 10, "русский, английский",
            "Историк-реконструктор, сам надевал полный доспех. Знает о мечах и латах всё."
        )
        addGuide(
            db, 5, "Ольга Лебедева", "Методист детских программ", 5, "русский",
            "Придумывает квесты и интерактивные занятия для детей от 6 до 12 лет."
        )

        // экскурсии
        addTour(
            db, 1, 3, 3, "Главное за час", "Обзорная экскурсия по главному зданию: от Египта до Ван Гога",
            "Обзорная экскурсия", 600, 60, 15, "11:00,13:00,16:30"
        )
        addTour(
            db, 2, 1, 2, "Тайны Древнего Египта", "Как жили, верили и хоронили фараонов и жрецов",
            "Тематическая экскурсия", 500, 60, 15, "11:30,15:00"
        )
        addTour(
            db, 3, 2, 1, "Шедевры русской живописи", "Айвазовский, Шишкин, Репин и Васнецов: истории картин",
            "Тематическая экскурсия", 700, 90, 20, "12:00,15:00,18:00"
        )
        addTour(
            db, 4, 6, 1, "Лунный свет Куинджи", "Вечерняя экскурсия-спектакль в затемнённом зале пейзажа",
            "Экскурсия-спектакль", 1200, 75, 12, "19:00"
        )
        addTour(
            db, 5, 5, 4, "Рыцари и самураи", "Сравниваем оружие и доспехи Европы и Японии, можно примерить шлем",
            "Интерактивная экскурсия", 600, 60, 15, "13:00,17:00"
        )
        addTour(
            db, 6, 1, 5, "Тайна пропавшей амфоры", "Квест для детей 6-12 лет: ищем подсказки по залу Древнего мира",
            "Квест для детей", 450, 45, 10, "11:00,14:00"
        )
        addTour(
            db, 7, 4, 3, "Скульптура сквозь века", "От античного Дискобола до Мыслителя Родена",
            "Тематическая экскурсия", 450, 45, 15, "14:00"
        )
    }

    private fun addPlace(
        db: SQLiteDatabase, id: Long, name: String, address: String, metro: String,
        hours: String, phone: String, desc: String, lat: Double, lon: Double
    ) {
        val cv = ContentValues()
        cv.put("id", id)
        cv.put("name", name)
        cv.put("address", address)
        cv.put("metro", metro)
        cv.put("hours", hours)
        cv.put("phone", phone)
        cv.put("description", desc)
        cv.put("latitude", lat)
        cv.put("longitude", lon)
        cv.put("image", "")
        db.insert(TABLE_PLACES, null, cv)
    }

    private fun addHall(db: SQLiteDatabase, id: Long, placeId: Long, name: String, floor: Int, desc: String) {
        val cv = ContentValues()
        cv.put("id", id)
        cv.put("place_id", placeId)
        cv.put("name", name)
        cv.put("floor", floor)
        cv.put("description", desc)
        db.insert(TABLE_HALLS, null, cv)
    }

    private fun addExhibit(
        db: SQLiteDatabase, id: Long, hallId: Long, title: String, author: String,
        year: String, desc: String, inv: String, material: String, image: String
    ) {
        val cv = ContentValues()
        cv.put("id", id)
        cv.put("hall_id", hallId)
        cv.put("title", title)
        cv.put("author", author)
        cv.put("year", year)
        cv.put("description", desc)
        cv.put("inv_number", inv)
        cv.put("material", material)
        cv.put("image", image)
        db.insert(TABLE_EXHIBITS, null, cv)
    }

    private fun addGuide(
        db: SQLiteDatabase, id: Long, name: String, position: String,
        experience: Int, languages: String, bio: String
    ) {
        val cv = ContentValues()
        cv.put("id", id)
        cv.put("name", name)
        cv.put("position", position)
        cv.put("experience", experience)
        cv.put("languages", languages)
        cv.put("bio", bio)
        cv.put("image", "")
        db.insert(TABLE_GUIDES, null, cv)
    }

    private fun addTour(
        db: SQLiteDatabase, id: Long, hallId: Long, guideId: Long, title: String, desc: String,
        format: String, price: Int, duration: Int, maxPeople: Int, sessions: String
    ) {
        val cv = ContentValues()
        cv.put("id", id)
        cv.put("hall_id", hallId)
        cv.put("guide_id", guideId)
        cv.put("title", title)
        cv.put("description", desc)
        cv.put("format", format)
        cv.put("price", price)
        cv.put("duration", duration)
        cv.put("max_people", maxPeople)
        cv.put("sessions", sessions)
        db.insert(TABLE_TOURS, null, cv)
    }
}
