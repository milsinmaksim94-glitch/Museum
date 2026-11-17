package com.museum.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.museum.app.R
import java.io.ByteArrayOutputStream

// Всё про картинки.
// Картинка в базе хранится строкой:
//  "res:photo_repin" - фото, которое лежит в самом приложении (res/drawable-nodpi)
//  длинная строка base64 - фото, которое админ выбрал из галереи телефона
//  пустая строка - фото нет, показываем заглушку
object ImageHelper {

    private const val MAX_SIZE = 800 // больше 800 пикселей не храним, чтобы база не раздувалась

    fun show(imageView: ImageView, image: String, placeholder: Int) {
        if (image.isEmpty()) {
            showPlaceholder(imageView, placeholder)
            return
        }
        if (image.startsWith("res:")) {
            val name = image.substring(4)
            val context = imageView.context
            val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (resId != 0) {
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setImageResource(resId)
            } else {
                showPlaceholder(imageView, placeholder)
            }
            return
        }
        // иначе это base64
        try {
            val bytes = Base64.decode(image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setImageBitmap(bitmap)
            } else {
                showPlaceholder(imageView, placeholder)
            }
        } catch (e: Exception) {
            showPlaceholder(imageView, placeholder)
        }
    }

    // заглушка: значок по центру на спокойном фоне, без растягивания
    private fun showPlaceholder(imageView: ImageView, placeholder: Int) {
        imageView.scaleType = ImageView.ScaleType.CENTER
        imageView.setBackgroundColor(ContextCompat.getColor(imageView.context, R.color.placeholder_bg))
        imageView.setImageResource(placeholder)
    }

    // Фото из галереи -> уменьшаем -> сжимаем в JPEG -> строка base64.
    // Вызывать в фоновом потоке, это небыстро
    fun uriToBase64(context: Context, uri: Uri): String? {
        try {
            // сначала узнаём размер картинки, не загружая её целиком
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }

            // считаем, во сколько раз уменьшить
            var sample = 1
            while (options.outWidth / sample > MAX_SIZE * 2 || options.outHeight / sample > MAX_SIZE * 2) {
                sample *= 2
            }
            val options2 = BitmapFactory.Options()
            options2.inSampleSize = sample
            val bitmap = context.contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it, null, options2)
            } ?: return null

            // доводим до MAX_SIZE с сохранением пропорций
            val scale = minOf(MAX_SIZE.toFloat() / bitmap.width, MAX_SIZE.toFloat() / bitmap.height, 1f)
            val scaled = Bitmap.createScaledBitmap(
                bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true
            )

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            return null
        }
    }

    // инициалы для аватарки экскурсовода без фото: "Анна Соколова" -> "АС"
    fun initials(name: String): String {
        val sb = StringBuilder()
        for (part in name.trim().split(" ")) {
            if (part.isNotEmpty() && sb.length < 2) sb.append(part[0].uppercaseChar())
        }
        return sb.toString()
    }
}
