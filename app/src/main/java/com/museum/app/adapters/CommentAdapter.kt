package com.museum.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.museum.app.R
import com.museum.app.data.Comment

// Адаптер для отзывов на экране экспоната.
// Долгое нажатие - удалить (проверка прав в самой активити)
class CommentAdapter(
    private var items: List<Comment>,
    private val onLongClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUser: TextView = view.findViewById(R.id.tvUser)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val tvText: TextView = view.findViewById(R.id.tvText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = items[position]
        holder.tvUser.text = comment.userName
        holder.tvDate.text = comment.date
        holder.ratingBar.rating = comment.rating.toFloat()
        holder.tvText.text = comment.text

        holder.itemView.setOnLongClickListener {
            onLongClick(comment)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<Comment>) {
        items = newItems
        notifyDataSetChanged()
    }
}
