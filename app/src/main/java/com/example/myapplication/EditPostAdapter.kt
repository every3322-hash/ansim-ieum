package com.example.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemEditPostBinding

class EditPostAdapter(
    private var postList: List<Post>,
    private val listener: OnPostActionListener
) : RecyclerView.Adapter<EditPostAdapter.PostViewHolder>() {

    interface OnPostActionListener {
        fun onEdit(post: Post)
        fun onDelete(post: Post)
    }

    inner class PostViewHolder(val binding: ItemEditPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            binding.titleText.text = post.title
            binding.contentText.text = post.content

            // 수정 버튼 클릭
            binding.btnEdit.setOnClickListener {
                listener.onEdit(post)
            }

            // 삭제 버튼 클릭
            binding.btnDelete.setOnClickListener {
                listener.onDelete(post)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemEditPostBinding.inflate(inflater, parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(postList[position])
    }

    override fun getItemCount(): Int = postList.size



    //  리스트를 갱신할 수 있도록 추가
    fun submitList(newList: List<Post>) {
        postList = newList
        notifyDataSetChanged()
    }
}
