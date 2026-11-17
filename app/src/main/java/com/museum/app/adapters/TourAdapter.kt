package com.museum.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.museum.app.R
import com.museum.app.data.Tour

class TourAdapter(
    private var items: List<Tour>,
    private val onBookClick: (Tour) -> Unit
) : RecyclerView.Adapter<TourAdapter.TourViewHolder>() {

    class TourViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFormat: TextView = view.findViewById(R.id.tvFormat)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvGuide: TextView = view.findViewById(R.id.tvGuide)
        val tvInfo: TextView = view.findViewById(R.id.tvInfo)
        val tvSessions: TextView = view.findViewById(R.id.tvSessions)
        val btnBook: Button = view.findViewById(R.id.btnBook)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TourViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tour, parent, false)
        return TourViewHolder(view)
    }

    override fun onBindViewHolder(holder: TourViewHolder, position: Int) {
        val tour = items[position]
        val context = holder.itemView.context

        holder.tvFormat.text = tour.format
        holder.tvTitle.text = tour.title
        holder.tvPrice.text = context.getString(R.string.format_price, tour.price)
        holder.tvDescription.text = tour.description
        holder.tvGuide.text = context.getString(R.string.format_guide, tour.guideName)
        holder.tvInfo.text = context.getString(
            R.string.format_tour_info, tour.placeName, tour.hallName, tour.duration, tour.maxPeople
        )
        holder.tvSessions.text = context.getString(R.string.format_sessions, tour.sessionList().joinToString(", "))
        holder.btnBook.setOnClickListener { onBookClick(tour) }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<Tour>) {
        items = newItems
        notifyDataSetChanged()
    }
}
