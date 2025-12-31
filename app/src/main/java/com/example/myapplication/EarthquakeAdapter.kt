package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class EarthquakeAdapter(private val earthquakeList: List<EarthquakeModel>) :
    RecyclerView.Adapter<EarthquakeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val location: TextView = view.findViewById(R.id.location)
        val magnitude: TextView = view.findViewById(R.id.magnitude)
        val depth: TextView = view.findViewById(R.id.depth)
        val time: TextView = view.findViewById(R.id.time)
        val image: ImageView = view.findViewById(R.id.image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.earthquake_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val earthquake = earthquakeList[position]
        holder.location.text = earthquake.location
        holder.magnitude.text = "규모: ${earthquake.magnitude}"
        holder.depth.text = "깊이: ${earthquake.depth}km"
        holder.time.text = "발생 시각: ${earthquake.time}"

        Glide.with(holder.itemView.context)
            .load(earthquake.imageUrl)
            .into(holder.image)
    }

    override fun getItemCount() = earthquakeList.size
}
