package com.museum.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.museum.app.R
import com.museum.app.data.Hall

// Список залов в админке
class HallAdapter(
    private var items: List<Hall>,
    private val onDelete: (Hall) -> Unit
) : RecyclerView.Adapter<HallAdapter.HallViewHolder>() {

    class HallViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvFloor: TextView = view.findViewById(R.id.tvFloor)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HallViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hall, parent, false)
        return HallViewHolder(view)
    }

    override fun onBindViewHolder(holder: HallViewHolder, position: Int) {
        val hall = items[position]
        holder.tvName.text = hall.name
        holder.tvFloor.text = holder.itemView.context.getString(R.string.format_place_floor, hall.placeName, hall.floor)
        holder.tvDescription.text = hall.description
        holder.btnDelete.setOnClickListener { onDelete(hall) }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<Hall>) {
        items = newItems
        notifyDataSetChanged()
    }
}
