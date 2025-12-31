package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityPostDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker


class PostDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityPostDetailBinding
    private lateinit var likeButton: TextView
    private lateinit var postId: String
    private lateinit var naverMap: NaverMap
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var marker: Marker? = null // 마커 객체를 관리하기 위해 추가
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)


        likeButton = findViewById(R.id.likeButton)

        postId = intent.getStringExtra("postId") ?: ""

        likeButton.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val postRef = FirebaseFirestore.getInstance().collection("posts").document(postId)
            val likeRef = postRef.collection("likes").document(uid)

            likeRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // 이미 좋아요 눌렀으면 취소
                    likeRef.delete().addOnSuccessListener {
                        postRef.update("likeCount", FieldValue.increment(-1))
                            .addOnSuccessListener {
                                Toast.makeText(this, "좋아요를 취소했습니다.", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // 좋아요 추가
                    likeRef.set(mapOf("timestamp" to System.currentTimeMillis()))
                        .addOnSuccessListener {
                            postRef.update("likeCount", FieldValue.increment(1))
                                .addOnSuccessListener {
                                    Toast.makeText(this, "좋아요를 눌렀습니다.", Toast.LENGTH_SHORT).show()
                                }
                        }
                }
            }
        }

        // 인텐트를 통해 전달받은 데이터 처리
        val title = intent.getStringExtra("title") ?: "제목 없음"
        val content = intent.getStringExtra("content") ?: "내용 없음"
        val profileImageName = intent.getStringExtra("profileImageName") ?: "profile1" // 기본 프로필 이미지
        val profileName = intent.getStringExtra("profileName") ?: "익명 사용자"
        val postDate = intent.getStringExtra("postDate") ?: "날짜 정보 없음"
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)

        // 디버그 로그 확인
        Log.d("PostDetailActivity", "Received Profile Name: $profileName")
        Log.d("PostDetailActivity", "Received Profile Image Name: $profileImageName")

        // UI에 데이터 설정
        binding.detailTitle.text = title
        binding.detailContent.text = content
        binding.profileName.text = profileName
        binding.postDate.text = postDate


        // 프로필 이미지 리소스 설정
        val resId = resources.getIdentifier(profileImageName, "drawable", packageName)
        if (resId != 0) {
            binding.profileImage.setImageResource(resId)
        } else {
            binding.profileImage.setImageResource(R.drawable.ic_profile_placeholder)
        }

        if (postId != null) {
            loadComments(postId)  //  진입 시 바로 댓글 불러오기
            CommentListener.startListeningToComments(this, postId)
        }

        // 뒤로가기 버튼 리스너 설정
        binding.backButton.setOnClickListener {
            finish() // 이전 화면으로 이동
        }
        //댓글 생성
        binding.postCommentButton.setOnClickListener {
            val commentText = binding.commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                val postId = intent.getStringExtra("postId") ?: return@setOnClickListener
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

                // 🔸 먼저 사용자 정보 가져오기
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val authorName = document.getString("name") ?: "익명"

                        val commentData = hashMapOf(
                            "content" to commentText,
                            "author" to authorName,
                            "timestamp" to System.currentTimeMillis(),
                            "userId" to uid
                        )

                        FirebaseFirestore.getInstance()
                            .collection("posts")
                            .document(postId)
                            .collection("comments")
                            .add(commentData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "댓글이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                                binding.commentInput.text.clear()
                                loadComments(postId)

                                FirebaseFirestore.getInstance()
                                    .collection("posts")
                                    .document(postId)
                                    .update("commentCount", FieldValue.increment(1))
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "댓글 등록에 실패했습니다.", Toast.LENGTH_SHORT).show()

                            }

                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "사용자 이름을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        // 공유 버튼 리스너 설정
        binding.shareButton.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
            }
            startActivity(Intent.createChooser(shareIntent, "게시글 공유하기"))
        }

        // 지도 초기화
        val mapFragment = supportFragmentManager.findFragmentByTag("mapFragment") as MapFragment?
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction()
                    .add(R.id.detail_map_fragment, it, "mapFragment").commit()
            }
        mapFragment.getMapAsync(this)
    }

    override fun onStop() {
        super.onStop()
        CommentListener.stopListeningToComments()
    }

    private fun loadComments(postId: String) {
        val commentContainer = binding.commentContainer
        commentContainer.removeAllViews()

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // 🔹 게시글 작성자의 UID 가져오기
        FirebaseFirestore.getInstance()
            .collection("posts")
            .document(postId)
            .get()
            .addOnSuccessListener { postDoc ->
                val postAuthorId = postDoc.getString("userId") ?: ""

                FirebaseFirestore.getInstance()
                    .collection("posts")
                    .document(postId)
                    .collection("comments")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener { result ->
                        for (doc in result) {
                            val inflater = LayoutInflater.from(this)
                            val commentView = inflater.inflate(R.layout.item_comment, commentContainer, false)

                            val authorView = commentView.findViewById<TextView>(R.id.commentAuthor)
                            val contentView = commentView.findViewById<TextView>(R.id.commentContent)
                            val deleteButton = commentView.findViewById<TextView>(R.id.commentDeleteButton)

                            val author = doc.getString("author") ?: "익명"
                            val content = doc.getString("content") ?: ""
                            val commentUserId = doc.getString("userId") ?: ""
                            val commentId = doc.id

                            authorView.text = author
                            contentView.text = content

                            // 🔸 댓글 작성자나 게시글 작성자일 경우 삭제 버튼 표시
                            if (commentUserId == currentUserId || postAuthorId == currentUserId) {
                                deleteButton.visibility = View.VISIBLE
                                deleteButton.setOnClickListener {
                                    FirebaseFirestore.getInstance()
                                        .collection("posts")
                                        .document(postId)
                                        .collection("comments")
                                        .document(commentId)
                                        .delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                            loadComments(postId)

                                            // 댓글 수 -1
                                            FirebaseFirestore.getInstance()
                                                .collection("posts")
                                                .document(postId)
                                                .update("commentCount", FieldValue.increment(-1))
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "댓글 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }

                            commentContainer.addView(commentView)
                        }
                    }
            }
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map

        // 마커 추가 및 위치 설정
        val postLocation = LatLng(latitude, longitude)
        marker = Marker().apply {
            position = postLocation
            this.map = naverMap
        }

        // 카메라 이동 및 줌 설정
        val cameraUpdate = CameraUpdate.scrollAndZoomTo(postLocation, 15.0)
        naverMap.moveCamera(cameraUpdate)
    }
}
