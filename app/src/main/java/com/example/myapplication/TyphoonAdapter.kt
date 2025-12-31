package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TyphoonAdapter(private val list: List<TyphoonModel>) :
    RecyclerView.Adapter<TyphoonAdapter.ViewHolder>() {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.name)
        val time = view.findViewById<TextView>(R.id.time)
        val location = view.findViewById<TextView>(R.id.location)
        val pressure = view.findViewById<TextView>(R.id.pressure)
        val windSpeed = view.findViewById<TextView>(R.id.windSpeed)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_typhoon, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.name.text = "태풍 이름: ${item.name}"
        holder.time.text = "시간: ${item.time}"
        holder.location.text = "위치: ${item.location}"
        holder.pressure.text = "기압: ${item.pressure} hPa"
        holder.windSpeed.text = "풍속: ${item.windSpeed} m/s"
    }
}