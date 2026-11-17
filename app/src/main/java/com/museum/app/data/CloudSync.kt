package com.museum.app.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

// Синхронизация с Firebase (общая база для всех телефонов).
// Читаем всегда из SQLite, а когда что-то меняем - пишем в SQLite и сразу кидаем в Firebase.
// При запуске и по кнопке "Обновить" качаем всё из Firebase и перезаписываем SQLite,
// так на другом телефоне тоже видно что добавили.
// В Firebase коллекции называются как таблицы, одна строка = один документ
object CloudSync {

    private const val TAG = "CloudSync"
    private const val TIMEOUT_MS = 20000L

    private fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // id документа в Firebase. У избранного нет своего id, поэтому склеиваем два
    private fun docId(table: String, row: Map<String, Any?>): String {
        return if (table == DBHelper.TABLE_FAVORITES) {
            "${row["user_id"]}_${row["exhibit_id"]}"
        } else {
            row["id"].toString()
        }
    }

    // строку из курсора превращаем в Map (колонка -> значение)
    private fun cursorToMap(cursor: Cursor): HashMap<String, Any?> {
        val map = HashMap<String, Any?>()
        for (i in 0 until cursor.columnCount) {
            val name = cursor.getColumnName(i)
            when (cursor.getType(i)) {
                Cursor.FIELD_TYPE_INTEGER -> map[name] = cursor.getLong(i)
                Cursor.FIELD_TYPE_FLOAT -> map[name] = cursor.getDouble(i)
                Cursor.FIELD_TYPE_STRING -> map[name] = cursor.getString(i)
                else -> map[name] = null
            }
        }
        return map
    }

    // и наоборот: документ из Firebase -> ContentValues для вставки в SQLite
    private fun mapToValues(map: Map<String, Any?>): ContentValues {
        val cv = ContentValues()
        for ((key, value) in map) {
            when (value) {
                null -> cv.putNull(key)
                is Long -> cv.put(key, value)
                is Int -> cv.put(key, value)
                is Double -> cv.put(key, value)
                is Boolean -> cv.put(key, if (value) 1 else 0)
                else -> cv.put(key, value.toString())
            }
        }
        return cv
    }

    // отправка изменений

    // Отправить в Firebase одну строку таблицы (после добавления или изменения).
    // Не ждём ответа: если интернета нет, Firebase сам отправит позже
    fun push(db: SQLiteDatabase, table: String, id: Long) {
        val cursor = db.rawQuery("SELECT * FROM $table WHERE id = ?", arrayOf(id.toString()))
        if (cursor.moveToFirst()) {
            val row = cursorToMap(cursor)
            firestore().collection(table).document(docId(table, row)).set(row)
                .addOnFailureListener { e -> Log.w(TAG, "push $table/$id failed", e) }
        }
        cursor.close()
    }

    fun pushFavorite(userId: Long, exhibitId: Long) {
        val row = HashMap<String, Any?>()
        row["user_id"] = userId
        row["exhibit_id"] = exhibitId
        firestore().collection(DBHelper.TABLE_FAVORITES).document("${userId}_$exhibitId").set(row)
    }

    fun pushDelete(table: String, docId: String) {
        firestore().collection(table).document(docId).delete()
            .addOnFailureListener { e -> Log.w(TAG, "delete $table/$docId failed", e) }
    }

    // скачивание

    // Скачать все таблицы из Firebase и заменить ими локальную базу.
    // Возвращает true если получилось, false если нет интернета или ошибка (тогда локальные данные не трогаем)
    suspend fun downloadAll(dbHelper: DBHelper): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(TIMEOUT_MS) {
                val db = dbHelper.writableDatabase

                // сначала ждём, пока наши последние изменения дойдут до сервера,
                // иначе скачаем старые данные и затрём только что добавленное
                firestore().waitForPendingWrites().await()

                // если в облаке ещё пусто (самый первый запуск) - загружаем туда стартовые данные
                uploadStartDataIfNeeded(db)

                // скачиваем всё. Source.SERVER - именно с сервера, а не из кеша,
                // иначе без интернета можно получить пустые списки и стереть локальную базу
                val cloudData = HashMap<String, List<Map<String, Any?>>>()
                for (table in DBHelper.ALL_TABLES) {
                    val snapshot = firestore().collection(table).get(Source.SERVER).await()
                    val rows = ArrayList<Map<String, Any?>>()
                    for (doc in snapshot.documents) {
                        val data = doc.data
                        if (data != null) rows.add(data)
                    }
                    cloudData[table] = rows
                }

                // перезаписываем локальную базу одной транзакцией: либо всё, либо ничего
                db.beginTransaction()
                try {
                    // удаляем в обратном порядке (сначала "дети", потом "родители")
                    for (table in DBHelper.ALL_TABLES.reversed()) {
                        db.delete(table, null, null)
                    }
                    // вставляем в прямом порядке. Если строка ссылается на удалённую запись,
                    // insert вернёт -1 из-за FOREIGN KEY и строка просто пропустится
                    for (table in DBHelper.ALL_TABLES) {
                        for (row in cloudData[table]!!) {
                            db.insert(table, null, mapToValues(row))
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "downloadAll failed", e)
            false
        }
    }

    // в коллекции meta лежит отметка, что стартовые данные уже загружены
    private suspend fun uploadStartDataIfNeeded(db: SQLiteDatabase) {
        val metaDoc = firestore().collection("meta").document("start_data")
        val snapshot = metaDoc.get(Source.SERVER).await()
        if (snapshot.exists()) return

        Log.i(TAG, "облако пустое, загружаем стартовые данные")
        val batch = firestore().batch()
        for (table in DBHelper.ALL_TABLES) {
            val cursor = db.rawQuery("SELECT * FROM $table", null)
            while (cursor.moveToNext()) {
                val row = cursorToMap(cursor)
                batch.set(firestore().collection(table).document(docId(table, row)), row)
            }
            cursor.close()
        }
        val mark = HashMap<String, Any>()
        mark["uploaded"] = System.currentTimeMillis()
        batch.set(metaDoc, mark)
        batch.commit().await()
    }
}
