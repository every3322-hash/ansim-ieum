package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoriteAdapter(
    private val favoriteList: MutableList<FavoriteItem>,
    private val onDeleteClick: (FavoriteItem) -> Unit,
    private val onItemClick: (FavoriteItem) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder>() {

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val category: TextView = itemView.findViewById(R.id.categoryTextView)
        val deleteBtn: Button = itemView.findViewById(R.id.deleteButton)
        val click: TextView = itemView.findViewById(R.id.categoryTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val item = favoriteList[position]
        holder.category.text = "${item.title}"  // 제목 표시

        holder.deleteBtn.setOnClickListener {
            onDeleteClick(item)
        }
        holder.click.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = favoriteList.size
}

