package com.museum.app.utils

import kotlin.random.Random

// Генерация id для новых записей.
// AUTOINCREMENT не подошёл: записи создаются на разных телефонах и id могут совпасть.
// Поэтому берём время в мс и добавляем рандом, вряд ли совпадёт
object IdUtils {
    fun newId(): Long {
        return System.currentTimeMillis() * 1000 + Random.nextInt(1000)
    }
}
