package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityBoardBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker

class updatePostActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityBoardBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var naverMap: NaverMap? = null
    private var selectedLatLng: LatLng? = null
    private var selectedImageUri: Uri? = null
    private var postId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        postId = intent.getStringExtra("postId")
        if (postId == null) {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 게시글 데이터 불러오기
        loadPostData()

        // 지도 설정
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapContainer) as MapFragment?
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().add(R.id.mapContainer, it).commit()
            }
        mapFragment.getMapAsync(this)
        binding.mapContainer.visibility = View.GONE

        binding.locationButton.setOnClickListener {
            binding.mapContainer.visibility = View.VISIBLE
        }

        binding.photoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            photoResultLauncher.launch(intent)
        }

        binding.inputBtn.text = "수정하기"
        binding.inputBtn.setOnClickListener {
            val title = binding.titleArea.text.toString().trim()
            val content = binding.contentArea.text.toString().trim()

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else if (selectedLatLng != null) {
                updatePostToFirestore(title, content, selectedLatLng!!.latitude, selectedLatLng!!.longitude)
            } else {
                Toast.makeText(this, "지도를 클릭해 위치를 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPostData() {
        postId?.let { id ->
            firestore.collection("posts").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        binding.titleArea.setText(document.getString("title") ?: "")
                        binding.contentArea.setText(document.getString("content") ?: "")

                        val lat = document.getDouble("latitude") ?: 0.0
                        val lng = document.getDouble("longitude") ?: 0.0
                        selectedLatLng = LatLng(lat, lng)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "게시글 로드 실패", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updatePostToFirestore(title: String, content: String, latitude: Double, longitude: Double) {
        val updatedPost = mapOf(
            "title" to title,
            "content" to content,
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to System.currentTimeMillis()
        )

        postId?.let { id ->
            firestore.collection("posts").document(id)
                .update(updatedPost)
                .addOnSuccessListener {
                    Toast.makeText(this, "게시글이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "수정에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private val photoResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                selectedImageUri = result.data?.data
                binding.photoButton.text = "사진이 선택되었습니다."
            }
        }

    override fun onMapReady(map: NaverMap) {
        naverMap = map

        selectedLatLng?.let { latLng ->
            val marker = Marker()
            marker.position = latLng
            marker.map = naverMap
            val cameraUpdate = CameraUpdate.scrollTo(latLng)
            naverMap?.moveCamera(cameraUpdate)
        }

        naverMap?.setOnMapClickListener { _, latLng ->
            selectedLatLng = latLng
            Toast.makeText(this, "선택된 위치: ${latLng.latitude}, ${latLng.longitude}", Toast.LENGTH_SHORT).show()

            val marker = Marker()
            marker.position = latLng
            marker.map = naverMap
            naverMap?.moveCamera(CameraUpdate.scrollTo(latLng))
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
