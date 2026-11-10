package com.museum.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.museum.app.R
import com.museum.app.data.Booking
import com.museum.app.utils.StatusHelper

// showUser = true в админке, чтобы видеть кто забронировал
class BookingAdapter(
    private var items: List<Booking>,
    private val showUser: Boolean,
    private val onClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTour: TextView = view.findViewById(R.id.tvTour)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val tvPeople: TextView = view.findViewById(R.id.tvPeople)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        val tvUser: TextView = view.findViewById(R.id.tvUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = items[position]
        val context = holder.itemView.context

        holder.tvTour.text = booking.tourTitle
        holder.tvDateTime.text = context.getString(R.string.format_date_time, booking.date, booking.time)

        // если взят аудиогид - дописываем это
        var people = context.getString(R.string.format_people, booking.adults, booking.children)
        if (booking.audioGuide) {
            people += context.getString(R.string.with_audio_guide)
        }
        holder.tvPeople.text = people
        holder.tvTotal.text = context.getString(R.string.format_price, booking.total)

        holder.tvStatus.text = StatusHelper.getStatusText(context, booking.status)
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, StatusHelper.getStatusColor(booking.status)))

        if (showUser) {
            holder.tvUser.visibility = View.VISIBLE
            holder.tvUser.text = context.getString(R.string.format_client, booking.userLogin)
        } else {
            holder.tvUser.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(booking) }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<Booking>) {
        items = newItems
        notifyDataSetChanged()
    }
}
