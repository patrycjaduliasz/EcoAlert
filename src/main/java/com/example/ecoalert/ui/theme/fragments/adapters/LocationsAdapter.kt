package com.example.ecoalert.ui.theme.fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecoalert.data.api.models.Location
import com.example.ecoalert.databinding.ItemLocationBinding

class LocationsAdapter(
    private val onLocationClick: (Location) -> Unit,
    private val onDeleteClick: (Location) -> Unit,
    private val onToggleAlert: (Location, Boolean) -> Unit
) : ListAdapter<Location, LocationsAdapter.LocationViewHolder>(LocationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemLocationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LocationViewHolder(
        private val binding: ItemLocationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(location: Location) {
            binding.tvLocationName.text = location.name
            binding.tvCoordinates.text = "Lat: ${location.latitude}, Lon: ${location.longitude}"
            binding.switchAlert.isChecked = location.alertEnabled

            binding.root.setOnClickListener {
                onLocationClick(location)
            }
            binding.btnDelete.setOnClickListener {
                onDeleteClick(location)
            }
            binding.switchAlert.setOnCheckedChangeListener { _, isChecked ->
                onToggleAlert(location, isChecked)
            }
        }
    }

    class LocationDiffCallback : DiffUtil.ItemCallback<Location>() {
        override fun areItemsTheSame(oldItem: Location, newItem: Location): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Location, newItem: Location): Boolean {
            return oldItem == newItem
        }
    }
}
