package com.llorente.tfg_gpsreminders.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.llorente.tfg_gpsreminders.R

data class LocationPredictionItem(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String?
)

class LocationPredictionAdapter(
    private var items: List<LocationPredictionItem>,
    private val onItemClicked: (LocationPredictionItem) -> Unit
) : RecyclerView.Adapter<LocationPredictionAdapter.LocationPredictionViewHolder>() {

    class LocationPredictionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewPredictionTitle: TextView =
            itemView.findViewById(R.id.textViewPredictionTitle)
        val textViewPredictionSubtitle: TextView =
            itemView.findViewById(R.id.textViewPredictionSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationPredictionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location_prediction, parent, false)

        return LocationPredictionViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationPredictionViewHolder, position: Int) {
        val item = items[position]

        holder.textViewPredictionTitle.text = item.primaryText

        if (item.secondaryText.isNullOrBlank()) {
            holder.textViewPredictionSubtitle.visibility = View.GONE
        } else {
            holder.textViewPredictionSubtitle.visibility = View.VISIBLE
            holder.textViewPredictionSubtitle.text = item.secondaryText
        }

        holder.itemView.setOnClickListener {
            onItemClicked(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<LocationPredictionItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}