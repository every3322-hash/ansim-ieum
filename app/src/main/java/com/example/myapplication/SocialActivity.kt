package com.example.myapplication

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SocialActivity : AppCompatActivity() {
    private val db by lazy { com.google.firebase.ktx.Firebase.firestore }
    private lateinit var toolbarTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_social)

        setupToolbar()
        setupRecyclerView()
        handleIntentData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.top_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbarTitle = findViewById(R.id.toolbar_title)

        val backButton = findViewById<ImageView>(R.id.btnBack)
        backButton.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView2)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun handleIntentData() {
        val safetyCategory = intent.getStringExtra("safetyCategory") ?: ""
        if (safetyCategory.isEmpty()) {
            Toast.makeText(this, "No category provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        fetchData(safetyCategory)
    }

    private fun fetchData(safetyCategory: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.safetydata.go.kr/V2/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val call = apiService.getSafetyInfo(
            endpoint = "DSSP-IF-20589",
            serviceKey = "P510ZYWQBZ4ZI66B",
            pageNo = 1,
            numOfRows = 30,
            returnType = "json",
            safetyCate = safetyCategory // 전달받은 카테고리 사용
        )

        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val safetyItems = apiResponse?.body ?: emptyList()

                    // 툴바 제목 설정
                    val newTitle = safetyItems.firstOrNull()?.safety_cate_nm2 ?: "제목"
                    toolbarTitle.text = newTitle

                    // 카테고리별 그룹핑
                    val groupedItems = safetyItems.groupBy { it.safety_cate_nm3 ?: "기타" }

                    // 어댑터 초기화 - 드롭다운용
                    val recyclerView: RecyclerView = findViewById(R.id.recyclerView2)
                    recyclerView.layoutManager = LinearLayoutManager(this@SocialActivity)
                    recyclerView.adapter = MultiViewTypeAdapter(groupedItems)
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@SocialActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.favorite_menu, menu)
        val favoriteItem = menu.findItem(R.id.action_favorite)
        checkFavoriteAndUpdateIcon(favoriteItem)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_favorite -> {
                toggleFavorite()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkFavoriteAndUpdateIcon(menuItem: MenuItem) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val categoryId = intent.getStringExtra("safetyCategory") ?: return

        val favoriteRef = db.collection("users").document(currentUser.uid)
            .collection("favorites").document(categoryId)

        favoriteRef.get().addOnSuccessListener { document ->
            menuItem.setIcon(
                if (document.exists()) R.drawable.baseline_star_filled
                else R.drawable.baseline_star_outline
            )
        }
    }

    private fun toggleFavorite() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val categoryId = intent.getStringExtra("safetyCategory") ?: return
        val categoryName = toolbarTitle.text.toString()

        val favoriteRef = db.collection("users").document(currentUser.uid)
            .collection("favorites").document(categoryId)

        favoriteRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                favoriteRef.delete().addOnSuccessListener {
                    Toast.makeText(this, "즐겨찾기에서 제거되었습니다.", Toast.LENGTH_SHORT).show()
                    invalidateOptionsMenu()
                }
            } else {
                val favoriteItem = mapOf(
                    "title" to categoryName,
                    "safety_cate_nm2" to categoryId
                )
                favoriteRef.set(favoriteItem).addOnSuccessListener {
                    Toast.makeText(this, "즐겨찾기에 추가되었습니다.", Toast.LENGTH_SHORT).show()
                    invalidateOptionsMenu()
                }
            }
        }
    }
}
