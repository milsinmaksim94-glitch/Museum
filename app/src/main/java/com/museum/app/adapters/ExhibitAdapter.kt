package com.museum.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.museum.app.R
import com.museum.app.data.Exhibit

// Адаптер для списка экспонатов.
// onDelete передаём только в админке, тогда появляется кнопка удаления
class ExhibitAdapter(
    private var items: List<Exhibit>,
    private val onClick: (Exhibit) -> Unit,
    private val onDelete: ((Exhibit) -> Unit)? = null
) : RecyclerView.Adapter<ExhibitAdapter.ExhibitViewHolder>() {

    class ExhibitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val tvHall: TextView = view.findViewById(R.id.tvHall)
        val tvRating: TextView = view.findViewById(R.id.tvRating)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExhibitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exhibit, parent, false)
        return ExhibitViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExhibitViewHolder, position: Int) {
        val exhibit = items[position]
        val context = holder.itemView.context

        holder.tvTitle.text = exhibit.title
        holder.tvAuthor.text = context.getString(R.string.format_author_year, exhibit.author, exhibit.year)
        holder.tvHall.text = context.getString(R.string.format_hall, exhibit.hallName)

        // если оценок ещё нет, то так и пишем
        if (exhibit.rating > 0) {
            holder.tvRating.text = context.getString(R.string.format_rating, exhibit.rating)
        } else {
            holder.tvRating.text = context.getString(R.string.no_rating)
        }

        holder.itemView.setOnClickListener { onClick(exhibit) }

        if (onDelete != null) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDelete.invoke(exhibit) }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<Exhibit>) {
        items = newItems
        notifyDataSetChanged()
    }
}
