package com.museum.app.utils

import android.content.Context
import com.museum.app.R
import com.museum.app.data.DBHelper

// В базе статус хранится кодом (new, confirmed...), а пользователю показываем текст
object StatusHelper {

    // список статусов в том порядке, как они показываются в диалоге у админа
    val allStatuses = arrayOf(
        DBHelper.STATUS_NEW,
        DBHelper.STATUS_CONFIRMED,
        DBHelper.STATUS_CANCELLED,
        DBHelper.STATUS_DONE
    )

    fun getStatusText(context: Context, status: String): String {
        return when (status) {
            DBHelper.STATUS_NEW -> context.getString(R.string.status_new)
            DBHelper.STATUS_CONFIRMED -> context.getString(R.string.status_confirmed)
            DBHelper.STATUS_CANCELLED -> context.getString(R.string.status_cancelled)
            DBHelper.STATUS_DONE -> context.getString(R.string.status_done)
            else -> status
        }
    }

    fun getStatusColor(status: String): Int {
        return when (status) {
            DBHelper.STATUS_NEW -> R.color.status_new
            DBHelper.STATUS_CONFIRMED -> R.color.status_confirmed
            DBHelper.STATUS_CANCELLED -> R.color.status_cancelled
            else -> R.color.status_done
        }
    }
}
