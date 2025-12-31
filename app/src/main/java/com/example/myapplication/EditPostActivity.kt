package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityEditPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditPostActivity : AppCompatActivity(), EditPostAdapter.OnPostActionListener {

    private lateinit var binding: ActivityEditPostBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: EditPostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }


        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        adapter = EditPostAdapter(emptyList(), this)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadUserPosts()
    }

    private fun loadUserPosts() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        firestore.collection("posts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val postList = result.map { document ->
                    Post(
                        id = document.id,
                        title = document.getString("title") ?: "",
                        content = document.getString("content") ?: ""
                    )
                }

                //  로그 추가 (불러온 게시글 수 확인)
                android.util.Log.d("EditPostActivity", "불러온 게시글 수: ${postList.size}")

                adapter.submitList(postList)
            }
            .addOnFailureListener {
                Toast.makeText(this, "게시글을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun confirmAndDelete(postId: String) {
        AlertDialog.Builder(this)
            .setTitle("게시글 삭제")
            .setMessage("정말로 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                firestore.collection("posts").document(postId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        loadUserPosts()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onEdit(post: Post) {
        val intent = Intent(this, updatePostActivity::class.java)
        intent.putExtra("postId", post.id)
        startActivity(intent)
    }

    override fun onDelete(post: Post) {
        confirmAndDelete(post.id)
    }
}