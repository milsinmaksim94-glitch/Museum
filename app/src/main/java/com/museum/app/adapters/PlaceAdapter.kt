package com.museum.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.museum.app.R
import com.museum.app.data.Place
import com.museum.app.utils.ImageHelper

// Список площадок музея. onDelete передаём только в админке
class PlaceAdapter(
    private var items: List<Place>,
    private val onClick: (Place) -> Unit,
    private val onDelete: ((Place) -> Unit)? = null
) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        val tvMetro: TextView = view.findViewById(R.id.tvMetro)
        val tvHours: TextView = view.findViewById(R.id.tvHours)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = items[position]
        ImageHelper.show(holder.ivPhoto, place.cover, R.drawable.place_placeholder)
        holder.tvName.text = place.name
        holder.tvAddress.text = place.address
        holder.tvMetro.text = place.metro
        holder.tvHours.text = place.hours

        holder.itemView.setOnClickListener { onClick(place) }
        if (onDelete != null) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDelete.invoke(place) }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<Place>) {
        items = newItems
        notifyDataSetChanged()
    }
}
