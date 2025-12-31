package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale



class PostAdapter(private val context: Context) :
    ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.postTitle)
        val contentTextView: TextView = itemView.findViewById(R.id.postContent)
        val authorTextView: TextView = itemView.findViewById(R.id.postAuthor)
        val timestampTextView: TextView = itemView.findViewById(R.id.postTimestamp)
        val metaTextView = itemView.findViewById<TextView>(R.id.postMeta)


        fun bind(post: Post) {
            titleTextView.text = post.title
            contentTextView.text = post.content
            authorTextView.text = " 작성자 : ${post.authorName}"
            timestampTextView.text = if (post.timestamp > 0L) {
                val dateFormat = SimpleDateFormat("작성일자 : yyyy-MM-dd", Locale.getDefault())
                dateFormat.format(post.timestamp)
            } else {
                "날짜 정보 없음"
            }
            metaTextView.text = "♥ ${post.likeCount} · 댓글 ${post.commentCount}"

            itemView.setOnClickListener {
                val readableDate = if (post.timestamp > 0L) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    dateFormat.format(post.timestamp)
                } else {
                    "날짜 정보 없음"
                }


                val intent = Intent(context, PostDetailActivity::class.java).apply {
                    putExtra("title", post.title)
                    putExtra("postId", post.id)
                    putExtra("content", post.content)
                    putExtra("latitude", post.latitude)
                    putExtra("longitude", post.longitude)
                    putExtra("profileName", post.authorName)
                    putExtra("profileImageName", post.profileImage)
                    putExtra("postDate", readableDate)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }
}