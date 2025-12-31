package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

sealed class SafetyListItem {
    data class Header(val title: String) : SafetyListItem()
    data class Item(val data: SafetyItem) : SafetyListItem()
}

class MultiViewTypeAdapter(
    private val fullData: Map<String, List<SafetyItem>>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1

    private val displayList = mutableListOf<SafetyListItem>()
    private val expandedHeaders = mutableSetOf<String>()

    init {
        fullData.keys.forEach { title ->
            displayList.add(SafetyListItem.Header(title))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayList[position]) {
            is SafetyListItem.Header -> TYPE_HEADER
            is SafetyListItem.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_safety, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayList[position]) {
            is SafetyListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is SafetyListItem.Item -> (holder as ItemViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = displayList.size


    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.headerTitle)
        private val arrow: ImageView = view.findViewById(R.id.arrowIcon)

        fun bind(item: SafetyListItem.Header) {
            title.text = item.title
            arrow.setImageResource(
                if (expandedHeaders.contains(item.title)) R.drawable.baseline_down
                else R.drawable.baseline_right
            )

            itemView.setOnClickListener {
                val position = adapterPosition
                val titleText = item.title

                if (expandedHeaders.contains(titleText)) {
                    collapseItems(position, titleText)
                    arrow.setImageResource(R.drawable.baseline_right)
                } else {
                    expandItems(position, titleText)
                    arrow.setImageResource(R.drawable.baseline_down)
                }
            }
        }
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val content: TextView = view.findViewById(R.id.itemContent)
        fun bind(item: SafetyListItem.Item) {
            content.text = item.data.actRmks
        }
    }

    private fun expandItems(position: Int, title: String) {
        val children = fullData[title] ?: return
        val itemsToInsert = children.map { SafetyListItem.Item(it) }

        displayList.addAll(position + 1, itemsToInsert)
        notifyItemRangeInserted(position + 1, itemsToInsert.size)
        expandedHeaders.add(title)
    }

    private fun collapseItems(position: Int, title: String) {
        val count = fullData[title]?.size ?: 0
        displayList.subList(position + 1, position + 1 + count).clear()
        notifyItemRangeRemoved(position + 1, count)
        expandedHeaders.remove(title)
    }
}

