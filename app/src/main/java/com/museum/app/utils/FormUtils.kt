package com.museum.app.utils

import android.view.ViewParent
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout

// Показ ошибок на полях ввода.
// Поля у нас в рамке TextInputLayout, если ставить error прямо на EditText то выглядит криво,
// поэтому ставим на рамку - тогда ошибка пишется красным под полем. Начал печатать - ошибка пропадает
object FormUtils {

    fun showError(editText: EditText, message: String) {
        val layout = findLayout(editText)
        if (layout == null) {
            editText.error = message
            return
        }
        layout.error = message
        // вешаем "очистку ошибки" только один раз на поле
        if (editText.getTag(com.museum.app.R.id.tag_error_watcher) == null) {
            editText.setTag(com.museum.app.R.id.tag_error_watcher, true)
            editText.doAfterTextChanged { layout.error = null }
        }
    }

    // ищем рамку TextInputLayout среди родителей поля
    private fun findLayout(editText: EditText): TextInputLayout? {
        var parent: ViewParent? = editText.parent
        while (parent != null) {
            if (parent is TextInputLayout) return parent
            parent = parent.parent
        }
        return null
    }
}
