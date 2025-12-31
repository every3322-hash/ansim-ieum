package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FavoriteListActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FavoriteAdapter
    private val favoriteList = mutableListOf<FavoriteItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_list)

        val toolbar = findViewById(R.id.top_toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val top = supportActionBar!!
        top.setDisplayShowTitleEnabled(false)

        val backButton = findViewById<ImageView>(R.id.btnBack)
        backButton.setOnClickListener {
            finish()
        }

        recyclerView = findViewById(R.id.favoriteRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FavoriteAdapter(
            favoriteList,
            onDeleteClick = { item -> deleteFavorite(item) },
            onItemClick = { item -> navigateToDetailPage(item) }
        )
        recyclerView.adapter = adapter

        loadFavoritesFromFirestore()
    }

    private fun loadFavoritesFromFirestore() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).collection("favorites")
            .get()
            .addOnSuccessListener { result ->
                favoriteList.clear()
                for (doc in result) {
                    val item = doc.toObject(FavoriteItem::class.java)
                    favoriteList.add(item)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("FavoriteList", "Error getting favorites", e)
                Toast.makeText(this, "즐겨찾기 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun deleteFavorite(item: FavoriteItem) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(uid)
            .collection("favorites")
            .whereEqualTo("title", item.title)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    db.collection("users").document(uid)
                        .collection("favorites")
                        .document(doc.id)
                        .delete()
                        .addOnSuccessListener {
                            favoriteList.remove(item)
                            adapter.notifyDataSetChanged()
                            Toast.makeText(this, "즐겨찾기가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "삭제 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun navigateToDetailPage(item: FavoriteItem) {
        val categoryId = item.safety_cate_nm2

        Log.d("NavigateToPage", "categoryId: $categoryId")

        // 카테고리 코드 -> 페이지 매핑
        val targetActivity = when (categoryId) {
            "01001", "01002", "01003", "01004", "01005", "01006", "01007", "01008", "01009", "01010",
            "01011", "01012", "01013", "01014", "01015" -> NaturalActivity::class.java

            "02001", "02002", "02003", "02004", "02005", "02006", "02007", "02008", "02009", "02010",
            "02011", "02012", "02013", "02014", "02015", "02016", "02017", "02018", "02019", "02020",
            "02021", "02022", "02023" -> SocialActivity::class.java

            "03001", "03002", "03003", "03004", "03005", "03006", "03007", "03008", "03009", "03010",
            "03011", "03012", "03013", "03014", "03015", "03016", "03017" -> LifeActivity::class.java

            "04001", "04002", "04003" -> CivilActivity::class.java

            else -> null
        }
        if (targetActivity != null) {
            val intent = Intent(this, targetActivity)
            intent.putExtra("safetyCategory", categoryId)
            startActivity(intent)
        } else {
            Log.w("NavigateToPage", "No matching activity for categoryId: $categoryId")
            Toast.makeText(this, "해당하는 페이지가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
