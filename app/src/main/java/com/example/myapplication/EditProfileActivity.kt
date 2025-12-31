package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var selectedProfileImage: String = "profile1" // 기본 프로필 이미지

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.backButton.setOnClickListener {
            finish() // 현재 액티비티 종료
        }

        // Firestore에서 사용자 데이터 로드
        loadUserData()

        // 프로필 이미지 RecyclerView 설정
        setupProfileImageRecyclerView()

        // 저장 버튼 클릭 리스너
        binding.saveProfileButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    binding.editProfileName.setText(document.getString("name") ?: "")
                    binding.profileEmail.text = auth.currentUser?.email ?: "user@example.com"
                    val profileImage = document.getString("profileImage") ?: "profile1"
                    selectedProfileImage = profileImage
                    val resId = resources.getIdentifier(profileImage, "drawable", packageName)
                    binding.currentProfileImage.setImageResource(resId)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "사용자 데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupProfileImageRecyclerView() {
        val profileImages = listOf(
            "profile1", "profile2", "profile3", "profile4",
            "profile5", "profile6", "profile7", "profile8",
            "profile9", "profile10","profile11","profile12"
        )
        val adapter = ProfileImageAdapter(profileImages) { imageName ->
            selectedProfileImage = imageName
            val resId = resources.getIdentifier(imageName, "drawable", packageName)
            binding.currentProfileImage.setImageResource(resId)
        }
        binding.profileImageRecyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.profileImageRecyclerView.adapter = adapter
    }

    private fun saveProfile() {
        val userId = auth.currentUser?.uid ?: return
        val name = binding.editProfileName.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // Firestore에 이름과 이미지 이름만 저장
        val updates = mapOf(
            "name" to name,
            "profileImage" to selectedProfileImage // "profile1", "profile2" 등
        )

        firestore.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "프로필 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

}
