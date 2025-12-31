package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myapplication.databinding.ActivityMapBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit


class MapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapBinding
    lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initBinding()
        initNavigation()
        initDrawerMenu()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val workRequest = PeriodicWorkRequestBuilder<TrafficWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TrafficAlertWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        PostListener.startListening(this)

        // 네비게이션 헤더에서 사용자 정보 뷰 찾기 (추가된 부분)
        val headerView = navView.getHeaderView(0) // 네비게이션 헤더 가져오기
        val usernameTextView: TextView = headerView.findViewById(R.id.username)
        val emailTextView: TextView = headerView.findViewById(R.id.email)
        val profileImageView: ImageView = headerView.findViewById(R.id.currentProfileImage)

        // 내 정보 로드
        loadUserData(usernameTextView, emailTextView, profileImageView)
    }

    override fun onDestroy() {
        super.onDestroy()
        PostListener.stopListening()
    }

    private fun initBinding() {
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout
        navView = binding.navView
    }



    private fun initDrawerMenu() {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.epbtn -> {
                    val intent = Intent(this, EditProfileActivity::class.java)
                    startActivity(intent)
                }
                R.id.pwbtn -> {
                    val intent = Intent(this, ChangePasswordActivity::class.java)
                    startActivity(intent)
                }
                R.id.alarm_set -> {
                    val intent = Intent(this, AlarmActivity::class.java)
                    startActivity(intent)
                }
                R.id.privacy_set -> {
                    val intent = Intent(this, PrivacyActivity::class.java)
                    startActivity(intent)
                }
                R.id.earthquake_info -> {
                    val intent = Intent(this, EarthquakeActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_typhoon -> {
                    val intent = Intent(this, TyphoonActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_fire_info -> {
                    val intent = Intent(this, WebViewActivity::class.java)
                    startActivity(intent)
                }
                R.id.favoriteBtn -> {
                    val intent = Intent(this, FavoriteListActivity::class.java)
                    startActivity(intent)
                }
                R.id.logout -> {
                    auth.signOut()
                    getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit().clear().apply()
                    Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(navView) // 메뉴 선택 후 Drawer 닫기
            true
        }
    }

    // 하단 내비게이션 설정
    private fun initNavigation() {
        NavigationUI.setupWithNavController(binding.bottomNavigation, findNavController(R.id.nav_host))
    }

    private fun loadUserData(usernameTextView: TextView, emailTextView: TextView, profileImageView: ImageView) {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid

        if (userId != null) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // 사용자 이름 및 이메일 설정
                        usernameTextView.text = document.getString("name") ?: "사용자 이름"
                        emailTextView.text = document.getString("email") ?: "user@example.com"

                        // 프로필 이미지 설정
                        val profileImageName = document.getString("profileImage") ?: "profile1"
                        val resId = resources.getIdentifier(profileImageName, "drawable", packageName)
                        profileImageView.setImageResource(resId)
                    } else {
                        Toast.makeText(this, "사용자 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
        } else {
            // 로그인되지 않은 상태일 경우 기본값 설정
            usernameTextView.text = "사용자 이름"
            emailTextView.text = "user@example.com"
            profileImageView.setImageResource(R.drawable.profile1) // 기본 프로필 이미지
        }
    }
}
