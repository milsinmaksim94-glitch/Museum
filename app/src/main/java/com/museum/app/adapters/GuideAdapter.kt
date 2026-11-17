package com.museum.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.museum.app.R
import com.museum.app.data.Guide
import com.museum.app.utils.ImageHelper

// Список экскурсоводов. onDelete передаём только в админке
class GuideAdapter(
    private var items: List<Guide>,
    private val onClick: (Guide) -> Unit,
    private val onDelete: ((Guide) -> Unit)? = null
) : RecyclerView.Adapter<GuideAdapter.GuideViewHolder>() {

    class GuideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
        val tvInitials: TextView = view.findViewById(R.id.tvInitials)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPosition: TextView = view.findViewById(R.id.tvPosition)
        val tvExperience: TextView = view.findViewById(R.id.tvExperience)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_guide, parent, false)
        return GuideViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuideViewHolder, position: Int) {
        val guide = items[position]
        val context = holder.itemView.context

        // если фото нет - показываем кружок с инициалами
        if (guide.image.isEmpty()) {
            holder.ivPhoto.visibility = View.INVISIBLE
            holder.tvInitials.visibility = View.VISIBLE
            holder.tvInitials.text = ImageHelper.initials(guide.name)
        } else {
            holder.ivPhoto.visibility = View.VISIBLE
            holder.tvInitials.visibility = View.GONE
            ImageHelper.show(holder.ivPhoto, guide.image, R.drawable.ic_person)
        }

        holder.tvName.text = guide.name
        holder.tvPosition.text = guide.position
        holder.tvExperience.text = context.getString(R.string.format_experience, guide.experience, guide.languages)

        holder.itemView.setOnClickListener { onClick(guide) }
        if (onDelete != null) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDelete.invoke(guide) }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<Guide>) {
        items = newItems
        notifyDataSetChanged()
    }
}
