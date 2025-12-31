package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ProfileImageAdapter(
    private val profileImages: List<String>,
    private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<ProfileImageAdapter.ProfileImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_image, parent, false)
        return ProfileImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfileImageViewHolder, position: Int) {
        val imageName = profileImages[position]
        val resId = holder.itemView.context.resources.getIdentifier(imageName, "drawable", holder.itemView.context.packageName)
        holder.imageView.setImageResource(resId)
        holder.itemView.setOnClickListener {
            onImageClick(imageName)
        }
    }

    override fun getItemCount() = profileImages.size

    class ProfileImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.profileImageView)
    }
}
